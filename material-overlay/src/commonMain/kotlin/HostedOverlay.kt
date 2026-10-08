package top.ltfan.m3x.material.overlay

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import top.ltfan.m3x.material.LocalFloatingBottomBarRenderer
import top.ltfan.m3x.material.LocalMaterialSurfaceRenderer
import top.ltfan.m3x.material.theme.LocalM3xThemeExtensions
import top.ltfan.m3x.overlay.Overlay
import top.ltfan.m3x.overlay.OverlayBackHandler
import top.ltfan.m3x.overlay.OverlayHandle
import top.ltfan.m3x.overlay.OverlayInteraction
import top.ltfan.m3x.overlay.OverlayNavigationTransition

/** Presents Material content with caller design values while the host owns rendering and input. */
@Composable
public fun MaterialOverlay(
    entry: OverlayHandle,
    modal: Boolean,
    visible: () -> Boolean = { true },
    interaction: OverlayInteraction? = null,
    backHandler: OverlayBackHandler? = null,
    sharedContentState: SharedContentState? = null,
    navigationTransition: OverlayNavigationTransition? = null,
    navigationContentKey: Any? = sharedContentState?.key,
    bindAnimation: @Composable () -> Unit = {},
    immediateContent: Boolean = false,
    enter: EnterTransition = EnterTransition.None,
    exit: ExitTransition = ExitTransition.None,
    prepareForOwnerExit: (suspend () -> Unit)? = null,
    onExit: (suspend () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val motionScheme = MaterialTheme.motionScheme
    val shapes = MaterialTheme.shapes
    val typography = MaterialTheme.typography
    val barRenderer = LocalFloatingBottomBarRenderer.current
    val renderer = LocalMaterialSurfaceRenderer.current
    val surfaceValues = renderer.captureEnvironment()
    val extensions = LocalM3xThemeExtensions.current
    val callerValues: @Composable (@Composable () -> Unit) -> Unit = { hostedContent ->
        MaterialTheme(
            colorScheme = colorScheme,
            motionScheme = motionScheme,
            shapes = shapes,
            typography = typography,
        ) {
            CompositionLocalProvider(
                LocalM3xThemeExtensions provides extensions,
                LocalMaterialSurfaceRenderer provides renderer,
                LocalFloatingBottomBarRenderer provides barRenderer,
                content = { surfaceValues(hostedContent) },
            )
        }
    }

    Overlay(
        entry,
        modal,
        visible,
        interaction,
        backHandler,
        sharedContentState,
        navigationTransition,
        navigationContentKey,
        bindAnimation,
        immediateContent,
        enter,
        exit,
        prepareForOwnerExit,
        onExit,
        scrimAnimationSpec = motionScheme.slowEffectsSpec(),
        provideCallerValues = callerValues,
        content = content,
    )
}
