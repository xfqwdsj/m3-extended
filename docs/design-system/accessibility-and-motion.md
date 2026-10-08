# Accessibility, motion, and interaction light

## A1. Preferences resolve independently

`AccessibilityPreferences` configures reduced transparency, increased contrast, and reduced motion
with independent `System`, `Enabled`, or `Disabled` choices. `SystemAccessibilitySettings` retains
unknown values as null; System resolves only a known true value to enabled. Explicit choices
supersede the observed system value for policy resolution. Platform animation timing remains subject
to the limits described in A5. A backdrop preset change preserves these preferences.

Source: [resolution](../../ui/src/commonMain/kotlin/accessibility/AccessibilityPreferences.kt).
Regression: [preference resolution](../../ui/src/commonTest/kotlin/AccessibilityPreferencesTest.kt).

## A2. Runtime observation belongs to a dynamic provider

`ProvideAccessibility` works independently of Material. The root provider observes the platform;
nested providers reuse its live snapshot and resolve their own preferences. `M3xMaterialTheme`
installs this provider and accepts an independent `accessibility` argument. Hosted content forwards
the originating theme, resolved accessibility values, and effect switches; host-owned backdrop,
shared-transition, and input scopes remain at the host.

Read the composable inline policy getters for effective behavior. `LocalAccessibilitySettings`
exposes the resolved values to custom policies. Dynamic locals track readers; static theme
extensions retain configuration. Increased contrast also updates the native Material color scheme.

Platform readers preserve unknown values when the host cannot report a setting. The
[platform implementations](../../ui/src) own availability checks and observation
lifetimes.

Regressions: [observation](../../ui/src/composeTest/kotlin/AccessibilityTest.kt),
[theme recomposition and inheritance](../../material-theming/src/commonTest/kotlin/AccessibilityThemeTest.kt),
and [surface response](../../material-backdrop/src/commonTest/kotlin/AccessibilityAppearanceTest.kt).

## A3. Reduced transparency changes appearance

Built-in material appearance resolves role tint to opaque, stops optical effects and local
adaptation sampling, and flattens progressive masking. The source and host architecture remain
available to the composition. Explicit component effects are subject to this appearance policy.
This policy is applied by the Material appearance layer; low-level rendering primitives remain the
caller's responsibility.

## A4. Increased contrast applies to semantic colors and materials

The optional theme selects an explicit `increasedContrastColorScheme` or derives one from the
configured base scheme. Nested themes retain the base separately, so disabling the setting restores
it and derivation does not accumulate. An explicit new base starts its own derived scheme unless a
high-contrast scheme is also supplied.

`ColorScheme.withIncreasedContrast` uses Material HCT tone and contrast algorithms to adjust
foregrounds against their background families and moves backgrounds only when necessary.
The color helpers treat background colors as opaque; contrast against a translucent surface must
use its final composited background. The project targets text contrast of 7:1 and necessary
boundaries of 3:1. These are library defaults, not a claim that Apple requires 7:1. Explicit high
contrast schemes and custom content colors remain the caller's responsibility.

Built-in materials evaluate their final tint and foreground together. With a known source, tint
opacity covers the full SDR backdrop range; unknown sampling and source-free drawing use an opaque
fallback. Reduced transparency independently requires opacity and disables optical adaptation.

Sources: [semantic colors](../../material3/src/commonMain/kotlin/Contrast.kt) and
[material appearance](../../material-backdrop/src/commonMain/kotlin/BackdropSurface.kt).
Algorithm provenance: [ported Material Color Utilities sources](../../ui/src/commonMain/kotlin/color/Contrast.kt).
Regression: [color families](../../material3/src/commonTest/kotlin/ContrastTest.kt).

## A5. Policy chooses motion; the scheme supplies its timing

Configured accessibility overrides resolve before effect switches. Ordinary switches can disable
an allowed effect but cannot enable an effect prohibited by accessibility. Consumers read these
`@Composable inline` getters rather than merging preferences themselves:

