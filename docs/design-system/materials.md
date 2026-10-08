# Material system

## M1. Choose a role by surface purpose

Components use the selected surface renderer. With Backdrop rendering, `M3xBackdropTheme` assigns
a material to each role, `material(role)` resolves it, and `ProvideBackdropMaterial` overrides one
role in a subtree. The baseline behavior below belongs to that adapter.

| Role | Purpose and baseline behavior |
| --- | --- |
| `Control` | Compact interactive glass; vibrancy, corner-based lens, and exterior shadow |
| `Panel` | Larger containers; stronger blur and depth lens on corner-based shapes |
| `Surface` | Card backing; vibrancy without its own highlight/shadow/inner shadow, because Material elevation supplies the card's elevation |
| `InversePanel` | Transient feedback using the inverse Material container/foreground pair and fixed supplied colors |
| `ScrollEdge` | Progressive separation over scrolling content, with a shared blur/mask profile and interaction lighting disabled by default |

A role chooses material behavior. A component or explicit mask determines geometry and placement.
Cards select the theme's `surface` material as their default backing even though their shared
appearance path is parameterized as a panel.

Source: [roles and baseline materials](../../material-backdrop/src/commonMain/kotlin/theme/M3xBackdropTheme.kt).

## M2. Effect precedence

Appearance resolves in this order:

1. A component material overrides the corresponding theme role.
2. Reduced transparency selects an empty optical chain and opaque tint.
3. Otherwise, explicit component `effects` replaces the entire optical chain, including built-in
   adaptation color controls, blur, and the material style's effects.
4. Without explicit effects, optional background color controls run first, then role blur when
   `BackdropBlurEnabled` is true, then the style's effects.

`BackdropBlurEnabled` resolves the master switch and reduced transparency for built-in backgrounds.
`BlurAnimationEnabled` separately resolves foreground motion. Disabling the master switch preserves
an available background source and stacking. A caller-supplied effect chain owns its own blur
policy. Decorations and lighting have separate style/material controls.

Source: [appearance resolution](../../material-backdrop/src/commonMain/kotlin/BackdropSurface.kt).

## M3. Material colors remain the base

An unspecified material tint uses the component container color. Content uses the supplied Material
foreground; `InversePanel` starts from the inverse surface pair. Fixed adaptation preserves those
choices. `tintAlpha` controls a sourced surface and `fallbackAlpha` controls its source-free tint.
`dimmingAlpha` adds black over the sampled background for a material that requests dimming.

Background adaptation changes foreground only when the material also enables
`adaptiveContentColor` and a sample is available. It selects black/white using the local appearance
and hysteresis. Reading theme brightness alone is insufficient to describe an adapted surface.

Source: [surface color resolution](../../material-backdrop/src/commonMain/kotlin/BackdropSurface.kt).
Regression: [inverse panels](../../material/src/nonBrowserTest/kotlin/InversePanelRenderingTest.kt).

## M4. Source absence is a supported appearance

Built-in surfaces draw the role's fallback tint when no backdrop handle is provided. The low-level
`Modifier.backdropSurface` adds no drawing in that case; its caller supplies an ordinary background.
An `OverlayHost` without a page source preserves that absence for built-in nested surfaces even
though it maintains internal capture layers.

A scroll edge whose built-in blur is disabled raises tint strength to at least `fallbackAlpha`.
Explicit mask opacity still controls an explicitly configured mask. Reduced transparency makes
ordinary role tint opaque and flattens progressive masking. Increased contrast resolves the final
foreground and container together and uses an opaque fallback when the source is unknown.

Sources: [low-level surface](../../backdrop/src/commonMain/kotlin/Backdrop.kt) and
[built-in fallback](../../material-backdrop/src/commonMain/kotlin/BackdropSurface.kt).
Regressions: [rendered fallback](../../material-backdrop/src/jvmTest/kotlin/BackdropFallbackRenderingTest.kt)
and [overlay fallback](../../overlay/src/commonTest/kotlin/OverlayFallbackTest.kt).

## M5. Background adaptation is local and opt-in

`BackdropAdaptation.Fixed` is the baseline for every supplied preset. Set a material to `Background`
to request local SDR appearance sampling. Sampling is enabled only with an available source and
without reduced transparency. Each adapted surface owns its thumbnail and result; `BackdropHost`
batches readbacks. One surface's result does not represent the whole page or another surface.
The sampler's initial fallback keeps supplied content colors usable before a result arrives.

Source: [appearance state and batching](../../backdrop/src/commonMain/kotlin/BackdropAppearance.kt).
Regressions: [appearance resolution](../../backdrop/src/commonTest/kotlin/BackdropAppearanceTest.kt)
and [batched readback](../../backdrop/src/jvmTest/kotlin/BackdropAppearanceBatchTest.kt).

## M6. Progressive blur and masking are coordinated inputs

The `ScrollEdge` role uses its `ramp` for progressive blur. A mask is applied only when a component
passes one. An explicit mask ramp takes precedence over the material ramp for masking, and an
explicit brush/alpha controls the mask. A default app bar coordinates blur and mask with the role's
sampled ramp and current presence, including the drawing room beyond the bar.

A surface's role alone does not turn a flat tint into a mask. Reduced transparency flattens the
mask to the surface, preserving readable opaque backing. The mask's extension must fit its ramp;
clipping it to the original layout bounds can leave a visible seam.

Regressions: [mask behavior](../../material-backdrop/src/nonBrowserTest/kotlin/BackdropMaskTest.kt),
[mask fill](../../material-backdrop/src/nonBrowserTest/kotlin/BackdropMaskFillTest.kt), and
[scroll-edge room](../../material-backdrop/src/nonBrowserTest/kotlin/ScrollEdgeAreaTest.kt).

## M7. Presets select optics independently of theme content

`BackdropPresets.GlassRegular` supplies the baseline role set. `GlassClear` lowers control/panel blur
and tint opacity. `MaterialBlur` uses quieter decoration and stronger tinting. Presets retain fixed
adaptation and stable theme foregrounds. Customize with `copy` from the selected preset so roles
that the application does not override retain that preset's values.

The exact optical tuning is owned by
[BackdropPresets](../../material-backdrop/src/commonMain/kotlin/BackdropPresets.kt) and the
[role definitions](../../material-backdrop/src/commonMain/kotlin/theme/M3xBackdropTheme.kt).
Regression: [preset stability and runtime replacement](../../material-theming/src/commonTest/kotlin/BackdropThemeTest.kt).
