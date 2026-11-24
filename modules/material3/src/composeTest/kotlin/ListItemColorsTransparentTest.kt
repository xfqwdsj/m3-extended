package top.ltfan.m3x.compose.material3

import androidx.compose.material3.ListItemColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ListItemColorsTransparentTest {
    @Test
    fun keepsEveryContainerStateTransparent() = runComposeUiTest {
        var colors: ListItemColors? = null
        setContent { colors = ListItemColorsTransparent }
        waitForIdle()
        assertEquals(Color.Transparent, colors?.containerColor)
        assertEquals(Color.Transparent, colors?.disabledContainerColor)
        assertEquals(Color.Transparent, colors?.selectedContainerColor)
        assertEquals(Color.Transparent, colors?.draggedContainerColor)
    }
}
