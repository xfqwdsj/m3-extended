# Benchmarks

Suites under `modules/benchmarks/` exercise public APIs and run independently of the correctness
tests. The Backdrop suite covers graph processing and animated rendering.

## Shared workloads

`benchmarks:backdrop:scenarios` owns the Compose scenes, target state and input schedules. Both
`benchmarks:backdrop:jvm` and `benchmarks:backdrop:android-app` consume that module directly. Rendering code is
shared by the hosts. `benchmarks:backdrop:android-macro` drives the Android host
and uses the same scenario and journey definitions. Graph microbenchmarks live in the
JVM module and use the public declaration and renderer APIs. They have the same access boundary
as external consumers.

The scene set covers distinct interaction families:

| Scene | Animated behavior | Rendering boundary |
| --- | --- | --- |
| SharedBounds | Card expands into an editor and returns | Shared overlay, remeasured bounds, translation, crossfade, foreground and backdrop blur, generic contour |
| Sheet | Bottom sheet expands and dismisses | Measured height, translation, group alpha, foreground and backdrop blur |
| LayeredCarousel | Cards advance through a stack | Uniform scale, translation and Z rotation with sampling of lower cards |
| Perspective | Card turns in depth | X/Y rotation and camera distance with a generic contour |
| Stretch | Elastic pull and recovery | Nonuniform scale, transform origin and spring motion |
| Compositing | Background fade and group reveal | Background and isolated group alpha from zero to one, animated destination-in mask |
| Scroll | Feed moves beneath a sticky surface | Actual scrolling of a captured source |
| Adaptive | Viewport contracts across a layout breakpoint | Measured constraints and row/column reflow |

The carousel records each lower card as a transparent stack layer before the next card samples
it. Its recording includes the rendered material and card content under the same transform used
for presentation. Perspective records one stationary striped background and samples it in page
coordinates through the projected card outline. The static stripe source and scrolling feed also
replay their recorded drawing.

The scenes use the public Backdrop DSL and Compose Foundation/Animation in the shared scenario
module. SharedBounds and Sheet animate blur radius from 8 to 28 dp; other
scenes keep optical parameters fixed to isolate their layout, transform, opacity or source changes.
Materials include vibrancy, processed-color highlighting, and lens effects on supported shapes.
Generic and rounded contours exercise distinct geometry paths. Stripe pitch, layout dimensions and
filter radii use dp so backend density scales them together; the scrolling feed retains text and row
boundaries.

`Complete` opens and closes each scene; `Interrupted` reverses at frames 12 and 20 before the
final close at frame 96. The shared reference clock advances at 16,666,667 ns per frame.
Tween animations last 600 ms; elastic motion uses a spring. The 96-frame transition window
includes settling time. JVM drives a virtual clock; Android schedules real input and waits for
the animation's idle semantics. Each standard Android measurement repeats three journeys, including the
first open after launch and reopening in one aggregate frame distribution. The closed scene
constructs its material before that distribution. Cold shader-construction measurements therefore
need a workload that defers material creation until the measured interaction.
Standard JMH reports describe warmed throughput. Measure cold-start latency in fresh processes.

The consumer rendering tests inspect intermediate images, endpoint restoration after reversals,
and the difference between background and group opacity. Image inspection occurs in correctness
tests, outside timed benchmark methods.

## JVM time and allocation

```sh
./gradlew :benchmarks:backdrop:scenarios:jvmTest
./gradlew :benchmark -PbenchmarkSmoke=true
./gradlew :benchmark
./gradlew :benchmark -PbenchmarkInclude=MotionBenchmark
```

`GraphBenchmark` isolates declaration and resolution with one or eight receivers.

`StyleBenchmark` measures immutable style construction and derivation, then pairs styled and native
declaration and retained resolution for uniform and layered blur. The resolution methods prepare
resources without recording pixels; the suite measures managed allocation and CPU work at each
boundary.

`GraphTopologyBenchmark` covers shared branches, overlapping transforms, masks and serial
filters with retained or rebuilt declarations.

`MotionBenchmark` renders the shared scenes synchronously through a raster `ImageComposeScene`;
its result is unpaced raster throughput, not displayed frame rate or GPU execution time.
Frame cost varies across each journey. A short measurement window can cover only part of a
transition, especially when raster frames are slow. For focused animation comparisons, also
measure complete open/close cycles after whole-cycle warm-up and repeat both revisions in
reversed order. Keep these cycle measurements separate from the standard JMH report.

