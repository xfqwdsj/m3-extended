package top.ltfan.m3x.compose.animation.core

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * Supplies a type- and unit-specific completion threshold for a spring that omits one.
 *
 * The returned spring preserves damping and stiffness. Explicit thresholds and non-spring
 * specifications retain their original instances. Remember the result at composition call sites
 * that reuse it across frames.
 *
 * @param threshold completion tolerance in the animated value's units
 */
public fun <T> AnimationSpec<T>.withDefaultVisibilityThreshold(threshold: T): AnimationSpec<T> =
    if (this is SpringSpec && visibilityThreshold == null) {
        spring(
            dampingRatio = dampingRatio,
            stiffness = stiffness,
            visibilityThreshold = threshold,
        )
    } else {
        this
    }

/** Preserves the finite specification type when supplying a missing spring threshold. */
public fun <T> FiniteAnimationSpec<T>.withDefaultVisibilityThreshold(
    threshold: T,
): FiniteAnimationSpec<T> =
    (this as AnimationSpec<T>).withDefaultVisibilityThreshold(threshold) as FiniteAnimationSpec<T>
