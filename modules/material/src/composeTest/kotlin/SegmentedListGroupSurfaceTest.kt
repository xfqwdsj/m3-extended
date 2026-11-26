package top.ltfan.m3x.material

import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import top.ltfan.m3x.compose.foundation.GroupedLazyColumn

class SegmentedListGroupSurfaceTest {
    private class RecordingRenderer : MaterialSurfaceRenderer by PlainMaterialSurfaceRenderer {
        var calls = 0

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
    fun segmentedRowsUseTheNativeMaterialSurface() = runComposeUiTest {
        val renderer = RecordingRenderer()
        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalMaterialSurfaceRenderer provides renderer) {
                    GroupedLazyColumn(Modifier.height(160.dp)) {
                        segmentedListGroup {
                            items(listOf("First", "Second"), key = { it }) { label, modifier, shapes
                                ->
                                SegmentedListItem(modifier = modifier, shapes = shapes) {
                                    Text(label)
                                }
                            }
                        }
                    }
                }
            }
        }
        waitForIdle()
        assertEquals(0, renderer.calls)
    }
}