| Policy | Effective rule |
| --- | --- |
| `BackgroundTransparencyEnabled` | Reduced transparency is off |
| `SpatialMotionEnabled` | Reduced motion is off |
| `IncreasedContrastEnabled` | Increased contrast is on |
| `BackdropBlurEnabled` | Master blur and background transparency are enabled |
| `BlurAnimationEnabled` | Master blur, animation blur, and spatial motion are enabled |
| `BackdropBlurAnimationEnabled` | Backdrop blur and animation blur are enabled |

Reduced motion substitutes fades for programmatic spatial presentation and content replacement.
Animated blur, shared geometry, and ornamental scale or rotation stop. Direct manipulation remains
responsive to the gesture. Necessary feedback uses effects transitions, subject to platform animation
timing. The motion scheme supplies explicit spatial or effects specs at Material call sites. Core
accepts specs and has no Material dependency. Bounded render values are clamped at consumption.

`M3xMaterialTheme` preserves the upstream Expressive motion parameters. Upstream generic springs
do not infer a consumer's physical units or value range: an omitted completion threshold falls back
to the animation vector default. M3X supplies missing, unit-appropriate thresholds at integration
boundaries, including native tolerances for typed geometry. For normalized corner interpolation,
convert a one-pixel corner tolerance to progress units using the largest endpoint corner difference
at the measured panel size. This avoids a nearly stationary tail caused by an unnecessarily small
normalized threshold. Threshold adjustment preserves spring stiffness and damping; explicit caller
thresholds and non-spring specifications remain authoritative. Existing opacity and scale thresholds
remain unchanged.
Nested M3X themes inherit the current scheme; an explicit `motionScheme` supplies the replacement.

Sources: [typed motion thresholds](../../material3/src/commonMain/kotlin/SharedBoundsMotion.kt) and
[corner progress tolerance](../../material-overlay/src/commonMain/kotlin/MenuTransitionShape.kt).
Regressions: [theme inheritance](../../material-theming/src/commonTest/kotlin/M3xThemeExtensionsTest.kt)
and [corner motion](../../material-overlay/src/nonBrowserTest/kotlin/MenuTransitionShapeTest.kt).

Animations use Compose's native transitions, animation state helpers, and coroutine scopes.
`ProvideAccessibility` resolves settings; it does not override animation duration scales.
Some platforms enforce system animation timing independently of the library's preferences.
On iOS, system Reduce Motion forces Compose's duration scale to zero. Android similarly follows
its system animator-duration scale. When the platform disables animation this way, the library's
Reduce motion switch is ineffective at restoring motion, and replacement fades may finish
immediately. The library cannot perfectly restore animation behavior on these platforms.

Sources: [effective policy](../../ui/src/commonMain/kotlin/accessibility/AccessibilityPolicy.kt)
and [native visibility integration](../../animation/src/commonMain/kotlin/BlurAnimation.kt).
Regression: [effective policy combinations](../../animation/src/jvmTest/kotlin/AccessibilityPolicyTest.kt).

## Shared interaction light

### L1. One scope connects participating surfaces

The theme and `BackdropHost` join an enclosing interaction-light scope with `isolate = false`.
This lets physical input on one participating surface illuminate other receivers in that scope.
`M3xBackdropLighting` sets global enablement, source color/radius, and interior/edge receiving
opacity. Source ownership and receiver appearance are separate settings.

### L2. Both the role and theme control participation

With a captured source, a surface emits and receives when theme lighting is enabled and its material's
`interactiveLighting` is true. Scroll edges opt out by default; card surface materials participate
without adding their own elevation decorations. Physical pointer input is observed directly.
A shared `InteractionSource` supplies synthetic press and focus lighting; absent one, the modifier
chain observes focus and keyboard activation. Reducing motion shortens release rather than
inventing a different material role.

Source: [source and receiver wiring](../../material-backdrop/src/commonMain/kotlin/BackdropSurface.kt).
Regressions: [input lighting](../../material/src/nonBrowserTest/kotlin/InteractionLightTest.kt) and
[shared-light rendering](../../material-backdrop/src/nonBrowserTest/kotlin/SharedLightRenderingTest.kt).

## A6. Entrance and exit blur follows component ownership

