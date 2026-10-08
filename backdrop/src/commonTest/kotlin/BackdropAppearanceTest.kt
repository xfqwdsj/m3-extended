package top.ltfan.m3x.backdrop

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradient
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import top.ltfan.backdrop.BackdropAxis
import top.ltfan.backdrop.BackdropCurve
import top.ltfan.backdrop.BackdropEdge
import top.ltfan.backdrop.BackdropRamp
import top.ltfan.backdrop.BackdropRampAnchors
import top.ltfan.backdrop.BackdropSide
import top.ltfan.backdrop.effects.progressiveBackdropStrength
import top.ltfan.backdrop.side

class BackdropAppearanceTest {
    @Test
    fun srgbMidGrayIsLinearizedBeforeMeasuringLuminance() {
        val appearance = measureBackdropAppearance(intArrayOf(0xFF808080.toInt()), Color.Black)
        assertTrue(abs(appearance.relativeLuminance - 0.21586f) < 0.0001f)
        assertTrue(abs(appearance.perceptualBrightness - 128f / 255f) < 0.0001f)
    }

    @Test
    fun transparentSamplesAreCompositedOverThePage() {
        assertEquals(
            1f,
            measureBackdropAppearance(intArrayOf(0x00000000), Color.White).relativeLuminance,
            0.0001f,
        )
        assertEquals(
            0f,
            measureBackdropAppearance(intArrayOf(0x00FFFFFF), Color.Black).relativeLuminance,
            0.0001f,
        )
        val halfBlack = measureBackdropAppearance(intArrayOf(0x80000000.toInt()), Color.White)
        assertTrue(halfBlack.relativeLuminance in 0.20f..0.22f)
    }

    @Test
    fun mixedSamplesAverageLinearLight() {
        val mixed =
            measureBackdropAppearance(
                intArrayOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt()),
                Color.Black,
            )
        assertEquals(0.5f, mixed.relativeLuminance, 0.0001f)
        assertTrue(mixed.perceptualBrightness in 0.73f..0.74f)
    }

    @Test
    fun contrastChoiceHasAHysteresisBand() {
        val middle = BackdropAppearance(0.18f, 0.46f)
        assertFalse(middle.prefersDarkContent(false))
        assertTrue(middle.prefersDarkContent(true))
        assertFalse(BackdropAppearance(0.02f, 0.15f).prefersDarkContent(true))
        assertTrue(BackdropAppearance(0.8f, 0.9f).prefersDarkContent(false))
    }
}

class BackdropRampTest {
    @Test
    fun profileRunsFromOneAtTheRampedEdgeToZeroOppositeIt() {
        assertEquals(1f, progressiveBackdropStrength(0f), 0.0001f)
        assertEquals(0f, progressiveBackdropStrength(1f), 0.0001f)
        assertEquals(1f, progressiveBackdropStrength(-1f), 0.0001f)
        assertEquals(0f, progressiveBackdropStrength(2f), 0.0001f)
    }

    @Test
    fun defaultCurveMatchesTheScrollEdgeBezierControls() {
        // Read the curve the ramp defaults to, so tuning that default does not need this test
        // edited.
        val curve = BackdropCurve.Default as BackdropCurve.CubicBezier
        val expected = CubicBezierEasing(curve.x1, curve.y1, curve.x2, curve.y2)
        for (step in 0..20) {
            val position = step / 20f
            assertEquals(
                expected.transform(position),
                BackdropRamp().intensityAt(position),
                0.002f,
                "intensity at $position",
            )
        }
    }

    @Test
    fun aStraightCurveTransformsToItsInput() {
        val linear = BackdropCurve.Linear
        for (step in 0..20) {
            val position = step / 20f
            assertEquals(position, linear.transform(position), 0.0005f)
        }
        assertEquals(0f, linear.transform(-1f), 0.001f)
        assertEquals(1f, linear.transform(2f), 0.001f)
        val ramp = BackdropRamp(curve = linear, stops = 4)
        assertEquals(0.5f, ramp.intensityAt(0.5f), 0.001f)
        val expected = listOf(0f, 1f / 3f, 2f / 3f, 1f)
        ramp.intensities().forEachIndexed { index, intensity ->
            assertEquals(expected[index], intensity, 0.001f, "stop $index")
        }
    }

