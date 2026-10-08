package top.ltfan.m3x.material.backdrop

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.ui.color.contrastRatioAgainst

class MaterialContrastTest {
    @Test
    fun sourcedContrastSurvivesTheFullBackdropRangeAndUnknownSourcesBecomeOpaque() {
        for (base in listOf(Color.White, Color.Black, Color.Gray, Color.Blue, Color.Yellow)) {
            for (foreground in listOf(Color.White, Color.Black, Color.Red)) {
                val (tint, content) =
                    resolveMaterialContrast(base, foreground, 0.2f, knownSource = true)
                for (value in 0..20) {
                    val behind = Color(value / 20f, value / 20f, value / 20f)
                    assertTrue(
                        content.contrastRatioAgainst(tint.compositeOver(behind)) >= 6.999f,
                        "$base $foreground $value tint=$tint",
                    )
                }
                val (fallback, fallbackContent) =
                    resolveMaterialContrast(base, foreground, 0.2f, knownSource = false)
                assertEquals(1f, fallback.alpha)
                assertTrue(fallbackContent.contrastRatioAgainst(fallback) >= 6.999f)
            }
        }
    }
}
