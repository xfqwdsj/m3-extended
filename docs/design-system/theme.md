# Theme composition

## T1. Material owns native design values

The Material UI is designed and tested for `MaterialExpressiveTheme`. Components read the caller's
`MaterialTheme.colorScheme`, `typography`, `motionScheme`, and eight native shape roles.
The overlay portal forwards these values from the originating
composition. Behavior under an ordinary Material 3 theme is outside the documented
Expressive integration contract.

`MaterialTheme.extended` supplies M3X design values while native Material values retain their own
owner. Without a provider it returns baseline extension values; installing runtime providers and
rendering hosts remains the application's responsibility.

Source: [extension values](../../material-theme/src/commonMain/kotlin/M3xThemeExtensions.kt).

## T2. The optional theme establishes a complete root

`M3xMaterialTheme` wraps `MaterialExpressiveTheme`. With no shape arguments at the root it
selects adaptive Capsule shapes using `CornerRadiiPolicy.Auto`. Backdrop defaults are Glass Regular.
`M3xBaseTheme` provides the independent rounded theme without selecting rendering adapters.
The base color scheme and typography use `MaterialExpressiveTheme` defaults at
the root. The accessibility provider selects the effective contrast variant without replacing the
motion scheme or changing the configured base palette. Root motion uses the upstream Expressive
scheme; nested M3X themes inherit the current Material motion scheme unless an explicit replacement
is supplied.

The theme supplies the control material's default Backdrop style and joins the shared interaction
lighting scope. Actual pixels still require a captured source supplied by `BackdropHost`.

Source: [M3xMaterialTheme](../../material-bundle/src/commonMain/kotlin/M3xMaterialTheme.kt).
Regression: [root and nested themes](../../material-theming/src/commonTest/kotlin/M3xThemeExtensionsTest.kt).

## T3. Nested themes inherit by input domain

| Input | Resolution |
| --- | --- |
| Explicit `shapes` | Supplies all eight native roles and M3X extension roles; wins over factory/policy arguments |
| No shape arguments in a nested M3X theme | Keeps current native Material roles and inherited extension shapes |
| Only `shapeFactory` | Rebuilds default roles with that factory and inherited radius metadata; baseline radii fill absent metadata |
| Explicit `cornerRadiiPolicy` | Reads adaptive measurements with that policy and uses the requested or inherited factory |
| `colorScheme = null` | Inherits the configured base scheme while the parent displays its contrast variant; a native color override establishes a new base |
| `increasedContrastColorScheme = null` | Inherits the parent override when inheriting its base; otherwise derives from the selected base |
| `M3xMaterialTheme(backdrop = null)` | Inherits the enclosing M3X backdrop, or uses the root default |
| `enterExitBlurRadius = null` | Inherits the enclosing foreground blur radius, or uses `DefaultEnterExitBlurRadius` (16 dp) at the root; zero keeps fading |
| `accessibility = null` | Reads the nearest `LocalAccessibilityPreferences`, including generic providers |

A nested native `MaterialTheme(shapes = ...)` changes native roles and leaves extension shapes and
radius metadata intact. Reconstruct the current complete preset with
`MaterialTheme.extended.shapes.toM3xShapes(MaterialTheme.shapes)` when an API needs `M3xShapes`.
Changing the backdrop preset preserves inherited shapes and independent accessibility preferences.

Regression: [theme resolution](../../material-theming/src/commonTest/kotlin/M3xThemeExtensionsTest.kt)
and [preference inheritance](../../material-theming/src/commonTest/kotlin/AccessibilityThemeTest.kt).

## T4. Overrides have an explicit scope

Use a complete `M3xShapes` preset for coordinated shape changes. Individual roles may still be
supplied independently when constructing it. Use `ProvideBackdropMaterial(role, material)` to
replace one role for a subtree while copying the rest of the extension theme. Use a component's
`material`/`backdropMaterial` parameter for one surface. Explicit component effects replace that
surface's complete optical chain; see [M2](materials.md#m2-effect-precedence).

Keep configured preference values in `MaterialTheme.extended.accessibilityPreferences`; read the
effective behavior through the
[accessibility policy getters](accessibility-and-motion.md#a5-policy-chooses-motion-the-scheme-supplies-its-timing).
This preserves the distinction between
static design inputs and runtime system state.

## T5. Continuous geometry is an optional dependency

`material-theme` defines M3X theme contracts, and `material-theming` integrates theme configuration.
Capsule integration belongs to `material-theming-capsule`. The complete `material-bundle`
composition retains the adaptive Capsule root default through `M3xMaterialTheme`. The production
dependencies of ordinary `material`, base theming, foundation and the settings stack do not pull in
Capsule. Select the complete composition or the Capsule adapter explicitly.

For example, choose a shape preset or supply a factory. An independent Backdrop composition uses
`material-theming`, `material-backdrop` and `overlay-backdrop` without the Capsule adapter:

```kotlin
val radii = rememberAdaptiveCornerRadii()
M3xMaterialTheme(shapes = capsuleShapes(radii)) { AppContent() }
M3xMaterialTheme(shapes = roundedShapes(ExpressiveCornerRadii)) { AppContent() }
M3xMaterialTheme(shapeFactory = RoundedM3xShapeFactory) { AppContent() }

M3xBaseTheme {
    ProvideBackdropTheme(M3xBackdropTheme()) {
        ProvideBackdropRendering {
            ProvideBackdropOverlay { AppContent() }
        }
    }
}

val complete = MaterialTheme.extended.shapes.toM3xShapes(MaterialTheme.shapes)
```

These are composable-body alternatives with application content and imports omitted.
Source: [theme-core dependencies](../../material-theme/build.gradle.kts),
[optional theming dependencies](../../material-theming/build.gradle.kts), and
[component dependencies](../../material/build.gradle.kts).
