package top.ltfan.m3x.material.theming

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame
import top.ltfan.m3x.material.theme.M3xShapes
import top.ltfan.m3x.material.theme.toMaterialShapes

class ShapeAdapterTest {
    @Test
    fun adapterMapsAllEightRolesAndCustomShapesDoNotInventMetadata() {
        val source =
            M3xShapes(
                extraSmall = RoundedCornerShape(1.dp),
                small = RoundedCornerShape(2.dp),
                medium = RoundedCornerShape(3.dp),
                large = RoundedCornerShape(4.dp),
                extraLarge = RoundedCornerShape(5.dp),
                largeIncreased = RoundedCornerShape(6.dp),
                extraLargeIncreased = RoundedCornerShape(7.dp),
                extraExtraLarge = RoundedCornerShape(8.dp),
            )
        val mapped = source.toMaterialShapes()
        assertSame(source.extraSmall, mapped.extraSmall)
        assertSame(source.small, mapped.small)
        assertSame(source.medium, mapped.medium)
        assertSame(source.large, mapped.large)
        assertSame(source.extraLarge, mapped.extraLarge)
        assertSame(source.largeIncreased, mapped.largeIncreased)
        assertSame(source.extraLargeIncreased, mapped.extraLargeIncreased)
        assertSame(source.extraExtraLarge, mapped.extraExtraLarge)
        assertNull(source.cornerRadii)
    }
}
