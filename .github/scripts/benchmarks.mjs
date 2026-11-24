import { Buffer } from 'node:buffer';

export const START = '<!-- m3x:performance:start -->';
export const END = '<!-- m3x:performance:end -->';
export const SKIP_BENCHMARKS = '<!-- m3x:benchmark:skip -->';
export const PENDING = 'Publication and benchmark results are pending.';
const FULL_CHANGELOG = /^\*\*Full Changelog\*\*:[ \t]*(.*)$/m;

export function skipsBenchmarks(body) {
  return (body ?? '').split(/\r?\n/).some(line => line.trim() === SKIP_BENCHMARKS);
}

export function releaseBaseline(body, repoUrl, releaseTag, commit) {
  const footer = FULL_CHANGELOG.exec(body ?? '')?.[1];
  const link = footer?.match(/https?:\/\/[^\s<>)]+/)?.[0];
  if (!link) return null;
  try {
    const repo = new URL(repoUrl);
    const comparison = new URL(link);
    const prefix = `${repo.pathname}/compare/`;
    if (comparison.origin !== repo.origin || !comparison.pathname.startsWith(prefix)) return null;
    const range = comparison.pathname.slice(prefix.length).split('...');
    if (range.length !== 2) return null;
    const [baseline, target] = range.map(decodeURIComponent);
    if (baseline === releaseTag || baseline === commit) return null;
    return baseline && (target === releaseTag || target === commit) ? baseline : null;
  } catch {
    return null;
  }
}

function managedBlock(body) {
  const starts = body.split(START).length - 1;
  const ends = body.split(END).length - 1;
  if (starts === 0 && ends === 0) return null;
  if (starts !== 1 || ends !== 1 || body.indexOf(START) > body.indexOf(END)) {
    throw new Error('Release notes need one ordered performance marker pair.');
  }
  const start = body.indexOf(START);
  const end = body.indexOf(END) + END.length;
  return { start, end, text: body.slice(start, end) };
}

/** Keeps the managed section last, with GitHub's changelog footer after it. */
export function performanceNotes(body = '', content) {
  body ??= '';
  if (skipsBenchmarks(body)) return body;
  const existing = managedBlock(body);
  const block = content === undefined && existing
    ? existing.text
    : `${START}\n## Benchmarks\n\n<details>\n<summary>Backdrop JVM benchmark results</summary>\n\n${content ?? PENDING}\n\n</details>\n${END}`;
  const prose = existing ? body.slice(0, existing.start) + body.slice(existing.end) : body;
  const footer = FULL_CHANGELOG.exec(prose);
  if (footer && /^##\s/m.test(prose.slice(footer.index + footer[0].length))) {
    throw new Error('Keep the generated Full Changelog footer after all release sections.');
  }
  const at = footer ? footer.index : prose.length;
  return `${prose.slice(0, at).trimEnd()}\n\n${block}\n\n${prose.slice(at).trimStart()}`.trim() + '\n';
}

/** Expands the workload matrix printed by JMH's own parameter discovery. */
export function workloadKeys(listing) {
  const methods = new Map();
  let current;
  let listed = false;
  for (const line of listing.split(/\r?\n/)) {
    if (/^Benchmarks:\s*$/.test(line)) { listed = true; continue; }
    if (!listed) continue;
    if (/^[\w$]+(?:\.[\w$]+)+$/.test(line)) {
      if (methods.has(line)) throw new Error('Duplicate JMH workload declaration.');
      current = new Map();
      methods.set(line, current);
      continue;
    }
    const param = /^\s+param "([^"]+)" = \{([^}]+)}\s*$/.exec(line);
    if (param) {
      if (!current || current.has(param[1])) throw new Error('Invalid JMH parameter declaration.');
      current.set(param[1], param[2].split(', ').map(value => value.trim()));
    }
  }
  if (methods.size === 0) throw new Error('JMH did not list any workloads.');
  const keys = new Set();
  for (const [method, params] of methods) {
    let combinations = [{}];
    for (const [name, values] of params) {
      combinations = combinations.flatMap(row => values.map(value => ({ ...row, [name]: value })));
    }
    for (const params of combinations) keys.add(resultKey({ benchmark: method, params }));
  }
  return keys;
}

