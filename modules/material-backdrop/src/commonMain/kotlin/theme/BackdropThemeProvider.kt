package top.ltfan.m3x.material.backdrop.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import top.ltfan.m3x.backdrop.BackdropBlur
import top.ltfan.m3x.backdrop.BackdropLens
import top.ltfan.m3x.backdrop.BackdropStyle
import top.ltfan.m3x.backdrop.LocalBackdropStyle
import top.ltfan.m3x.backdrop.ProvideBackdropRenderStyle
import top.ltfan.m3x.backdrop.ProvideBackdropStyle
import top.ltfan.m3x.backdrop.interaction.InteractionLighting
import top.ltfan.m3x.backdrop.styledSurface
import top.ltfan.m3x.compose.ui.accessibility.BackgroundTransparencyEnabled
import top.ltfan.m3x.compose.ui.visual.BackdropBlurEnabled
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.theme.M3xThemeExtensions
import top.ltfan.m3x.material.theme.extended

/** Backdrop design values inherited independently from the base design contracts. */
public val LocalM3xBackdropTheme: ProvidableCompositionLocal<M3xBackdropTheme?> =
    staticCompositionLocalOf {
        null
    }

/** Reads the current Backdrop theme through the Material theme extension namespace. */
@Suppress("UnusedReceiverParameter")
public val M3xThemeExtensions.backdrop: M3xBackdropTheme
    @Composable @ReadOnlyComposable get() = LocalM3xBackdropTheme.current ?: M3xBackdropTheme()

/** Overrides one material role for a component subtree while inheriting the remaining theme. */
@Composable
public fun ProvideBackdropMaterial(
    role: MaterialSurfaceRole,
    material: M3xBackdropMaterial,
    content: @Composable () -> Unit,
) {
    val parent = MaterialTheme.extended.backdrop
    val backdrop =
        when (role) {
            MaterialSurfaceRole.Control -> parent.copy(control = material)
            MaterialSurfaceRole.Panel -> parent.copy(panel = material)
            MaterialSurfaceRole.Surface -> parent.copy(surface = material)
            MaterialSurfaceRole.InversePanel -> parent.copy(inversePanel = material)
            MaterialSurfaceRole.ScrollEdge -> parent.copy(scrollEdge = material)
        }
    ProvideBackdropTheme(backdrop, content)
}

/**
 * Replaces the Backdrop design values for a subtree without selecting renderers or creating hosts.
 */
@Composable
public fun ProvideBackdropTheme(theme: M3xBackdropTheme, content: @Composable () -> Unit) {
    val material = theme.control
    val transparent = BackgroundTransparencyEnabled
    val blurEnabled = BackdropBlurEnabled
    val resolved = material.style.resolveAgainst(LocalBackdropStyle.current)
    val style =
        if (!transparent) BackdropStyle.NoEffects { insets = resolved.insets }
        else
            resolved {
                if (!blurEnabled) blur = { BackdropBlur.None }
                if (!theme.lighting.enabled || !material.interactiveLighting)
                    interactionLighting = { InteractionLighting.None }
            }
    CompositionLocalProvider(LocalM3xBackdropTheme provides theme) {
        ProvideBackdropStyle(style) {
            ProvideBackdropRenderStyle(
                render =
                    material.render
                        ?: {
                            if (shape is CornerBasedShape) styledSurface()
                            else styledSurface(BackdropStyle { lens = { BackdropLens.None } })
                        },
                insets = style.insets!!,
                content = content,
            )
        }
    }
}
