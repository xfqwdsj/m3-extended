package top.ltfan.m3x.material

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest

class MaterialModifierChainTest {
    private object Prefix : Modifier.Element

    private object Suffix : Modifier.Element

    @Test
    fun rendererFactoriesPreserveTheCallerPrefixAndAppendInOrder() = runInitializedComposeUiTest {
        var chains = emptyList<List<Modifier.Element>>()
        setContent {
            val appearance =
                PlainMaterialSurfaceRenderer.appearance(
                    MaterialSurfaceRole.Surface,
                    null,
                    Color.Red,
                    Color.Black,
                    null,
                    RectangleShape,
                    emptySet(),
                )
            val surface = Prefix.materialSurface(appearance, RectangleShape).then(Suffix)
            val bar =
                with(PlainFloatingBottomBarRenderer.appearance(RectangleShape)) {
                    val interaction = Prefix.interactionModifier()
                    assertSame(Prefix, interaction)
                    assertSame(Prefix, Prefix.barModifier { 0f })
                    interaction.then(Suffix)
                }
            SideEffect {
                chains =
                    listOf(surface, bar).map { chain ->
                        chain.foldIn(emptyList()) { elements, element -> elements + element }
                    }
            }
        }
        runOnIdle {
            chains.forEach { elements ->
                assertSame(Prefix, elements.first())
                assertSame(Suffix, elements.last())
                assertEquals(1, elements.count { it === Prefix })
            }
            assertEquals(2, chains.size)
        }
    }
}
