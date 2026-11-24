package top.ltfan.m3x.material.theming

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import top.ltfan.m3x.compose.material3.withIncreasedContrast
import top.ltfan.m3x.compose.ui.accessibility.AccessibilityPreferences
import top.ltfan.m3x.compose.ui.accessibility.BackgroundTransparencyEnabled
import top.ltfan.m3x.compose.ui.accessibility.IncreasedContrastEnabled
import top.ltfan.m3x.compose.ui.accessibility.LocalAccessibilityPreferences
import top.ltfan.m3x.compose.ui.accessibility.ProvideAccessibility
import top.ltfan.m3x.compose.ui.visual.DefaultEnterExitBlurRadius
import top.ltfan.m3x.material.theme.CornerRadiiPolicy
import top.ltfan.m3x.material.theme.ExpressiveCornerRadii
import top.ltfan.m3x.material.theme.LocalM3xThemeExtensions
import top.ltfan.m3x.material.theme.M3xShapeFactory
import top.ltfan.m3x.material.theme.M3xShapes
import top.ltfan.m3x.material.theme.M3xThemeExtensions
import top.ltfan.m3x.material.theme.RoundedM3xShapeFactory
import top.ltfan.m3x.material.theme.rememberAdaptiveCornerRadii
import top.ltfan.m3x.material.theme.toMaterialShapes
import top.ltfan.m3x.material.theme.toShapeExtensions

private data class ThemeColors(
    val base: ColorScheme,
    val increasedContrast: ColorScheme?,
    val displayed: ColorScheme,
)

private val LocalThemeColors = staticCompositionLocalOf<ThemeColors?> { null }

/**
 * Provides an expressive Material theme and resolves M3X extension values for its subtree.
 *
 * At the root, `null` [shapes] selects an adaptive rounded preset. Nested themes inherit the
 * extension shapes and current native Material 3 shape roles. An explicit preset supplies both its
 * native roles and M3X extension shapes.
 *
 * @param colorScheme the base palette, or `null` to inherit the enclosing base palette or use the
 *   Expressive root default.
 * @param increasedContrastColorScheme a dedicated high-contrast palette, or `null` to inherit it
 *   with the enclosing base palette or derive from the selected base.
 * @param motionScheme the motion scheme, or `null` to inherit a nested M3X theme or use the
 *   Expressive default at the root.
 * @param shapes the M3X shapes to use, or `null` to inherit or select the root adaptive preset.
 * @param shapeFactory the factory for generated roles, or `null` to inherit the current factory. An
 *   explicit [shapes] preset takes precedence.
 * @param cornerRadiiPolicy the corner measurement policy, or `null` to inherit the current policy.
 * @param typography the typography, or `null` for Expressive theme inheritance and defaults.
 * @param enterExitBlurRadius the maximum foreground entrance/exit blur radius, or `null` to inherit
 *   or use [DefaultEnterExitBlurRadius]. Zero retains only fading. Backdrop material radii remain
 *   independent.
 * @param accessibility the independent accessibility preferences to use, or `null` to inherit or
 *   use system defaults.
 * @param rootShapeFactory fallback factory used only at a root without [shapes] or [shapeFactory].
 *   Nested themes inherit their parent factory; explicit presets and factories take precedence.
 * @param content the composable content styled with this theme.
 */
@Composable
public fun M3xBaseTheme(
    colorScheme: ColorScheme? = null,
    increasedContrastColorScheme: ColorScheme? = null,
    motionScheme: MotionScheme? = null,
    shapes: M3xShapes? = null,
    shapeFactory: M3xShapeFactory? = null,
    cornerRadiiPolicy: CornerRadiiPolicy? = null,
    typography: Typography? = null,
    enterExitBlurRadius: Dp? = null,
    accessibility: AccessibilityPreferences? = null,
    rootShapeFactory: M3xShapeFactory = RoundedM3xShapeFactory,
    content: @Composable () -> Unit,
) {
    val parentColors = LocalThemeColors.current
    val inheritsBaseColors = parentColors?.displayed === MaterialTheme.colorScheme
    val parent = LocalM3xThemeExtensions.current
    val rootPreset =
        if (shapes == null && parent == null) {
            val radii = rememberAdaptiveCornerRadii(cornerRadiiPolicy ?: CornerRadiiPolicy.Auto)
            remember(radii, shapeFactory, rootShapeFactory) {
                M3xShapes(
                    cornerRadii = radii,
                    factory = shapeFactory ?: rootShapeFactory,
                )
            }
        } else {
            null
        }
    val configuredPreset =
        if (
            shapes == null && parent != null && (shapeFactory != null || cornerRadiiPolicy != null)
        ) {
            M3xShapes(
                cornerRadii =
                    if (cornerRadiiPolicy == null) {
                        parent.shapes.cornerRadii ?: ExpressiveCornerRadii
                    } else {
                        rememberAdaptiveCornerRadii(cornerRadiiPolicy)
                    },
                factory = shapeFactory ?: parent.shapes.factory,
            )
        } else {
            null
        }
    val preset = shapes ?: rootPreset ?: configuredPreset
    val nativeShapes = preset?.toMaterialShapes() ?: MaterialTheme.shapes
    val extensionShapes =
        preset?.toShapeExtensions() ?: parent?.shapes ?: error("A root shape preset is required.")
    val configuredAccessibility = accessibility ?: LocalAccessibilityPreferences.current
    val extensions =
        M3xThemeExtensions(
            shapes = extensionShapes,
            enterExitBlurRadius =
                enterExitBlurRadius ?: parent?.enterExitBlurRadius ?: DefaultEnterExitBlurRadius,
            accessibilityPreferences = configuredAccessibility,
        )
    CompositionLocalProvider(LocalM3xThemeExtensions provides extensions) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme ?: parentColors?.base?.takeIf { inheritsBaseColors },
            motionScheme = motionScheme,
            shapes = nativeShapes,
            typography = typography,
            content = {
                val baseColors = MaterialTheme.colorScheme
                val contrastColors =
                    increasedContrastColorScheme
                        ?: parentColors?.increasedContrast?.takeIf {
                            colorScheme == null && inheritsBaseColors
                        }
                ProvideAccessibility(configuredAccessibility) {
                    val reduceTransparency = !BackgroundTransparencyEnabled
                    val increasedContrast = IncreasedContrastEnabled
                    val derivedColors =
                        remember(baseColors, contrastColors, increasedContrast) {
                            if (increasedContrast)
                                contrastColors ?: baseColors.withIncreasedContrast()
                            else baseColors
                        }
                    val colorConfiguration =
                        remember(baseColors, contrastColors, derivedColors) {
                            ThemeColors(baseColors, contrastColors, derivedColors)
                        }
                    CompositionLocalProvider(LocalThemeColors provides colorConfiguration) {
                        MaterialTheme(colorScheme = derivedColors) {
                            content()
                        }
                    }
                }
            },
        )
    }
}
