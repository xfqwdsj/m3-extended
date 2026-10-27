package top.ltfan.m3x.backdrop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ProgressiveBlurModeTest {
    @Test
    fun autoFallsBackWhileFullRequiresShader() {
        assertEquals(ProgressiveBlurMode.Full, ProgressiveBlurMode.Auto.resolve(true, true))
        assertEquals(ProgressiveBlurMode.Layered(), ProgressiveBlurMode.Auto.resolve(false, true))
        assertNull(ProgressiveBlurMode.Auto.resolve(false, false))
        assertNull(ProgressiveBlurMode.Full.resolve(false, true))
        assertNull(ProgressiveBlurMode.Full.resolve(false, false))
    }

    @Test
    fun explicitApproximationsKeepTheirAlgorithmAndRequireUniformBlur() {
        val layered = ProgressiveBlurMode.Layered(3)
        assertEquals(layered, layered.resolve(true, true))
        assertEquals(ProgressiveBlurMode.Alpha, ProgressiveBlurMode.Alpha.resolve(true, true))
        assertNull(layered.resolve(false, false))
        assertNull(ProgressiveBlurMode.Alpha.resolve(false, false))
        assertFailsWith<IllegalArgumentException> { ProgressiveBlurMode.Layered(1) }
    }
}
