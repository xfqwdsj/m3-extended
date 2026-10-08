# Backdrop integration

Start with the [existing page template](../README.md#backdrop-page-setup). It places `BackdropHost`
outside `OverlayHost`, captures page background/content, and keeps sampling glass outside its own
source subtree. These responsibilities apply equally to a custom page and the demo.

## Supply pixels separately from surfaces

`rememberBackdropLayer(background, tileMode)` creates a source handle. With the same tile mode,
background color changes update capture without replacing the handle. A supplied background is
baked into the capture. Alternatively, draw the background inside the captured subtree: put
`captureBackdrop` before `background` in the modifier chain so that background is recorded.

`BackdropHost` provides `LocalBackdrop`, an appearance readback batch, and a shared lighting scope.
Built-in surfaces read the handle automatically. Low-level `Modifier.backdropSurface` takes a shape,
effects, decorations, draw callbacks, and optional exports. Its no-source behavior is to add no
drawing; custom callers own their fallback background. Built-in surfaces use the role's tint.

Source: [handle, capture, provider, and low-level drawing](../backdrop/src/commonMain/kotlin/Backdrop.kt).
Appearance policy: [material contracts](design-system/materials.md).

## Let the host build overlay sources

`OverlayHost` records and links entries in draw order. Each entry samples the page and lower entries,
then exports its own captured pixels for higher surfaces. Lower layers are composed once per stack
level without repeatedly including the same earlier layers. Stack captures start recording when
the layer exists so a newly opened upper surface can see the current frame below it.

The host maintains this stack even without a page source, but preserves source absence for built-in
surfaces. Hosted overlays participate in the automatic stack through their host registration.
Lower-level `rememberStackedBackdropLayer` and `captureStackBackdrop` support custom source
composition.

Regressions: [stack composition](../material-backdrop/src/nonBrowserTest/kotlin/BackdropStackRenderingTest.kt),
[overlay stack rendering](../material/src/jvmTest/kotlin/BackdropStackOverlayRenderingTest.kt), and
[active-stack rebuild](../material-backdrop/src/jvmTest/kotlin/FloatingBottomBarStackRebuildRenderingTest.kt).

## Define the source boundary deliberately

Both `rememberBackdropLayer` and `rememberStackedBackdropLayer` accept `tileMode`:

| Mode | Sampling beyond a recorded source |
| --- | --- |
| `Clamp` (default) | Extends boundary texels, including transparency |
| `Decal` | Transparent exterior |
| Repeated/mirror | Tiles the source |

Boundary extension requires runtime shader support. Each stack layer keeps its own source policy;
a transparent boundary cannot supply missing opaque content. Diagnose source coverage before
changing blur radius or tint opacity.

## Separate output capability from theme appearance

M3X material adaptation uses local SDR appearance. HDR rendering and display headroom belong to
the underlying Backdrop renderer and the application window. The demo queries public Backdrop
capabilities and configures Android window requests in
[DemoHdrWindowSettings](../demo/src/androidMain/kotlin/DemoHdrWindowSettings.kt).
An HDR request or a bright theme color does not prove that the active display supplies headroom.
Other platform implementations explicitly describe the support available to their launcher.

Use [BackdropLabPage](../demo/src/commonMain/kotlin/BackdropLabPage.kt) to inspect effects and moving
sources. Lab settings affect its preview; they are separate from application appearance settings.
