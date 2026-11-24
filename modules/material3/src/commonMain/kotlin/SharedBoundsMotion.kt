package top.ltfan.m3x.compose.material3

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.material3.MotionScheme
import androidx.compose.ui.geometry.Rect
import top.ltfan.m3x.compose.animation.core.withDefaultVisibilityThreshold

/**
 * The slow spatial specification for shared bounds in physical coordinates.
 *
 * A generic spring acquires Compose's native rectangle completion threshold, keeping its damping
 * and stiffness. Explicit spring thresholds and other animation specifications retain their values.
 */
public fun MotionScheme.sharedBoundsSpec(): FiniteAnimationSpec<Rect> {
    return slowSpatialSpec<Rect>().withDefaultVisibilityThreshold(Rect.VisibilityThreshold)
}