function resultKey(row) {
  return JSON.stringify([row.benchmark, Object.entries(row.params ?? {}).sort(([a], [b]) => a.localeCompare(b))]);
}

export function validateResults(results, listing) {
  if (!Array.isArray(results) || results.length === 0) throw new Error('Benchmark results are empty.');
  const expected = workloadKeys(listing);
  const actual = new Set();
  for (const row of results) {
    const key = resultKey(row);
    if (actual.has(key) || !expected.has(key)) throw new Error('Duplicate or unexpected benchmark result.');
    actual.add(key);
    for (const [metric, unit] of [[row.primaryMetric, 'us/op'], [row.secondaryMetrics?.['gc.alloc.rate.norm'], 'B/op']]) {
      if (!metric || !Number.isFinite(metric.score) || metric.score < 0 || metric.scoreUnit !== unit) {
        throw new Error('A benchmark metric is missing, non-finite or uses an unexpected unit.');
      }
    }
    if (row.mode !== 'avgt') throw new Error('Benchmark results must measure average time.');
  }
  if (actual.size !== expected.size) throw new Error('Benchmark results omit discovered workloads.');
  return results;
}

function cell(value) {
  return String(value).replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
    .replaceAll('|', '&#124;').replaceAll('`', '&#96;').replace(/[\r\n]+/g, ' ');
}

function parameters(row) {
  return Object.entries(row.params ?? {}).sort(([a], [b]) => a.localeCompare(b))
    .map(([name, value]) => `${cell(name)}=${cell(value)}`).join(', ') || '—';
}

export function benchmarkTable(results, listing) {
  validateResults(results, listing);
  const rows = [...results].sort((a, b) => resultKey(a).localeCompare(resultKey(b)));
  const table = [
    '| Benchmark | Parameters | Time (µs/op) | Error (µs/op) | Managed allocation (B/op) |',
    '| --- | --- | ---: | ---: | ---: |',
  ];
  for (const row of rows) {
    const error = Number.isFinite(row.primaryMetric.scoreError) ? row.primaryMetric.scoreError.toFixed(3) : 'n/a';
    table.push(`| ${cell(row.benchmark)} | ${parameters(row)} | ${row.primaryMetric.score.toFixed(3)} | ${error} | ${row.secondaryMetrics['gc.alloc.rate.norm'].score.toFixed(3)} |`);
  }
  return table.join('\n');
}

function benchmarkCommit(commit) {
  if (!/^[a-f0-9]{40,64}$/.test(commit)) throw new Error('A benchmark report needs its exact commit.');
  return `\`${commit}\``;
}

function measurementSettings(results) {
  const sample = results[0];
  for (const name of ['forks', 'warmupIterations', 'measurementIterations']) {
    if (!Number.isInteger(sample[name]) || sample[name] < 1) throw new Error('Benchmark execution metadata is incomplete.');
  }
  for (const name of ['jdkVersion', 'warmupTime', 'measurementTime']) {
    if (typeof sample[name] !== 'string' || !sample[name]) throw new Error('Benchmark runtime metadata is incomplete.');
  }
  const heapArguments = row => {
    if (!Array.isArray(row.jvmArgs)) throw new Error('Benchmark fork heap metadata is missing.');
    const initial = row.jvmArgs.findLast(arg => /^-Xms[1-9][0-9]*[kmg]?$/i.test(arg));
    const maximum = row.jvmArgs.findLast(arg => /^-Xmx[1-9][0-9]*[kmg]?$/i.test(arg));
    if (!initial || !maximum) throw new Error('Benchmark fork heap metadata is incomplete.');
    return `${initial} ${maximum}`;
  };
  const heap = heapArguments(sample);
  if (results.some(row => heapArguments(row) !== heap)) throw new Error('Benchmark fork heaps differ between workloads.');
  const fields = ['jdkVersion', 'vmName', 'vmVersion', 'threads', 'forks', 'warmupIterations',
    'warmupTime', 'warmupBatchSize', 'measurementIterations', 'measurementTime', 'measurementBatchSize'];
  if (results.some(row => fields.some(field => row[field] !== sample[field]))) {
    throw new Error('Benchmark measurement settings differ between workloads.');
  }
  return {
    key: JSON.stringify([heap, ...fields.map(field => sample[field])]),
    description: `JDK ${cell(sample.jdkVersion)}, ${sample.forks} forks, ${sample.warmupIterations} warm-ups (${cell(sample.warmupTime)}), ${sample.measurementIterations} measurements (${cell(sample.measurementTime)}). Fork heap \`${heap}\`.`,
  };
}

