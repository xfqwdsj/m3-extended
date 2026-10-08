package top.ltfan.m3x.material.backdrop

import androidx.compose.ui.unit.dp
import top.ltfan.backdrop.BackdropAxis
import top.ltfan.backdrop.BackdropRamp
import top.ltfan.backdrop.BackdropRampAnchors
import top.ltfan.backdrop.BackdropSide

/**
 * The intensity a top app bar's ramp has [y] px below its top, for a bar [height] px tall whose
 * content is inset by [insetTop], as [shape] measures it against the content that remains.
 *
 * A test states the shape it expects through this instead of the raw curve, so the bar's geometry
 * lives in one place and follows the theme's shape.
 */
internal fun appBarRampIntensity(
    y: Float,
    height: Float,
    insetTop: Float = 0f,
    ramp: BackdropRamp = AppBarTestRamp,
): Float {
    val content = height - insetTop
    // The axis a bar reports: 0 where its content side is — the bottom edge — and positive towards
    // its top edge.
    val axis =
        BackdropAxis(
            edge = BackdropSide.Top,
            occupied = 0f..height,
            content = 0f..content,
            density = 1f,
            fontScale = 1f,
        )
    val span = ramp.span(axis)
    val coordinate = height - y
    val position = (coordinate - span.start) / (span.endInclusive - span.start)
    return ramp.intensityAt(position.coerceIn(0f, 1f))
}

/** The ramp the theme starts a bar from, which these expectations mirror. */
internal val AppBarTestRamp: BackdropRamp =
    BackdropRamp(anchors = BackdropRampAnchors.offsets(plateau = 24.dp, fade = 24.dp))

/** The px below the top of a [height] px bar at which its ramp is spent. */
internal fun appBarRampEnd(
    height: Float,
    insetTop: Float = 0f,
    ramp: BackdropRamp = AppBarTestRamp,
): Float = height + (ramp.span(axisOf(height, insetTop)).start * -1f)

/** The axis a [height] px bar reports for [insetTop] of window insets. */
internal fun axisOf(height: Float, insetTop: Float = 0f): BackdropAxis =
    BackdropAxis(
        edge = BackdropSide.Top,
        occupied = 0f..height,
        content = 0f..(height - insetTop),
        density = 1f,
        fontScale = 1f,
    )
