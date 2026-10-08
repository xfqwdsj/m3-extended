package top.ltfan.m3x.compose.material3

import androidx.compose.material3.ListItemColors
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest

class ListItemColorsTransparentTest {
    @Test
    fun keepsEveryContainerStateTransparent() = runInitializedComposeUiTest {
        var colors: ListItemColors? = null
        setContent { colors = ListItemColorsTransparent }
        waitForIdle()
        assertEquals(Color.Transparent, colors?.containerColor)
        assertEquals(Color.Transparent, colors?.disabledContainerColor)
        assertEquals(Color.Transparent, colors?.selectedContainerColor)
        assertEquals(Color.Transparent, colors?.draggedContainerColor)
    }
}
