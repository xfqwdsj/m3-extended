package top.ltfan.m3x.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Transition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/** Maps visibility directly on the original root, retaining its native EnterExit child timeline. */
public class OverlayVisibilityTransition
internal constructor(
    internal val isPresent: () -> Boolean,
    internal val isHidden: () -> Boolean,
    internal val render:
        @Composable
        (
            (Boolean) -> Boolean,
            EnterTransition,
            ExitTransition,
            @Composable AnimatedVisibilityScope.() -> Unit,
        ) -> Unit,
) {
    @Composable
    public fun AnimatedVisibility(
        visible: (Boolean) -> Boolean,
        enter: EnterTransition,
        exit: ExitTransition,
        content: @Composable AnimatedVisibilityScope.() -> Unit,
    ) {
        render(visible, enter, exit, content)
    }
}

@Composable
internal fun <T> rememberOverlayVisibilityTransition(
    root: Transition<T>,
    isVisible: (T) -> Boolean,
): OverlayVisibilityTransition {
    val mapping by rememberUpdatedState(isVisible)
    return remember(root) {
        OverlayVisibilityTransition(
            isPresent = {
                mapping(root.currentState) || mapping(root.targetState) || root.isRunning
            },
            isHidden = { !mapping(root.currentState) && !mapping(root.targetState) },
            render = { visible, enter, exit, content ->
                root.AnimatedVisibility(
                    visible = { visible(mapping(it)) },
                    enter = enter,
                    exit = exit,
                    content = content,
                )
            },
        )
    }
}
