# Backdrop integration

Start with the [existing page template](../README.md#backdrop-page-setup). It places `BackdropHost`
outside `OverlayHost`, captures page background/content, and keeps sampling glass outside its own
source subtree. These responsibilities apply equally to a custom page and the demo.

The [Backdrop rendering guides](backdrop/README.md) cover standalone surfaces and custom renderers.

## Supply pixels separately from surfaces

`rememberBackdropLayer(background, tileMode)` creates a source handle. With the same tile mode,
background color changes update capture without replacing the handle. A supplied background is
baked into the capture. Alternatively, draw the background inside the captured subtree: put
`captureBackdrop` before `background` in the modifier chain so that background is recorded.

`BackdropHost` provides `LocalBackdrop`, an appearance readback batch, and a shared lighting scope.
Built-in surfaces read the handle automatically. `Modifier.backdropSurface` exposes one Backdrop
DSL for surface drawing and image composition. Effects, fill, highlights and shadows work at the
root and inside nested images or explicit `layer` declarations. `surface { }` applies fixed
appearance phases with inherited `BackdropStyle` values. An explicit layer retains its isolation;
ordinary single-consumer drawing can share a recording with its destination.

Built-in material surfaces draw the contour behind their content. The contour environment
combines the processed backdrop and interior light within the material with the original backdrop
beyond its outline. [Styles and decorations](backdrop/styles-and-decorations.md)

Both Material roles and custom callers choose their image dependencies, masks and export selection
through the same declaration. Material roles add color and accessibility policy. A low-level caller
without a source owns its fallback background.

Source: [handle, capture, provider, and low-level drawing](../modules/backdrop/src/commonMain/kotlin/Backdrop.kt).
Appearance policy: [material contracts](design-system/materials.md).

## Build overlay sources

The Backdrop adapter links the entries supplied by `OverlayHost` in draw order. `M3xMaterialTheme`
and `ProvideM3xMaterialRendering` install the adapter. For an independent overlay composition,
install it with `ProvideBackdropOverlay` from `overlay-backdrop`. Each entry samples the page and
lower entries, then exports its captured pixels for higher surfaces. Each lower layer contributes
once at each stack level. Recording starts when a layer exists, so a newly opened surface can
sample the current frame below it.

The adapter maintains the stack when the page source is absent and supplies a null source to
built-in surfaces, which use their fallback. Hosted overlays join the stack through host registration.
Lower-level `rememberStackedBackdropLayer` and `captureStackBackdrop` support custom source
composition.

Regressions: [stack composition](../modules/material-backdrop/src/jvmTest/kotlin/BackdropStackRenderingTest.kt),
[overlay stack rendering](../modules/material/src/jvmTest/kotlin/BackdropStackOverlayRenderingTest.kt), and
[active-stack rebuild](../modules/material-backdrop/src/jvmTest/kotlin/FloatingBottomBarStackRebuildRenderingTest.kt).

## Define the source boundary deliberately

Both `rememberBackdropLayer` and `rememberStackedBackdropLayer` accept `tileMode`:

| Mode | Sampling beyond a recorded source |
| --- | --- |
| `TileMode.Clamp` (default) | Extends boundary texels, including transparency |
| `TileMode.Decal` | Transparent exterior |
| `TileMode.Repeated`, `TileMode.Mirror` | Repeat or mirror the source |

Boundary extension requires runtime shader support. Each stack layer keeps its own source policy;
a transparent boundary cannot supply missing opaque content. Diagnose source coverage before
changing blur radius or tint opacity.

## Configure contour highlight preparation

`LocalHighlightSdfSettings` controls contour highlight acceleration in a Compose subtree.
Automatic mode prepares a half-resolution distance and normal field for a complex, stable
contour. `On` prepares half resolution after the first contour frame. `Progressive` presents
a complete quarter-resolution field before refining to half resolution. `Off` uses the
contour directly. CPU and GPU selection can be overridden independently. Forced GPU uses
contour rendering when GPU baking is unavailable. Field replacement follows the current
geometry and applies to all perimeter tiles together. Android's GPU path uses
a fenced hardware buffer on API 34 and later. Compose Desktop records the field as a Skia
picture for replay by the window renderer. The picture and filter graph retain the
geometry for reuse in subsequent frames. Skia composites cropped SDF tiles over one
environment recording. CPU preparation uses expanded segment bounds to mark distant texels
invalid without evaluating their distance. The field retains the neighboring samples
needed for interpolation.

## Separate output capability from theme appearance

M3X material adaptation uses local SDR appearance. HDR rendering and display headroom belong to
the underlying Backdrop renderer and the application window. The demo queries public Backdrop
capabilities and configures Android window requests in
[DemoHdrWindowSettings](../modules/demo/src/androidMain/kotlin/DemoHdrWindowSettings.kt).
An HDR request or a bright theme color does not prove that the active display supplies headroom.
Other platform implementations explicitly describe the support available to their launcher.

Use [BackdropLabPage](../modules/demo/src/commonMain/kotlin/BackdropLabPage.kt) to inspect effects and moving
sources. Lab settings affect its preview; they are separate from application appearance settings.
