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
| `ScrollEdge` | Progressive separation over scrolling content, with a shared blur/fill profile and interaction lighting disabled by default |

A role chooses material behavior. Components determine geometry and placement; explicit progressive
fills also determine the room used for their ramp.
Cards select the theme's `surface` material as their default backing even though their shared
appearance path is parameterized as a panel.

Source: [roles and baseline materials](../../material-backdrop/src/commonMain/kotlin/theme/M3xBackdropTheme.kt).

## M2. Effect precedence

Appearance resolves in this order:

1. A component material overrides the corresponding theme role.
2. Reduced transparency selects an empty optical chain and opaque tint.
3. Otherwise, explicit component `effects` replaces the entire optical chain, including built-in
   adaptation color controls and the style's optical fields.
4. Without explicit effects, optional background adaptation and style color controls precede blur,
   vibrancy, and lens. `BackdropBlurEnabled` gates the style blur.

`BackdropBlurEnabled` resolves the master switch and reduced transparency for built-in backgrounds.
`BlurAnimationEnabled` separately resolves foreground motion. Disabling the master switch preserves
an available background source and stacking. A caller-supplied effect chain owns its own blur
policy. The parameterized `BackdropStyle` supplies blur, fill, decorations, receiving light opacity,
and page insets. `M3xBackdropLighting` configures emitted light color, radius, and its global gate.
A material's `render` declaration replaces the complete graph for surfaces that need Backdrop's render DSL. `BackdropSurface` also
accepts `render` directly for a single surface; it takes precedence over the material declaration.
Its content overload uses a Box and supplies the resolved content color; the content-free overload
uses a Spacer. `rememberBackdropSurfaceAppearance` and `Modifier.materialBackdropSurface` expose
the same theme resolution and drawing for caller-owned containers, including explicit presence.

`ProvideBackdropTheme` supplies independent `BackdropStyle` and `BackdropRenderStyle` defaults.
The default declaration uses `styledSurface`, which resolves optical fields at the receiving
modifier. Low-level `Modifier.backdropSurface` inherits that declaration or the control material's
complete custom `render`. Outside a render-style provider its default draws the unprocessed source;
pass `render = { styledSurface() }` to select the styled phases explicitly. A nested control material
override updates both defaults. Built-in declarations apply the transparency, blur and interaction
light gates; a complete custom declaration owns its rendering policy.

Construct or derive a style with `BackdropStyle { ... }` or `baseStyle { ... }`. Omitted fields
inherit at the receiver; assigning `null` clears an override, while neutral values such as
`BackdropBlur.None` explicitly disable a stage. `BackdropStyle.Default` and `NoEffects` supply
complete values. Native uniform, progressive, and layered progressive algorithms retain their
algorithm, edge and ramp. M3X's progressive mode selects an algorithm for a uniform scroll-edge
radius; an explicit native progressive configuration selects its own algorithm and geometry.
Use style fields for theme optics, a component's explicit `effects` for a complete optical-chain
replacement, and `render` for a custom graph. Lens processing applies to corner-based shapes.

Source: [appearance resolution](../../material-backdrop/src/commonMain/kotlin/BackdropSurface.kt).
Regression: [receiving style inheritance](../../material-bundle/src/jvmTest/kotlin/BackdropThemeRenderingTest.kt),
[native algorithms](../../material-backdrop/src/jvmTest/kotlin/BackdropStyleAlgorithmsTest.kt), and
[style filling](../../material-backdrop/src/jvmTest/kotlin/BackdropSurfaceRenderTest.kt).

## M3. Material colors remain the base

An unspecified material tint uses the component container color. Content uses the supplied Material
foreground; `InversePanel` starts from the inverse surface pair. Fixed adaptation preserves those
choices. `tintAlpha` controls a sourced surface and `fallbackAlpha` controls its source-free tint.
`dimmingAlpha` adds black over the sampled background for a material that requests dimming.
`style.fill` supplies a `BackdropFill`: a null brush delegates to the Material role tint; an
explicit brush, including transparent fill, takes precedence. Its optional alpha multiplies the
brush opacity and its ramp controls filling independently of blur. Presets delegate to Material.

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
Explicit fill opacity still controls an explicitly configured fill. Reduced transparency makes
ordinary role tint opaque and flattens progressive filling. Increased contrast resolves the final
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

## M6. Progressive blur and fill are coordinated inputs

The `ScrollEdge` role uses its `ramp` when converting uniform style blur to progressive blur.
Progressive filling is requested by a component fill or a ramp in the style fill. An explicit fill
ramp takes precedence over the material ramp for filling, and an explicit brush/alpha controls the
fill. An app bar with `scrollBehavior` coordinates blur and
fill with the role's sampled ramp and current presence, including the drawing room beyond the bar.
Without a behavior its material stays within the bar and preserves explicit fill brush and opacity
without ramping. `backdropEnabled = false` leaves drawing to an enclosing container and preserves
its source for controls. Controls sample the page composed with the exported bar background,
including while that background fades or contributes no pixels.

`Modifier.backdropScrollEdge` applies the same rendering to any container or edge strip. It accepts
an edge and a caller-owned `visible` condition, such as `scrollState.canScrollForward`; presence
animates with the theme's effects motion. Zero presence contributes no background pixels,
so a translucent capture preserves the scene beneath it. Positive finite blur motion changes radius
and fill strength without also fading the complete background.
`progressive = false` keeps automatic uniform blur and filling inside a static container. Explicit
native progressive blur retains its configured geometry. App bars use this shared resolution and
drawing path, adapting their Material overlap state to visibility.
`rememberScrollEdgePresence` exposes the same animation independently, and
`BackdropScrollEdgeDefaults.progressiveBlur` supplies image-level processing for custom graphs.

For a blur-only strip, pass `fill = BackdropFill.None`. A material override can supply a fractional
`BackdropRamp()` for a thin horizontal edge instead of the theme's bar-oriented plateau and fade.
Capture the complete background and controls, with the edge overlays outside that capture. An
opaque finite strip uses a clamped source boundary: `TileMode.Decal` deliberately returns
transparent exterior samples, which blend the original sharp content back through a blurred edge.
For an unprocessed background copy, pass `render = { source() }` to select source pixels explicitly.

A surface's role alone does not make its tint progressive. Reduced transparency flattens the
fill to the surface, preserving readable opaque backing. The fill's extension must fit its ramp;
clipping it to the original layout bounds can leave a visible seam.

`LocalProgressiveBlurMode` defaults to `Auto`, which selects the full shader or an eight-level
approximation. A surface's `progressiveBlurMode` overrides it. `Full` requires shader support and
otherwise draws no material background or decorations; the bar then preserves the enclosing source.
`Layered(levels)` selects the approximation directly. `Alpha` uniformly blurs the image and applies
the ramp to that image's alpha, independently of filling. These selections remain subject to the
master blur switch, per-surface `blurEnabled`, and accessibility. Disabling blur retains tint
fallback, and reduced transparency retains opaque backing even with `Full`. Without uniform blur
support, the approximation modes use material tint fallback. Explicit effects and complete render
graphs retain their own processing policy.

Regressions: [public surfaces and algorithms](../../material-backdrop/src/jvmTest/kotlin/PublicBackdropSurfaceTest.kt),
[fill behavior](../../material-backdrop/src/nonBrowserTest/kotlin/BackdropFillTest.kt),
[fill brush](../../material-backdrop/src/nonBrowserTest/kotlin/BackdropFillBrushTest.kt), and
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
