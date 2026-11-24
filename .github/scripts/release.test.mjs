import test from 'node:test';
import assert from 'node:assert/strict';
import { checkVersion, prepareDraft, requirePassingTests } from './release.mjs';

const repo = { owner: 'example', repo: 'library' };
const properties = 'version=1.2.3-rc01\n';

test('release versions match with an optional v prefix', () => {
  assert.equal(checkVersion('v1.2.3-rc01', properties), '1.2.3-rc01');
  assert.equal(checkVersion('1.2.3-rc01', properties), '1.2.3-rc01');
  assert.throws(() => checkVersion('v1.2.2', properties));
  assert.throws(() => checkVersion('v1.2.3', ''));
});

function releases(existing = []) {
  const calls = [];
  return {
    calls,
    paginate: async () => existing,
    rest: { repos: {
      listReleases() {},
      generateReleaseNotes: async args => {
        calls.push(['notes', args]);
        return { data: { name: 'Generated title', body: 'Generated changes\n\n**Full Changelog**: link' } };
      },
      createRelease: async args => { calls.push(['create', args]); return { data: { id: 42, ...args } }; },
    } },
  };
}

test('drafts use generated notes and the requested target without creating a tag', async () => {
  const api = releases();
  const draft = await prepareDraft(api, repo, 'v1.2.3-rc01', 'v1.2.2', 'main', properties);
  assert.equal(draft.draft, true);
  assert.equal(draft.prerelease, true);
  assert.equal(draft.target_commitish, 'main');
  assert.match(draft.body, /^Generated changes\n/);
  assert.match(draft.body, /## Benchmarks/);
  assert.match(draft.body, /\*\*Full Changelog\*\*: link\n$/);
  assert.equal(api.calls[0][1].previous_tag_name, 'v1.2.2');
  assert.deepEqual(api.calls.map(([name]) => name), ['notes', 'create']);
});

test('resuming a draft preserves manual edits without regeneration', async () => {
  const existing = { tag_name: 'v1.2.3-rc01', draft: true, body: 'Owner edits' };
  const api = releases([existing]);
  assert.equal(await prepareDraft(api, repo, existing.tag_name, '', 'main', properties), existing);
  assert.deepEqual(api.calls, []);
});

test('published releases and duplicate version aliases cannot be replaced', async () => {
  for (const existing of [
    { tag_name: 'v1.2.3-rc01', draft: false },
    { tag_name: '1.2.3-rc01', draft: true },
  ]) {
    const api = releases([existing]);
    await assert.rejects(prepareDraft(api, repo, 'v1.2.3-rc01', '', 'main', properties));
    assert.deepEqual(api.calls, []);
  }
});

function ci(pages, jobs = (run) => [job('Tests', run.run_attempt, run.conclusion, run.status)]) {
  let index = 0;
  let runs;
  const actions = { listJobsForWorkflowRun() {}, listWorkflowRuns: async args => {
    assert.equal(args.workflow_id, 'test.yml');
    assert.equal(args.event, 'push');
    assert.equal(args.head_sha, 'release-commit');
    runs = pages[Math.min(index++, pages.length - 1)];
    return { data: { workflow_runs: runs } };
  } };
  return { rest: { actions }, paginate: async (endpoint, args) => {
    assert.equal(endpoint, actions.listJobsForWorkflowRun);
    assert.equal(args.filter, 'all');
    return jobs(runs.find(run => run.id === args.run_id));
  } };
}
const run = (id, status, conclusion, run_attempt = 1) => ({ id, head_sha: 'release-commit', event: 'push', status, conclusion, run_attempt });
const job = (name, run_attempt, conclusion, status = 'completed') => ({ name, run_attempt, status, conclusion });

test('publication requires successful push CI for the exact commit', async () => {
  const passed = run(1, 'completed', 'success');
  assert.equal(await requirePassingTests(ci([[passed]]), repo, 'release-commit'), passed);
  await assert.rejects(requirePassingTests(ci([[{ ...passed, head_sha: 'other' }]]), repo, 'release-commit'));
  await assert.rejects(requirePassingTests(ci([[{ ...passed, event: 'pull_request' }]]), repo, 'release-commit'));
});

test('the latest failed CI blocks publication despite an earlier success', async () => {
  await assert.rejects(requirePassingTests(ci([[run(1, 'completed', 'success'), run(2, 'completed', 'failure')]]), repo, 'release-commit'));
});

test('pending CI is observed until completion without dispatching tests', async () => {
  const passed = run(2, 'completed', 'success');
  let waited = 0;
  const actual = await requirePassingTests(ci([[run(2, 'in_progress', null)], [passed]]), repo, 'release-commit', {
    wait: async () => { waited++; },
  });
  assert.equal(actual, passed);
  assert.equal(waited, 1);
});

test('missing CI and a pending run past its deadline block publication', async () => {
  await assert.rejects(requirePassingTests(ci([[]]), repo, 'release-commit'));
  let clock = 0;
  await assert.rejects(requirePassingTests(ci([[run(1, 'in_progress', null)]]), repo, 'release-commit', {
    timeoutMs: 1, now: () => clock++, wait: async () => {},
  }));
});

test('the Tests job can pass while Pages is pending or after Pages fails', async () => {
  for (const workflow of [run(1, 'in_progress', null), run(1, 'completed', 'failure')]) {
    assert.equal(await requirePassingTests(ci([[workflow]], () => [job('Tests', 1, 'success')]), repo, 'release-commit'), workflow);
  }
});

test('Pages-only retries use the most recent test attempt', async () => {
  const retry = run(1, 'completed', 'failure', 3);
  const pagesRuns = [job('Build Wasm demo site', 2, 'success'), job('Deploy to GitHub Pages', 3, 'failure')];
  assert.equal(await requirePassingTests(ci([[retry]], () => [job('Tests', 1, 'success'), ...pagesRuns]), repo, 'release-commit'), retry);
  await assert.rejects(requirePassingTests(ci([[retry]], () => [job('Tests', 1, 'failure'), ...pagesRuns]), repo, 'release-commit'), /has not passed/);
  await assert.rejects(requirePassingTests(ci([[retry]], () => [
    job('Tests', 1, 'success'), job('Android device tests', 2, 'cancelled'), ...pagesRuns,
  ]), repo, 'release-commit'), /No completed Tests check/);
});

test('a test retry must produce a new passing result', async () => {
  const retry = run(1, 'completed', 'failure', 2);
  for (const conclusion of ['failure', 'cancelled', 'skipped', 'timed_out']) {
    await assert.rejects(requirePassingTests(ci([[retry]], () => [
      job('Tests', 1, 'success'), job('Tests', 2, conclusion),
    ]), repo, 'release-commit'), new RegExp(`has not passed: ${conclusion}`));
  }
  await assert.rejects(requirePassingTests(ci([[retry]], () => [job('Tests', 1, 'success')]), repo, 'release-commit'), /No completed Tests check/);
});