The full JMH allocation task uses five one-second warm-ups, eight one-second measurements and two
forks with a fixed 2 GiB heap. Use `-PbenchmarkHeap=<size>`, such as `2g`, to select another fixed
fork heap; matched comparisons use the same heap on both revisions. The report includes the actual
initial and maximum fork heap sizes from JMH metadata. It reports microseconds per operation and GC-profiler managed bytes
per operation. Native Skia memory is outside that allocation metric. Smoke runs use shorter windows
to verify execution. Establish baselines with the full measurement schedule and preserve generated
JSON reports before another run overwrites them. The manual
[benchmark workflow](../.github/workflows/benchmark.yml) checks the workload matrix and preserves results with
environment metadata. Shared CI runner load varies, so use their observations with the recorded
environment metadata. See the official [JMH project](https://github.com/openjdk/jmh) for its
measurement model.

## Android frame timing

`benchmarks:backdrop:android-app` is a release-optimized, profileable, non-debuggable host using local
debug signing. `benchmarks:backdrop:android-macro` uses official Android Macrobenchmark
`FrameTimingMetric` with full compilation and ten measured iterations for each scene/journey.
The metric reports CPU frame duration and, on API 31+, frame deadline overrun percentiles;
see the official [metric definitions](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-metrics).
Each iteration launches a fresh process in setup, outside the measured animation block.
UI Automator idle synchronization is disabled during input and restored afterward. Input deadlines
come from the shared journey; every toggle verifies the accepted target before the final idle
check. Device input can run late under load, so retain traces when interpreting reversal timing.
A synchronous touch call can span a slow animation; observing an active state after it returns
is not required.

```sh
./gradlew :benchmarks:backdrop:android-app:assembleRelease :benchmarks:backdrop:android-macro:assembleRelease
ANDROID_SERIAL="<device-serial>" ./gradlew :benchmarks:backdrop:android-macro:connectedReleaseAndroidTest
```

For workload execution checks, the harness defines the same Pixel 6 API 37 Google APIs device
profile used by ordinary M3X device tests. AGP manages the emulator lifecycle. The release-variant
task below uses Macrobenchmark dry-run mode. See [Gradle managed devices](https://developer.android.com/studio/test/managed-devices)
and [Macrobenchmark instrumentation arguments](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-instrumentation-args).

```sh
./gradlew :benchmarks:backdrop:android-macro:pixel6Api37ReleaseAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.dryRunMode.enable=true \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.killExistingPerfettoRecordings=false
```

The device task installs the profileable app and runs one journey per workload in dry-run mode. For an emulator
already attached through ADB, use `connectedReleaseAndroidTest` with the same arguments.
Use physical devices for representative frame measurements. Preserve benchmark JSON and Perfetto traces with device model, API, refresh rate, compilation
mode, power and thermal state. See the official
[Macrobenchmark overview](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview).

## Measurement boundaries

Measure GPU execution with backend profiling tools and correlate their events with the measured
frames. Synchronous submission waiting includes driver and GPU waits;
it is not isolated GPU execution time. Neither raster timing nor layer counts establish GPU
memory use, shader cost or displayed frame rate. Collect diagnostic profiles separately from
unprofiled timing runs, and state the measured boundary explicitly.

For comparisons, use the same committed harness and workload parameters against identified library
revisions. Preserve raw results, source provenance and environment; do not compare changed scenes
as though only the renderer changed. Correctness and visual preservation are prerequisites for
interpreting a timing improvement.

## Official Compose contracts

The scene design follows the official
[graphics modifiers](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers),
[animation guide](https://developer.android.com/develop/ui/compose/animation/quick-guide) and
[shared transition customization](https://developer.android.com/develop/ui/compose/animation/shared-elements/customize).
Measured size and graphics-layer scaling are separate workloads: graphics transforms do not
change layout constraints. Animated graphics properties are read in the graphics-layer lambda.
Shared bounds use remeasurement; isolated opacity and masks have explicit storage padding for
shadows. Perspective applies the same camera projection to the card outline and its foreground.
The backdrop filter reads the stationary source through an affine mapping in the page plane.
Platform blur implementations can differ, so backend results remain separate.

## Pixel regression bounds

JVM progressive-blur tests measure premultiplied RGBA mean error, maximum channel error and
maximum neighbor-difference error over fixed patterns, directions and level counts. Backend
shader and F16 checks exercise the corresponding device paths separately. Keep calibrated limits
beside the tests. These assertions detect visual regressions independently of timing.

## Release reporting contract

The [release workflow](../.github/workflows/publish.yml) calls the benchmark workflow after Maven
Central publication succeeds. The changelog comparison selects the baseline for the same-runner
pair. JMH discovers workload names and parameter domains, and the report validator checks each
complete Cartesian matrix before matching comparable rows. The [release contract](releases.md)
defines the folded report, artifact links, skip marker and manual workflow inputs.

A detailed comparative performance report records the following.

- The compared release tags and exact commit IDs, built from clean working trees. Benchmark
  harnesses and workload changes are committed and identified alongside the measured library
  revisions so the published comparison can be reproduced.
- Workload parameters, backend, hardware, OS, runtime, build and compilation configuration, power
  and thermal conditions, warm-up, iterations and forks.
- Timing units and measured boundaries, uncertainty or percentiles, and allocation scope. Separate
  unpaced JVM rendering throughput, displayed frame timing, managed allocation and native memory.
- Correctness and lifecycle coverage, unavailable checks, and inconclusive or adverse observations.
- Raw benchmark JSON, relevant traces and environment metadata linked from the report.

Compare matched workloads under equivalent conditions. Record probe or profiler overhead and
collect attribution profiles separately from timing runs. A deterministic reduction in graph
nodes, layers or sampling area supports that work bound; timed claims require measurements of the
stated backend and workload.
