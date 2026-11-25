package top.ltfan.m3x.settingspage.material

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.lerp
import top.ltfan.m3x.compose.ui.accessibility.SpatialMotionEnabled

/** Morphs the base shape as visible rows gain or lose a section edge. */
@Composable
internal fun settingsSegmentShapes(first: Boolean, last: Boolean): ListItemShapes {
    val defaults = ListItemDefaults.shapes()
    val inner = defaults.shape as? CornerBasedShape
    val outer = ListItemDefaults.segmentedShapes(index = 0, count = 1).shape as? CornerBasedShape
    if (!SpatialMotionEnabled || inner == null || outer == null) {
        val index = if (first) 0 else if (last) 2 else 1
        return ListItemDefaults.segmentedShapes(index, if (first && last) 1 else 3, defaults)
    }

    val spec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val top = animateFloatAsState(if (first) 1f else 0f, spec, label = "settings-top-corners")
    val bottom = animateFloatAsState(if (last) 1f else 0f, spec, label = "settings-bottom-corners")
    // Keep the base shape stable while its corners animate so Material can still morph it on press.
    return remember(defaults, inner, outer, top, bottom) {
        defaults.copy(
            shape =
                inner.copy(
                    topStart = SegmentCornerSize(inner.topStart, outer.topStart, top),
                    topEnd = SegmentCornerSize(inner.topEnd, outer.topEnd, top),
                    bottomStart = SegmentCornerSize(inner.bottomStart, outer.bottomStart, bottom),
                    bottomEnd = SegmentCornerSize(inner.bottomEnd, outer.bottomEnd, bottom),
                ),
        )
    }
}

private class SegmentCornerSize(
    private val inner: CornerSize,
    private val outer: CornerSize,
    private val progress: State<Float>,
) : CornerSize {
    override fun toPx(shapeSize: Size, density: Density): Float =
        lerp(
            inner.toPx(shapeSize, density),
            outer.toPx(shapeSize, density),
            progress.value.coerceIn(0f, 1f),
        )
}
