package top.ltfan.m3x.material.theming.capsule

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.capsule.AbsoluteContinuousRoundedRectangle
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import top.ltfan.m3x.material.theme.ExpressiveCornerRadii
import top.ltfan.m3x.material.theme.M3xCornerRadii
import top.ltfan.m3x.material.theme.M3xShapeFactory
import top.ltfan.m3x.material.theme.M3xShapes
import top.ltfan.m3x.material.theme.RoundedM3xShapeFactory
import top.ltfan.m3x.material.theme.rememberAdaptiveCornerRadii

/** Continuous-rounded implementation for Capsule shape roles. */
public object CapsuleM3xShapeFactory : M3xShapeFactory {
    override fun logical(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize,
    ): CornerBasedShape = ContinuousRoundedRectangle(topStart, topEnd, bottomEnd, bottomStart)

    override fun physical(
        topLeft: CornerSize,
        topRight: CornerSize,
        bottomRight: CornerSize,
        bottomLeft: CornerSize,
    ): CornerBasedShape =
        AbsoluteContinuousRoundedRectangle(topLeft, topRight, bottomRight, bottomLeft)

    override val full: Shape = ContinuousCapsule
    override val none: Shape = RoundedM3xShapeFactory.none
}

/** Builds the continuous Capsule preset from an explicit radius scale. */
public fun capsuleShapes(radii: M3xCornerRadii = ExpressiveCornerRadii): M3xShapes =
    M3xShapes(cornerRadii = radii, factory = CapsuleM3xShapeFactory)

/** Remembers the adaptive continuous Capsule preset for the current display. */
@Composable
public fun rememberCapsuleShapes(fallbackContainerRadius: Dp = 32.dp): M3xShapes {
    val radii = rememberAdaptiveCornerRadii(fallbackContainerRadius = fallbackContainerRadius)
    return remember(radii) { capsuleShapes(radii) }
}
