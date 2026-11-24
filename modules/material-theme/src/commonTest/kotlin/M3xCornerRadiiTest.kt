package top.ltfan.m3x.material.theme

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import top.ltfan.m3x.compose.ui.layout.CornerRadii
import top.ltfan.m3x.compose.ui.layout.WindowPresentation

class M3xCornerRadiiTest {
    @Test
    fun scalesFromLargestKnownPhysicalCornerAndKeepsAsymmetry() {
        val display = CornerRadii(12.dp, 24.dp, 48.dp, 32.dp)
        val result =
            adaptiveCornerRadii(display, null, WindowPresentation.Fullscreen, isMobile = true)
        assertEquals(48.dp, result.containerRadius)
        assertEquals(CornerRadii(12.dp, 24.dp, 48.dp, 32.dp), result.resolvedContainerCorners)
        assertEquals(CornerRadiiSource.Display, result.source)
        assertEquals(72.dp, result.extraExtraLarge)
    }

    @Test
    fun clampsShapeScaleWhilePreservingMeasuredContainerRadius() {
        val large =
            adaptiveCornerRadii(
                CornerRadii(200.dp, 180.dp, 190.dp, 170.dp),
                null,
                WindowPresentation.Fullscreen,
                isMobile = true,
            )
        assertEquals(200.dp, large.containerRadius)
        assertEquals(1.5f, large.extraSmall.value / 4f)
        assertEquals(72.dp, large.extraExtraLarge)

        val small =
            adaptiveCornerRadii(
                CornerRadii(1.dp, 1.dp, 1.dp, 1.dp),
                null,
                WindowPresentation.Fullscreen,
                isMobile = true,
            )
        assertEquals(1.dp, small.containerRadius)
        assertEquals(0.75f, small.extraSmall.value / 4f)
    }

    @Test
    fun copyRecomputesSelectedSourceAndContainerCorners() {
        val original =
            M3xCornerRadii(
                displayCorners = CornerRadii(10.dp, 10.dp, 10.dp, 10.dp),
                windowCorners = CornerRadii(20.dp, 20.dp, 20.dp, 20.dp),
                policy = CornerRadiiPolicy.Display,
                windowPresentation = WindowPresentation.Fullscreen,
                containerRadius = 10.dp,
                mobilePlatform = true,
            )
        val copied = original.copy(policy = CornerRadiiPolicy.Window)
        assertEquals(CornerRadiiSource.Display, original.source)
        assertEquals(CornerRadiiSource.Window, copied.source)
        assertEquals(CornerRadii(10.dp, 10.dp, 10.dp, 10.dp), original.resolvedContainerCorners)
        assertEquals(CornerRadii(20.dp, 20.dp, 20.dp, 20.dp), copied.resolvedContainerCorners)
    }

    @Test
    fun preservesTrueZeroAndFillsOnlyUnknownCorners() {
        val allZero =
            adaptiveCornerRadii(
                CornerRadii(0.dp, 0.dp, 0.dp, 0.dp),
                null,
                WindowPresentation.Fullscreen,
                isMobile = true,
            )
        assertEquals(0.dp, allZero.containerRadius)
        assertEquals(CornerRadii(0.dp, 0.dp, 0.dp, 0.dp), allZero.resolvedContainerCorners)
        assertEquals(3.dp, allZero.extraSmall)

        val partial =
            adaptiveCornerRadii(
                CornerRadii(0.dp, 10.dp, null, Float.NaN.dp),
                null,
                WindowPresentation.Fullscreen,
                isMobile = true,
            )
        assertEquals(CornerRadii(0.dp, 10.dp, 10.dp, 10.dp), partial.resolvedContainerCorners)
        assertEquals(CornerRadii(0.dp, 10.dp, null, Float.NaN.dp), partial.displayCorners)
    }

    @Test
    fun invalidOrUnknownMeasurementsUseFallbackAndRecordFallbackSource() {
        val fallback = adaptiveCornerRadii(null, null, WindowPresentation.Unknown)
        assertEquals(32.dp, fallback.containerRadius)
        assertEquals(CornerRadiiSource.Fallback, fallback.source)
        assertEquals(CornerRadii(32.dp, 32.dp, 32.dp, 32.dp), fallback.resolvedContainerCorners)

        val invalid =
            adaptiveCornerRadii(
                CornerRadii((-1).dp, Float.POSITIVE_INFINITY.dp),
                null,
                WindowPresentation.Fullscreen,
                isMobile = true,
            )
        assertEquals(32.dp, invalid.containerRadius)
        assertEquals(CornerRadiiSource.Fallback, invalid.source)
    }

    @Test
    fun policyChoosesDisplayOrWindowAndUnknownAutoDoesNotBorrowDisplay() {
        val display = CornerRadii(40.dp, 40.dp, 40.dp, 40.dp)
        val window = CornerRadii(12.dp, 12.dp, 12.dp, 12.dp)
        assertEquals(
            40.dp,
            adaptiveCornerRadii(display, window, WindowPresentation.Fullscreen, isMobile = true)
                .containerRadius,
        )
        assertEquals(
            12.dp,
            adaptiveCornerRadii(display, window, WindowPresentation.Windowed, isMobile = true)
                .containerRadius,
        )
        assertEquals(
            12.dp,
            adaptiveCornerRadii(display, window, WindowPresentation.Fullscreen, isMobile = false)
                .containerRadius,
        )
        val unknown =
            adaptiveCornerRadii(display, null, WindowPresentation.Unknown, isMobile = true)
        assertEquals(32.dp, unknown.containerRadius)
        assertEquals(CornerRadiiSource.Fallback, unknown.source)
        assertEquals(
            40.dp,
            adaptiveCornerRadii(
                    display,
                    window,
                    WindowPresentation.Unknown,
                    CornerRadiiPolicy.Display,
                )
                .containerRadius,
        )
        assertEquals(
            12.dp,
            adaptiveCornerRadii(
                    display,
                    window,
                    WindowPresentation.Fullscreen,
                    CornerRadiiPolicy.Window,
                )
                .containerRadius,
        )
    }

    @Test
    fun fallbackRadiusMustBeFiniteAndPositive() {
        for (radius in listOf(0.dp, (-1).dp, Float.NaN.dp, Float.POSITIVE_INFINITY.dp)) {
            assertFailsWith<IllegalArgumentException> {
                adaptiveCornerRadii(
                    null,
                    null,
                    WindowPresentation.Unknown,
                    fallbackContainerRadius = radius,
                )
            }
        }
    }
}
