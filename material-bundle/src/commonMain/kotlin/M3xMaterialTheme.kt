package top.ltfan.m3x.material.bundle

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import top.ltfan.backdrop.ProvideBackdropStyle
import top.ltfan.backdrop.effects.blur
import top.ltfan.backdrop.interaction.ProvideInteractionLighting
import top.ltfan.m3x.compose.ui.accessibility.AccessibilityPreferences
import top.ltfan.m3x.compose.ui.accessibility.BackgroundTransparencyEnabled
import top.ltfan.m3x.compose.ui.visual.BackdropBlurEnabled
import top.ltfan.m3x.compose.ui.visual.DefaultEnterExitBlurRadius
import top.ltfan.m3x.material.backdrop.theme.LocalM3xBackdropTheme
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropTheme
import top.ltfan.m3x.material.backdrop.theme.ProvideBackdropTheme
import top.ltfan.m3x.material.theme.CornerRadiiPolicy
import top.ltfan.m3x.material.theme.M3xShapeFactory
import top.ltfan.m3x.material.theme.M3xShapes
import top.ltfan.m3x.material.theming.M3xBaseTheme
import top.ltfan.m3x.material.theming.capsule.CapsuleM3xShapeFactory

/**
 * Provides an expressive Material theme and resolves M3X extension values for its subtree.
 *
 * At the root, `null` [shapes] selects an adaptive Capsule preset. Nested themes inherit the
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
 * @param backdrop the backdrop values to use, or `null` to inherit or use defaults.
 * @param enterExitBlurRadius the maximum foreground entrance/exit blur radius, or `null` to inherit
 *   or use [DefaultEnterExitBlurRadius]. Zero retains only fading. Backdrop material radii remain
 *   independent.
 * @param accessibility the independent accessibility preferences to use, or `null` to inherit or
 *   use system defaults.
 * @param content the composable content styled with this theme.
 */
@Composable
public fun M3xMaterialTheme(
    colorScheme: ColorScheme? = null,
    increasedContrastColorScheme: ColorScheme? = null,
    motionScheme: MotionScheme? = null,
    shapes: M3xShapes? = null,
    shapeFactory: M3xShapeFactory? = null,
    cornerRadiiPolicy: CornerRadiiPolicy? = null,
    typography: Typography? = null,
    backdrop: M3xBackdropTheme? = null,
    enterExitBlurRadius: Dp? = null,
    accessibility: AccessibilityPreferences? = null,
    content: @Composable () -> Unit,
) {
    val configuredBackdrop = backdrop ?: LocalM3xBackdropTheme.current ?: M3xBackdropTheme()
    M3xBaseTheme(
        colorScheme,
        increasedContrastColorScheme,
        motionScheme,
        shapes,
        shapeFactory,
        cornerRadiiPolicy,
        typography,
        enterExitBlurRadius,
        accessibility,
        rootShapeFactory = CapsuleM3xShapeFactory,
    ) {
        ProvideBackdropTheme(configuredBackdrop) {
            val reduceTransparency = !BackgroundTransparencyEnabled
            val material = configuredBackdrop.control
            val blurEnabled = BackdropBlurEnabled
            val style =
                material.style.copy(
                    effects = {
                        if (!reduceTransparency) {
                            if (blurEnabled) blur(material.blurRadius.toPx())
                            material.style.effects(this)
                        }
                    },
                )
            ProvideM3xMaterialRendering {
                ProvideBackdropStyle(style) {
                    ProvideInteractionLighting(isolate = false, content = content)
                }
            }
        }
    }
}
