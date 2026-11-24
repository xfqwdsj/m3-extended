package top.ltfan.m3x.backdrop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import top.ltfan.m3x.backdrop.backdrops.rememberCanvasBackdrop
import top.ltfan.m3x.backdrop.highlight.Highlight
import top.ltfan.m3x.backdrop.highlight.highlight
import top.ltfan.m3x.capsule.ContinuousCapsule
import top.ltfan.m3x.capsule.ContinuousRoundedRectangle
import top.ltfan.m3x.compose.shape.Contour
import top.ltfan.m3x.compose.shape.ContourShape
import top.ltfan.m3x.compose.shape.PathSegment
import top.ltfan.m3x.compose.shape.toPath

class ContourShapeTest : PlatformGraphicsTest() {
    @Test
    fun highlightRequestsSegmentsOnlyForTheNonAnalyticCapsule() = runRenderTest {
        val height = mutableFloatStateOf(80f)
        val contourSizes = mutableSetOf<Size>()
        val shape =
            object : ContourShape by ContinuousCapsule {
                override fun createContour(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Contour? {
                    contourSizes += size
                    return ContinuousCapsule.createContour(size, layoutDirection, density)
                }
            }
        setContent {
            Box(
                Modifier.size(120.dp, height.floatValue.dp)
                    .testTag("contour-shape")
                    .drawBackdrop(
                        backdrop = rememberCanvasBackdrop { drawRect(Color.Blue) },
                        shape = { shape },
                        render = {
                            source()
                            highlight(
                                Highlight(
                                    width = 8.dp,
                                    colorGain = 1f,
                                    environmentGain = 0f,
                                ),
                            )
                        },
                    ),
            )
        }
        for (heightDp in listOf(80f, 120f)) {
            runOnIdle { height.floatValue = heightDp }
            waitForIdle()
            val pixels = onNodeWithTag("contour-shape").captureToImage().toPixelMap()
            assertTrue(pixels[60, 1].red > .5f)
            assertTrue(pixels[60, 60].red < .1f)
        }
        val size = Size(120f, 120f)
        if (isRuntimeShaderSupported()) {
            runOnIdle {
                assertTrue(Size(120f, 80f) in contourSizes)
                assertFalse(size in contourSizes)
            }
        }
        val expected =
            ContinuousCapsule.createOutline(size, LayoutDirection.Ltr, Density(1f)).bounds
        val contour =
            requireNotNull(
                ContinuousCapsule.createContour(size, LayoutDirection.Ltr, Density(1f)),
            )
        val actual = contour.toPath().getBounds()
        assertEquals(expected.left, actual.left, 0.01f)
        assertEquals(expected.top, actual.top, 0.01f)
        assertEquals(expected.right, actual.right, 0.01f)
        assertEquals(expected.bottom, actual.bottom, 0.01f)
    }

    @Test
    fun nestedSameDirectionLoopsUseTheFilledOutlineBoundary() = runRenderTest {
        val shape =
            object : ContourShape {
                override fun createContour(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Contour {
                    fun rectangle(inset: Float): List<PathSegment> {
                        val corners =
                            listOf(
                                Offset(inset, inset),
                                Offset(size.width - inset, inset),
                                Offset(size.width - inset, size.height - inset),
                                Offset(inset, size.height - inset),
                            )
                        return corners.mapIndexed { index, corner ->
                            PathSegment.Line(corner, corners[(index + 1) % corners.size])
                        }
                    }

                    return Contour(rectangle(0f), rectangle(size.minDimension * .3f))
                }

                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline = Outline.Generic(createContour(size, layoutDirection, density).toPath())
            }
        setContent {
            Box(
                Modifier.size(120.dp)
                    .testTag("nested-contour")
                    .drawBackdrop(
                        backdrop = rememberCanvasBackdrop { drawRect(Color.Blue) },
                        shape = { shape },
                        render = {
                            source()
                            highlight(
                                Highlight(width = 8.dp, colorGain = 1f, environmentGain = 0f),
                            )
                        },
                    ),
            )
        }
        waitForIdle()
        val pixels = onNodeWithTag("nested-contour").captureToImage().toPixelMap()
        assertTrue(pixels[60, 1].red > .5f)
        assertTrue(pixels[60, 34].red < .1f)
    }

    @Test
    fun structuredAndGenericContoursMatchAcrossRtlCornersAndResize() = runRenderTest {
        val width = mutableFloatStateOf(120f)
        val height = mutableFloatStateOf(80f)
        val structured = mutableStateOf(true)
        val layoutDirection = mutableStateOf(LayoutDirection.Ltr)
        setContent {
            val contour = remember {
                ContinuousRoundedRectangle(
                    topStart = CornerSize(6.dp),
                    topEnd = CornerSize(24.dp),
                    bottomEnd = CornerSize(12.dp),
                    bottomStart = CornerSize(32.dp),
                )
            }
            val generic =
                remember(contour) {
                    object : Shape {
                        override fun createOutline(
                            size: Size,
                            layoutDirection: LayoutDirection,
                            density: Density,
                        ): Outline = contour.createOutline(size, layoutDirection, density)
                    }
                }
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection.value) {
                val source = rememberCanvasBackdrop {
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color.Red, Color.Blue),
                            Offset.Zero,
                            Offset(120f, 120f),
                        ),
                    )
                }
                Box(Modifier.size(160.dp).testTag("contour-equivalence")) {
                    Box(
                        Modifier.offset(20.dp, 20.dp)
                            .size(width.floatValue.dp, height.floatValue.dp)
                            .drawBackdrop(
                                backdrop = source,
                                shape = { if (structured.value) contour else generic },
                                render = {
                                    val material = image {
                                        source()
                                        clipSurface()
                                    }
                                    group {
                                        draw(material)
                                        highlight(
                                            Highlight(
                                                width = 8.dp,
                                                colorGain = .6f,
                                                environmentGain = 0f,
                                            ),
                                        )
                                    }
                                },
                            ),
                    )
                }
            }
        }
        waitForIdle()

        val scenarios =
            listOf(
                Triple(120f, 80f, LayoutDirection.Ltr),
                Triple(120f, 80f, LayoutDirection.Rtl),
                Triple(80f, 120f, LayoutDirection.Rtl),
            )
        for ((targetWidth, targetHeight, targetDirection) in scenarios) {
            runOnIdle {
                width.floatValue = targetWidth
                height.floatValue = targetHeight
                layoutDirection.value = targetDirection
                structured.value = true
            }
            waitForIdle()
            val contourPixels = onNodeWithTag("contour-equivalence").captureToImage().toPixelMap()

            runOnIdle { structured.value = false }
            waitForIdle()
            val genericPixels = onNodeWithTag("contour-equivalence").captureToImage().toPixelMap()

            for (y in 20 until 140) for (x in 20 until 140) {
                val a = contourPixels[x, y]
                val e = genericPixels[x, y]
                // The highlight contour uses a 0.2 px flattening tolerance at curved edges.
                assertTrue(
                    abs(a.red * a.alpha - e.red * e.alpha) <= .18f &&
                        abs(a.green * a.alpha - e.green * e.alpha) <= .18f &&
                        abs(a.blue * a.alpha - e.blue * e.alpha) <= .18f &&
                        abs(a.alpha - e.alpha) <= .02f,
                    "size=${targetWidth}x$targetHeight, direction=$targetDirection, " +
                        "pixel=($x,$y), structured=$a, generic=$e",
                )
            }
        }
    }
}