    @Test
    fun theShippedRampRisesTowardsTheRampedEdge() {
        // Whatever the theme's curve is tuned to, the shipped ramp must reach both ends and never
        // weaken as it approaches the ramped edge.
        // Position counts away from the ramped edge, so the ramp never rises as it goes.
        var previous = 1.0001f
        for (step in 0..20) {
            val position = step / 20f
            val profile = progressiveBackdropStrength(position)
            assertTrue(profile <= previous, "the ramp must not rise at $position: $profile")
            previous = profile
        }
        assertEquals(1f, progressiveBackdropStrength(0f), 0.001f)
        assertEquals(0f, progressiveBackdropStrength(1f), 0.001f)
    }

    @Test
    fun samplesFollowTheEasingAndRiseTowardsTheRampedEdge() {
        val ramp = BackdropRamp(stops = 5)
        val intensities = ramp.intensities()
        assertEquals(5, intensities.size)
        assertEquals(0f, intensities.first(), 0.0001f)
        assertEquals(1f, intensities.last(), 0.0001f)
        assertTrue((0 until intensities.lastIndex).all { intensities[it] <= intensities[it + 1] })
    }

    @Test
    fun defaultSamplingIsFinerThanADisplayCanShow() {
        val ramp = BackdropRamp()
        assertEquals(BackdropRamp.DEFAULT_STOPS, ramp.stops)
        assertEquals(256, ramp.stops)
        // One sample per step of an 8-bit alpha channel: the painted polyline stays within one
        // alpha step of the curve, so a ramp at this default cannot show its own sampling.
        val gap = deviation(ramp.stops)
        assertTrue(gap <= 1f / 255f, "deviation $gap")
    }

    @Test
    fun moreStopsTrackTheEasingMoreClosely() {
        assertTrue(deviation(BackdropRamp.MAX_STOPS) < deviation(2))
        assertTrue(deviation(BackdropRamp.DEFAULT_STOPS) < deviation(BackdropRamp.MIN_STOPS))
    }

    /** The largest gap between the painted polyline of [stops] samples and the curve itself. */
    private fun deviation(stops: Int): Float {
        val ramp = BackdropRamp(stops = stops)
        val intensities = ramp.intensities()
        return (0..1000).maxOf { step ->
            val position = step / 1000f
            val sample = position * (stops - 1)
            val lower = sample.toInt().coerceAtMost(stops - 2)
            val fraction = sample - lower
            val interpolated =
                intensities[lower] + (intensities[lower + 1] - intensities[lower]) * fraction
            abs(interpolated - ramp.intensityAt(position))
        }
    }

    @Test
    fun stopsOutsideTheShadersUniformArrayAreRejected() {
        assertFailsWith<IllegalArgumentException> { BackdropRamp(stops = 1) }
        assertFailsWith<IllegalArgumentException> {
            BackdropRamp(stops = BackdropRamp.MAX_STOPS + 1)
        }
    }

    @Test
    fun aRampRunsFromZeroToFullAlongItsAxis() {
        // The order is the intensity order: the smaller coordinate is the weaker end, on every
        // edge,
        // so no caller flips anything per edge.
        val axis = axis(thickness = 200f, content = 120f)
        for (anchors in
            listOf(
                BackdropRampAnchors.Auto,
                BackdropRampAnchors.offsets(40.dp, 24.dp),
                BackdropRampAnchors.ratio(0.5f, 0.5f),
            )) {
            val ramp = BackdropRamp(anchors = anchors)
            val span = ramp.span(axis)
            assertTrue(span.start <= span.endInclusive, "$anchors ran backwards: $span")
            assertEquals(0f, ramp.intensityAt(0f), 0.001f)
            assertEquals(1f, ramp.intensityAt(1f), 0.001f)
        }
    }

