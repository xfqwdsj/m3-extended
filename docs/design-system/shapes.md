# Shape system

## S1. Native and extension roles have separate homes

`M3xShapes` is the complete input preset. Its eight finite roles map to `MaterialTheme.shapes`.
`MaterialTheme.extended.shapes` holds `full`, `none`, physical `container`, the factory, and optional
radius metadata. Converting a custom preset does not invent measurement metadata.

| Finite role | Baseline radius |
| --- | --- |
| `extraSmall` | 4 dp |
| `small` | 8 dp |
| `medium` | 12 dp |
| `large` | 16 dp |
| `largeIncreased` | 20 dp |
| `extraLarge` | 28 dp |
| `extraLargeIncreased` | 32 dp |
| `extraExtraLarge` | 48 dp |

`full` follows the drawn bounds as a capsule/circle-style shape. `none` is rectangular. `container`
uses per-corner physical measurements. The finite roles follow layout direction; physical container
corners retain their screen positions in both LTR and RTL.

Source: [shape values and conversion](../../modules/material-theme/src/commonMain/kotlin/M3xShapes.kt).
Regressions: [preset contracts](../../modules/material-theming/src/commonTest/kotlin/ShapePresetTest.kt) and
[rendered geometry](../../modules/material-theming/src/composeTest/kotlin/ShapeRenderingTest.kt).

## S2. Factories preserve the role model

`M3xShapeFactory.logical` creates direction-aware corner shapes; `physical` creates absolute-corner
shapes. `RoundedM3xShapeFactory` uses standard rounded geometry; `CapsuleM3xShapeFactory` uses
continuous rounded geometry. Both provide `full` and `none`. Explicit shapes supplied to a preset
remain authoritative. Continuous geometry may normalize radii to fit the drawn bounds.

For a nested container-like surface, inset each physical radius by its corresponding surface
padding and clamp at zero. This is a composition rule for nested containers; standard Material
components retain their upstream sizing rules.

Source: [Capsule factory and preset helpers](../../modules/material-theming-capsule/src/commonMain/kotlin/CapsuleShapes.kt).

`ContourShape` is an optional structured extension of Compose `Shape`. It reports ordered closed
loops made from lines, circular arcs, and cubic Béziers, or returns null when the shape has no
structured form. Logical and physical continuous rounded rectangles implement this contract.
Backdrop uses the contour for optimized boundary work and falls back to the shape's ordinary
`Outline` when no contour is available.

Source: [contour geometry](../../modules/shape/src/commonMain/kotlin/ContourShape.kt) and
[Capsule contours](../../modules/capsule/src/commonMain/kotlin/Contour.kt).
Regressions: [shape contours](../../modules/capsule/src/commonTest/kotlin/ContourTest.kt) and
[highlight geometry](../../modules/backdrop/src/jvmTest/kotlin/ContourShapeTest.kt).

## S3. Adaptation chooses one measurement source

| Policy | Selected source |
| --- | --- |
| `Auto`, mobile and confirmed `Fullscreen` | Display |
| `Auto`, desktop or other/unknown mobile presentation | Window |
| `Display` | Display |
| `Window` | Window |

Unavailable values remain unknown. Auto does not borrow display corners when its selected window
source is unavailable. `M3xCornerRadii` retains both raw measurements, presentation, policy, and the
resolved source. Display and window are independent concepts.

Read them with `rememberDisplayCornerRadii`, `rememberWindowCornerRadii`, and
`rememberWindowPresentation` from `ui`. Hosts may override the two readers separately
with their hooks. A null hook selects the public platform reader; a hook returning null explicitly
reports unknown. Optional Apple probes live in `ui-platform-private`; native macOS hosts
provide their `NSWindow` through `ProvideWindowCornerRadii`.

Source: [public measurement hooks](../../modules/ui/src/commonMain/kotlin/layout/CornerRadii.kt)
and [adaptive policy](../../modules/material-theme/src/commonMain/kotlin/M3xCornerRadii.kt).

## S4. Unknown, invalid, and zero values have distinct meanings

Finite non-negative measurements are usable, including zero. Missing or invalid corners are filled
with the largest usable corner from the selected source. With no usable measurement, all resolved
corners use `fallbackContainerRadius` (32 dp by default) and `source` reports `Fallback`.
The fallback argument must be finite and positive.

`containerRadius` is the largest usable corner or the fallback. Finite role sizes use
`containerRadius / 32`, clamped to 0.75–1.5. The physical container retains the resolved measured
corners, including true zero and asymmetry, independently of that scale clamp. Apple measurements
convert host points using backing scale and Compose density, without font scale.

Regression: [radius policy, invalid values, asymmetry, and scale](../../modules/material-theme/src/commonTest/kotlin/M3xCornerRadiiTest.kt).

## S5. Interactive shapes keep Material state behavior

Theme-aware component defaults resolve full silhouettes through `MaterialTheme.extended.shapes.full`.
Native Material roles remain owned by `MaterialTheme.shapes`. Explicit component shapes and slots
keep precedence over either default. Component names match their Material counterparts; use the
M3X package for the theme-aware entry points. Color and sizing factories remain provided by
Material defaults.

Interaction shape sets retain upstream pressed, selected, and dragged states unless the state's
shape is itself a full silhouette. For example, a toggle button retains its distinct checked shape,
while a selected chip uses the theme's full shape. Material interpolates corner-based shapes;
other shape types change directly between states.

Connected controls resolve outer corners from a corner-based full shape and preserve upstream
inner corners. Their partial shapes use the theme factory. When full does not expose corner sizes,
the factory derives the outside corners from the upstream relative corner sizes.

Custom drawing remains owned by Material: replacing a slider's default thumb does not replace its
track path, tick drawing, or gesture/state implementation. Explicit custom thumbs and tracks pass
through. Vertical controls pass their orientation to their default thumbs; standalone thumbs supply
`isVertical` explicitly.

Material owns time picker rendering and the interaction shapes of its AM/PM selector.

Source: [component defaults](../../modules/material/src/commonMain/kotlin/ToggleButton.kt).
Regressions: [inheritance and overrides](../../modules/material/src/composeTest/kotlin/ThemeShapeDefaultsTest.kt),
[connected geometry](../../modules/material/src/composeTest/kotlin/ConnectedShapeFactoryTest.kt), and
[rendered slots](../../modules/material/src/jvmTest/kotlin/ThemeShapeSlotIntegrationTest.kt).

## S6. Segmented lists shape the visible section edges

Material supplies the default outer and inner corner shapes. A single row takes the outer
corners on both ends. Section headings sit outside the row count.
In settings, retained exiting rows keep their previous corner targets while the rows that remain
visible take their new section positions.

Sources: [segmented list group](../../modules/material/src/commonMain/kotlin/SegmentedListGroup.kt) and
[settings segment shape](../../modules/settings-page-material/src/commonMain/kotlin/SettingsSegmentShape.kt).
Regression: [segmented sections](../../modules/material/src/composeTest/kotlin/SegmentedListGroupTest.kt).