function change(before, after) {
  if (before.score === 0) return after.score === 0 ? '0.0%' : 'From zero';
  const delta = (after.score - before.score) / before.score * 100;
  const uncertain = !Number.isFinite(before.scoreError) || !Number.isFinite(after.scoreError)
    || Math.abs(after.score - before.score) <= before.scoreError + after.scoreError;
  return `${delta > 0 ? '+' : ''}${delta.toFixed(1)}%${uncertain ? ' †' : ''}`;
}

function compareBenchmarks(results, baseline, settings, harnessChanged) {
  validateResults(baseline.results, baseline.listing);
  const heading = `Baseline \`${cell(baseline.ref)}\` at ${benchmarkCommit(baseline.commit)}.`;
  if (settings.key !== measurementSettings(baseline.results).key) {
    return `${heading}\n\n*Comparison unavailable because JMH measurement settings differ.*`;
  }
  const previous = new Map(baseline.results.map(row => [resultKey(row), row]));
  const groups = new Map();
  let matched = 0;
  for (const row of [...results].sort((a, b) => resultKey(a).localeCompare(resultKey(b)))) {
    const old = previous.get(resultKey(row));
    if (!old) continue;
    matched++;
    const parts = row.benchmark.split('.');
    const method = parts.pop();
    const group = parts.join('.');
    if (!groups.has(group)) groups.set(group, []);
    groups.get(group).push(`| ${cell(method)} | ${parameters(row)} | ${change(old.primaryMetric, row.primaryMetric)} | ${change(old.secondaryMetrics['gc.alloc.rate.norm'], row.secondaryMetrics['gc.alloc.rate.norm'])} |`);
  }
  const sections = [heading,
    `${matched} matched workloads, ${results.length - matched} candidate only, ${previous.size - matched} baseline only.`,
  ];
  if (harnessChanged) sections.push('*Benchmark or scene sources differ between revisions. Matching names and parameters can still describe changed workloads.*');
  if (matched) sections.push('*Positive changes mean more time or allocation. † marks overlapping or unavailable JMH uncertainty intervals.*');
  for (const [group, rows] of groups) {
    const name = group.split('.').at(-1);
    sections.push(`**${cell(name)}**`,
      ['| Benchmark | Parameters | Time change | Managed allocation change |', '| --- | --- | ---: | ---: |', ...rows].join('\n'));
  }
  return sections.join('\n\n');
}

export function benchmarkReports(results, listing, { commit, runUrl, baseline, harnessChanged = false }) {
  const table = benchmarkTable(results, listing);
  const settings = measurementSettings(results);
  const intro = [
    `Candidate ${benchmarkCommit(commit)}. [Raw results and environment](${runUrl}).`,
    '*Compare measurements only within the same CI run. Each pair runs sequentially on one shared Linux VM. Motion workloads measure unpaced JVM CPU rasterization, and allocation counts managed memory only.*',
    settings.description,
  ];
  const comparison = baseline
    ? compareBenchmarks(results, baseline, settings, harnessChanged)
    : '*No baseline was requested.*';
  const summary = ['## Backdrop JVM time and allocation', ...intro, comparison, table].join('\n\n');
  const release = [...intro, comparison].join('\n\n');
  if (Buffer.byteLength(release, 'utf8') > 50000) throw new Error('The performance table exceeds the release-note budget; retain the full artifact and revise the reporting layout.');
  return { summary, release };
}

/** Reads the current body immediately before replacing only the managed section. */
export async function updatePerformance(github, repo, releaseId, content, expectedTag) {
  const { data } = await github.rest.repos.getRelease({ ...repo, release_id: releaseId });
  if (expectedTag && data.tag_name !== expectedTag) throw new Error("The release tag changed during the workflow.");
  const body = performanceNotes(data.body, content);
  if (body !== data.body) await github.rest.repos.updateRelease({ ...repo, release_id: releaseId, body });
  return data;
}

/** Requires the latest push CI for this commit; pending runs are observed without dispatching tests. */