    @Test
    fun aHalfAndHalfRampStraddlesTheContentSideEdge() {
        // The shape a top app bar was designed with: full strength half the content in, nothing
        // half
        // the content out, which is also the room the ramp asks for past the surface.
        val axis = axis(thickness = 100f, content = 100f)
        val ramp = BackdropRamp(anchors = BackdropRampAnchors.ratio(0.5f, 0.5f))
        val span = ramp.span(axis)
        assertEquals(-50f, span.start, 0.001f)
        assertEquals(50f, span.endInclusive, 0.001f)
    }

    @Test
    fun offsetsMeasureInDpWhateverTheSurfaceIs() {
        val tall = BackdropRampAnchors.offsets(plateau = 40.dp, fade = 24.dp)
        val short = BackdropRampAnchors.offsets(plateau = 40.dp, fade = 24.dp)
        assertEquals(
            tall(axis(thickness = 400f, content = 400f)),
            short(axis(thickness = 80f, content = 80f)),
        )
        assertEquals(-24f, tall(axis(thickness = 80f, content = 80f)).start, 0.001f)
        assertEquals(40f, tall(axis(thickness = 80f, content = 80f)).endInclusive, 0.001f)
    }

    @Test
    fun aRangeOutsideTheSurfaceIsRoomTheEffectsCover() {
        val axis = axis(thickness = 64f, content = 64f)
        val ramp = BackdropRamp(anchors = BackdropRampAnchors.ratio(0.5f, 0.5f))
        assertEquals(32f, -ramp.span(axis).start, 0.001f)
    }

    @Test
    @Suppress("EmptyRange") // The constructor must reject a reversed surface range.
    fun anAxisKeepsItsContentInsideItsSurface() {
        assertFailsWith<IllegalArgumentException> {
            BackdropAxis(BackdropSide.Top, 0f..64f, 0f..80f, 1f, 1f)
        }
        assertFailsWith<IllegalArgumentException> {
            BackdropAxis(BackdropSide.Top, 64f..0f, 0f..0f, 1f, 1f)
        }
    }

    @Test
    fun brushPaintsTheRampAsAGradient() {
        // The profile itself is asserted through the rendered mask; what matters here is that the
        // brush stays a gradient, so a ramp never collapses into a solid color.
        val brush = BackdropRamp(stops = 4).brush(Color.White, Offset.Zero, Offset(0f, 100f))
        assertTrue(brush is LinearGradient, "expected a gradient brush, got $brush")
    }

    private fun axis(thickness: Float, content: Float, density: Float = 1f) =
        BackdropAxis(
            edge = BackdropSide.Top,
            occupied = 0f..thickness,
            content = 0f..content,
            density = density,
            fontScale = 1f,
        )

    @Test
    fun aReadingOrderRequestNamesTheSideItResolvesTo() {
        // A vertical request is physical already, so its side does not depend on the direction.
        assertEquals(BackdropSide.Top, BackdropEdge.Top.side(LayoutDirection.Ltr))
        assertEquals(BackdropSide.Top, BackdropEdge.Top.side(LayoutDirection.Rtl))
        assertEquals(BackdropSide.Bottom, BackdropEdge.Bottom.side(LayoutDirection.Rtl))
        assertEquals(BackdropSide.Left, BackdropEdge.Start.side(LayoutDirection.Ltr))
        assertEquals(BackdropSide.Right, BackdropEdge.End.side(LayoutDirection.Ltr))
        // A right-to-left layout puts the leading edge on the right, which a ramp follows.
        assertEquals(BackdropSide.Right, BackdropEdge.Start.side(LayoutDirection.Rtl))
        assertEquals(BackdropSide.Left, BackdropEdge.End.side(LayoutDirection.Rtl))
    }
}
