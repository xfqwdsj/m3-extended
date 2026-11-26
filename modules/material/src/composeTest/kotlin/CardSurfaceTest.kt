package top.ltfan.m3x.material

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class CardSurfaceTest {
    private object TestSurfaceStyle : MaterialSurfaceStyle

    private class RecordingRenderer : MaterialSurfaceRenderer by PlainMaterialSurfaceRenderer {
        var calls = 0
        var role: MaterialSurfaceRole? = null
        var style: MaterialSurfaceStyle? = null

        @Composable
        override fun appearance(
            role: MaterialSurfaceRole,
            style: MaterialSurfaceStyle?,
            containerColor: Color,
            contentColor: Color,
            effects: MaterialSurfaceEffects?,
            shape: Shape,
            excludedEdges: Set<MaterialSurfaceEdge>,
        ): MaterialSurfaceAppearance {
            calls++
            this.role = role
            this.style = style
            return PlainMaterialSurfaceRenderer.appearance(
                role,
                style,
                containerColor,
                contentColor,
                effects,
                shape,
                excludedEdges,
            )
        }
    }

    @Test
    fun defaultAndDisabledCardsUseTheNativeMaterialSurface() = runComposeUiTest {
        val renderer = RecordingRenderer()
        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalMaterialSurfaceRenderer provides renderer) {
                    Column {
                        Card { Text("Default") }
                        Card(backdrop = false, surfaceStyle = TestSurfaceStyle) {
                            Text("Explicit native")
                        }
                    }
                }
            }
        }
        waitForIdle()
        assertEquals(0, renderer.calls)
    }

    @Test
    fun enabledCardsResolveTheSurfaceRoleAndStyleOverride() = runComposeUiTest {
        val renderer = RecordingRenderer()
        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalMaterialSurfaceRenderer provides renderer) {
                    Card(backdrop = true, surfaceStyle = TestSurfaceStyle) {
                        Text("Ornate")
                    }
                }
            }
        }
        waitForIdle()
        assertEquals(1, renderer.calls)
        assertEquals(MaterialSurfaceRole.Surface, renderer.role)
        assertSame(TestSurfaceStyle, renderer.style)
    }
}
