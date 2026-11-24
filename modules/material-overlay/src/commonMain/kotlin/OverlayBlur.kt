package top.ltfan.m3x.material.overlay

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.ltfan.m3x.compose.animation.blurEnterExit
import top.ltfan.m3x.material.theme.extended
import top.ltfan.m3x.overlay.LocalOverlayAnimatedVisibilityScope

/**
 * Connects hosted content to entry optics, with opacity owned by either this layer or shared
 * bounds.
 */
@Composable
internal fun Modifier.overlayBlur(enabled: Boolean = true, fade: Boolean = true): Modifier {
    if (!enabled) return this
    val visibility = LocalOverlayAnimatedVisibilityScope.current ?: return this
    return context(visibility) {
        blurEnterExit(
            maxRadius = MaterialTheme.extended.enterExitBlurRadius,
            animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
            fade = fade,
        )
    }
}
