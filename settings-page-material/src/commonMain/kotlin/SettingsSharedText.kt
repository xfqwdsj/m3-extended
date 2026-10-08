package top.ltfan.m3x.settingspage.material

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import top.ltfan.m3x.compose.material3.sharedBoundsSpec
import top.ltfan.m3x.compose.ui.accessibility.SpatialMotionEnabled
import top.ltfan.m3x.overlay.LocalSharedTransitionScope

/**
 * One endpoint of the shared texts of a settings item in the settings shared transition.
 *
 * A settings row and its editor render some texts from the same string: the item label, the item
 * description, and the display string of the stored value of a Selector. [SettingsRow] and
 * [SettingsItemEditor] attach those texts to a shared bounds of their own when this is provided, so
 * one string travels between the row and the editor while the shared bounds of the item animates.
 * Texts that only one side renders, such as the selected count of a MultiSelector or a value inside
 * a text field, stay out of the shared transition.
 *
 * Both endpoints of a pair carry the same [keyPrefix], which the item ID keeps unique across a
 * settings tree.
 *
 * @param keyPrefix the shared content key prefix of the settings item
 * @param animatedVisibilityScope the visibility scope of this endpoint, which decides when it takes
 *   part in the shared animation
 */
@Stable
public class SettingsSharedText(
    public val keyPrefix: String,
    public val animatedVisibilityScope: AnimatedVisibilityScope,
) {
    /** The shared content key of the item label shown in the row and in the editor title. */
    public fun labelKey(): String = "$keyPrefix:label"

    /** The shared content key of the item description shown on both sides. */
    public fun descriptionKey(): String = "$keyPrefix:description"

    /** The shared content key of the display string of the stored value with [valueId]. */
    public fun valueKey(valueId: String): String = "$keyPrefix:value:$valueId"
}

@Composable
internal fun Modifier.settingsSharedLabel(sharedText: SettingsSharedText?): Modifier {
    if (sharedText == null) return this
    return settingsSharedText(sharedText, sharedText.labelKey())
}

@Composable
internal fun Modifier.settingsSharedDescription(sharedText: SettingsSharedText?): Modifier {
    if (sharedText == null) return this
    return settingsSharedText(sharedText, sharedText.descriptionKey())
}

@Composable
internal fun Modifier.settingsSharedValue(
    sharedText: SettingsSharedText?,
    valueId: String,
): Modifier {
    if (sharedText == null) return this
    return settingsSharedText(sharedText, sharedText.valueKey(valueId))
}

/**
 * Marks the text as the [key] endpoint of a shared text. The bounds hug the text so the animated
 * bounds follow the glyphs instead of their container, and the content scales with the bounds
 * instead of reflowing while it travels.
 */
@Composable
private fun Modifier.settingsSharedText(sharedText: SettingsSharedText, key: String): Modifier {
    if (!SpatialMotionEnabled) return this
    val motionScheme = MaterialTheme.motionScheme
    val sharedTransitionScope = LocalSharedTransitionScope.current ?: return this
    with(sharedTransitionScope) {
        return wrapContentWidth(Alignment.Start)
            .sharedBounds(
                boundsTransform = { _, _ -> motionScheme.sharedBoundsSpec() },
                enter = fadeIn(MaterialTheme.motionScheme.slowEffectsSpec()),
                exit = fadeOut(MaterialTheme.motionScheme.slowEffectsSpec()),
                sharedContentState = rememberSharedContentState(key),
                animatedVisibilityScope = sharedText.animatedVisibilityScope,
                resizeMode = ResizeMode.scaleToBounds(),
            )
    }
}
