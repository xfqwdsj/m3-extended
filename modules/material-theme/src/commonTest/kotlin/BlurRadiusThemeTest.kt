package top.ltfan.m3x.material.theme

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BlurRadiusThemeTest {
    @Test
    fun radiusValidationAlsoAppliesToCopies() {
        val theme = M3xThemeExtensions(enterExitBlurRadius = 12.dp)
        assertEquals(0.dp, theme.copy(enterExitBlurRadius = 0.dp).enterExitBlurRadius)
        for (radius in listOf((-1).dp, Float.NaN.dp, Float.POSITIVE_INFINITY.dp)) {
            assertFailsWith<IllegalArgumentException> { theme.copy(enterExitBlurRadius = radius) }
        }
    }
}