`LocalBlurAnimationEnabled` controls transition blur independently of static backdrop blur.
`LocalBlurEnabled` is the master blur switch. Disabled or unsupported foreground blur preserves
fading where the transition owns opacity; the animation switch leaves static material blur available.

A visibility transition applies finite blur and fading to the whole unshared container so it can
fully disappear. Shared bounds own container geometry and crossfading; inner non-shared content
adds blur with `fade = false`, inheriting opacity once. Individually shared descendants retain
their own transition ownership. Scrims fade independently. Components with spatial presentation
contracts, such as a bottom sheet's slide and anchoring, use no added blur layer. Reduced motion
replaces programmatic presentation with fading while preserving direct dragging.

Bounded Backdrop transitions have a different optical contract. With blur motion enabled, the
radius varies within the material's range and the mask fades, without a whole-background fade.
With it disabled, the material stays at full strength and the whole background fades. Component
providers and callers own adherence to this contract; foreground blur defaults do not determine
the material radius. Adaptive material blur also holds a fixed radius when blur motion is disabled;
color adaptation may continue through effects transitions.

Press deformation and velocity settle through spatial specs independently of optical progress.
Optical fading cannot truncate a geometric release; reduced motion omits decorative settling.
Selection placement keeps tinted content aligned with its source when motion snaps to a resting
position. Velocity-driven deformation returns to zero when position motion completes, including
interrupted or reversed drags. A queued velocity update cannot replace that final resting target.

Shared container geometry and matched text use the caller's slow spatial spec; their crossfades
and modal scrims use slow effects specs. Effects specs must avoid overshoot for bounded values.
Corner interpolation belongs to the visibility transition and uses effects motion without bounce;
surface and clipping share its shape.

Core helpers read no Material theme. Callers supply animation specifications and radius overrides;
`DefaultEnterExitBlurRadius` provides the shared fallback. Theme-aware callers pass
`MaterialTheme.extended.enterExitBlurRadius` and their chosen `MaterialTheme.motionScheme` specs.
A zero radius preserves fading without a render effect. Theme inheritance follows
[theme composition](theme.md#t3-nested-themes-inherit-by-input-domain).

`Modifier.blurTransition` applies optical progress without owning layout or input. The context-bound
`Modifier.blurEnterExit` registers its animation with visibility so exiting content remains
composed until it finishes. Custom transitions supply spatial motion or explicitly take over
opacity with `fade = false` to avoid multiplying fades.

Sources: [optical helpers](../../animation/src/commonMain/kotlin/BlurAnimation.kt),
[host integration](../../material-overlay/src/commonMain/kotlin/OverlayBlur.kt), and
[bounded background motion](../../material-backdrop/src/commonMain/kotlin/BackdropAppBar.kt).
Regressions: [optical ownership](../../material/src/nonBrowserTest/kotlin/BlurTransitionTest.kt),
[container rendering](../../material/src/nonBrowserTest/kotlin/ContainerBlurRenderingTest.kt),
[shared crossfading](../../material/src/nonBrowserTest/kotlin/SharedContainerFadeTest.kt),
[shared motion specs](../../material/src/nonBrowserTest/kotlin/SharedMotionSchemeTest.kt),
[press and release continuity](../../material-backdrop/src/nonBrowserTest/kotlin/FloatingBottomBarMotionTest.kt),
[editor integration](../../settings-page-material/src/nonBrowserTest/kotlin/CardSettingsTest.kt), and
[bounded background fallback](../../material-backdrop/src/nonBrowserTest/kotlin/BackdropScrollPresenceTest.kt).

## Platform interpretation

The three settings follow Apple's distinct accessibility purposes. Platform adapters report public
measurable signals and retain unknown values where unavailable. Runtime changes use the same
resolution path as application overrides.

References: [reduced motion](https://developer.apple.com/help/app-store-connect/manage-app-accessibility/reduced-motion-evaluation-criteria/),
[visual accessibility](https://developer.apple.com/videos/play/wwdc2020/10020/), and
[sufficient contrast](https://developer.apple.com/help/app-store-connect/manage-app-accessibility/sufficient-contrast-evaluation-criteria/).
