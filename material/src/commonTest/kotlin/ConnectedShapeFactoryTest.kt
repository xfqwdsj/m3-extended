package top.ltfan.m3x.material

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.SplitButtonDefaults as MaterialSplitButtonDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.theme.LocalM3xThemeExtensions
import top.ltfan.m3x.material.theme.M3xShapeExtensions
import top.ltfan.m3x.material.theme.M3xThemeExtensions
import top.ltfan.m3x.material.theming.capsule.CapsuleM3xShapeFactory

class ConnectedShapeFactoryTest {
    @Test
    fun connectedShapesKeepInnerSizesAndUseContinuousFactory() = runInitializedComposeUiTest {
        setContent {
            ProvideM3xMaterialRendering {
                MaterialExpressiveTheme {
                    CompositionLocalProvider(
                        LocalM3xThemeExtensions provides
                            M3xThemeExtensions(
                                shapes = M3xShapeExtensions(factory = CapsuleM3xShapeFactory),
                            ),
                    ) {
                        for (height in listOf(32.dp, 40.dp, 56.dp, 96.dp, 136.dp)) {
                            val leading = SplitButtonDefaults.leadingButtonShapesFor(height)
                            val trailing = SplitButtonDefaults.trailingButtonShapesFor(height)
                            val upstreamLeading =
                                MaterialSplitButtonDefaults.leadingButtonShapesFor(height)
                            val upstreamTrailing =
                                MaterialSplitButtonDefaults.trailingButtonShapesFor(height)
                            for ((actualShape, upstreamShape, isLeading) in
                                listOf(
                                    Triple(leading.shape, upstreamLeading.shape, true),
                                    Triple(trailing.shape, upstreamTrailing.shape, false),
                                    Triple(
                                        leading.pressedShape,
                                        upstreamLeading.pressedShape,
                                        true,
                                    ),
                                    Triple(
                                        trailing.pressedShape,
                                        upstreamTrailing.pressedShape,
                                        false,
                                    ),
                                )) {
                                assertTrue(actualShape is CornerBasedShape)
                                assertTrue(upstreamShape is CornerBasedShape)
                                assertFalse(actualShape is RoundedCornerShape)
                                val size = Size(180f, 56f)
                                val density = Density(1f)
                                val actualInner =
                                    if (isLeading) actualShape.topEnd else actualShape.topStart
                                val upstreamInner =
                                    if (isLeading) upstreamShape.topEnd else upstreamShape.topStart
                                assertEquals(
                                    upstreamInner.toPx(size, density),
                                    actualInner.toPx(size, density),
                                )
                                actualShape.createOutline(size, LayoutDirection.Ltr, density)
                                actualShape.createOutline(size, LayoutDirection.Rtl, density)
                            }
                            assertSame(CapsuleM3xShapeFactory.full, trailing.checkedShape)
                        }
                        val connected = ButtonGroupDefaults.connectedLeadingButtonShapes()
                        assertFalse(connected.shape is RoundedCornerShape)
                        assertSame(CapsuleM3xShapeFactory.full, connected.checkedShape)
                        val explicit = RoundedCornerShape(2.dp)
                        assertSame(
                            explicit,
                            ButtonGroupDefaults.connectedTrailingButtonShapes(shape = explicit)
                                .shape,
                        )
                        val base = SegmentedButtonDefaults.baseShape
                        assertSame(base, SegmentedButtonDefaults.itemShape(0, 1, base))
                        assertFalse(SegmentedButtonDefaults.itemShape(0, 3) is RoundedCornerShape)
                    }
                }
            }
        }
    }
}
