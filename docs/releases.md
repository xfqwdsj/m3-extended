# M3X releases

The [release group manifest](../gradle/release-groups.json) assigns publication versions to modules
and lists the groups to upload. All groups start at `0.1.0-alpha02` for the first joint release.

## Choose release versions

Each group maintains its own semantic version for its public contracts. The BOM maintains a
semantic version for the compatibility of the full managed combination. The `version` in
[gradle.properties](../gradle.properties) is the BOM, GitHub Release, and demo version. Use one Git
tag per release with that version, optionally prefixed with `v`.

During the BOM's `0.x` releases, APIs and module boundaries may change. Keep the BOM at `0.x`
while any managed group remains at `0.x`, even when other groups reach `2.x` or later. Publish
BOM `1.0.0` once every managed group has reached a stable `1.0.0` or later and M3X can guarantee
compatibility for the managed combination.

From BOM `1.0.0` onward, increase its major version whenever it adopts a breaking release from
any managed group or introduces a breaking change to the combination itself. Removing a managed
coordinate breaks consumers that rely on the BOM to supply its version. Use a minor release for
backward-compatible additions and a patch release for backward-compatible fixes.

For example, if a release upgrades Backdrop from `1.4.2` to its breaking `2.0.0`, the BOM can
advance from `1.6.0` to `2.0.0` with Git tag `v2.0.0`. Select `backdrop` in the manifest and keep
the published versions of unaffected groups. List the changed groups and migration steps in the
release notes.

## Choose publication groups

| Group | Modules |
| --- | --- |
| `ui` | Compose infrastructure, overlays, Material, and their adapters |
| `backdrop` | Backdrop rendering |
| `shapes` | Structured contour geometry and Capsule |
| `resource` | Resource contracts and their Compose integration |
| `datastore` | DataStore contracts, runtime adapters, serialization, and its processor |
| `settings` | Settings contracts, Compose and DataStore adapters, and its processor |
| `compiler` | Shared JVM infrastructure used by both processors |

For a subsequent release, advance the repository release version and set `publish` to the
affected groups. Advance those groups' versions in the manifest. Keep every other group's
version at its published coordinate. Review source, generated APIs, ABI, effective dependencies
and publication metadata when choosing the groups. If a dependency update changes a published
consumer's metadata or behavior, review and release that consumer's group too. Publish each
selected KMP module's root and target variants together.

The processors follow their generated code's contracts, while `processor-support` has its own
compiler group because both processors consume it. The settings DataStore adapter joins the
settings group and depends on the DataStore group. The Material settings adapter joins UI and
depends on the settings and resource groups. Backdrop and the UI adapters consume the shape and
renderer contracts at their respective dependency boundaries.

## Align consumer versions

Every release publishes `top.ltfan.m3x:m3x-bom:<release-version>`. The BOM takes the versions
and coordinates from the library publications, including each KMP root and target artifact.
It lists the complete combination of groups for that release. Select the libraries your
application uses in Gradle or Maven. [Gradle platform usage](../README.md#getting-started)

In Maven, import the BOM in `dependencyManagement` and add the desired platform artifact in
your dependencies. For example, a JVM consumer may declare `top.ltfan.m3x:capsule-jvm`
with the version supplied by the BOM.

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>top.ltfan.m3x</groupId>
      <artifactId>m3x-bom</artifactId>
      <version>${m3x.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

## Prepare and publish a release

Commit the selected versions, sources, and harness changes together. Run
[Prepare release draft](../.github/workflows/draft-release.yml) with the tag and target commit.
Generated notes seed an editable draft and reserve space for benchmarks. An optional previous tag
controls the changelog comparison range. Resuming the draft preserves author edits.

Publishing validates the version and exact tagged commit, then requires the `Tests` check from the
latest [Test workflow](../.github/workflows/test.yml) push run for that commit. The gate waits up to
20 minutes for the JVM and Android result. Missing, failed, canceled, or timed-out tests block
publication. A Pages-only retry uses the preceding test result. The
[publishing workflow](../.github/workflows/publish.yml) verifies the release and uploads selected
groups to GitHub Packages, then Maven Central. Release verification builds the Maven publication
artifacts and metadata for the selected groups. The BOM follows the selected libraries in each
repository. Packages uses `--continue` to try independent tasks.
Central starts after all Packages uploads succeed. Demo packages use the same verified commit.

`./gradlew :publishLocal` publishes all library groups to Maven Local after JVM verification.
Remote entry points `:publishCentral` and `:publishPackages` take `-PreleaseTag=<tag>` and require
a clean checkout at that tag. Module build files own POM metadata, signing, and variants.
Inspect a KMP module's POM with `generatePomFileForKotlinMultiplatformPublication`; Gradle writes
publication metadata under that module's `build/publications/`. Use
`:m3x-bom:generatePomFileForMavenPublication` to inspect the BOM's managed versions.

## Publish benchmark results

After Maven Central succeeds, the [benchmark workflow](../.github/workflows/benchmark.yml) compares
the Backdrop JVM suite against the base ref in the release body's same-repository
**Full Changelog** comparison link. The link must target the release tag or commit. A missing
or unusable link, or a revision without the harness, skips measurement. The first release can
establish a standalone execution reference from the manual workflow. For an explicit comparison,
supply `baseline-ref` to run both revisions sequentially on the same runner.

The folded **Benchmarks** section shows relative changes for matched workloads with equivalent
measurement settings. Raw JSON, discovered workloads, environment metadata and detailed Markdown
are retained as workflow artifacts for 90 days. The [benchmark guide](benchmarks.md) defines
measurement boundaries for the Backdrop suite.

Add `<!-- m3x:benchmark:skip -->` on its own line before publishing to skip measurement and keep
the release body as written. Automation checks it again before writing results. Keep author
text outside the `<!-- m3x:performance:start -->` and `<!-- m3x:performance:end -->` markers.
The managed section follows the author sections and precedes GitHub's **Full Changelog** footer.
Completion reads the current body, preserves author edits, and recreates a missing placeholder.
Retry a failed benchmark job to update the report from the same commit and comparison.
