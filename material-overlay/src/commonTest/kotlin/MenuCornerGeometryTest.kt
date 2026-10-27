package top.ltfan.m3x.material.overlay

import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class MenuCornerGeometryTest {
    @Test
    fun normalizedThresholdRepresentsOnePixelAcrossDensitiesAndCornerRanges() {
        val collapsed = RoundedCornerShape(28.dp)
        val expanded = RoundedCornerShape(4.dp)
        assertEquals(
            1f / 24f,
            menuCornerProgressThreshold(collapsed, expanded, Size(200f, 200f), Density(1f)),
        )
        assertEquals(
            1f / 48f,
            menuCornerProgressThreshold(collapsed, expanded, Size(200f, 200f), Density(2f)),
        )
        assertEquals(
            1f / 72f,
            menuCornerProgressThreshold(
                AbsoluteRoundedCornerShape(28.dp, 76.dp, 16.dp, 8.dp),
                expanded,
                Size(200f, 200f),
                Density(1f),
            ),
        )
    }
}
