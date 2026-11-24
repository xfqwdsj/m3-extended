package top.ltfan.m3x.backdrop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import kotlin.math.floor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import top.ltfan.m3x.backdrop.highlight.HighlightBoundaryGeometry
import top.ltfan.m3x.backdrop.highlight.HighlightSdfLayout
import top.ltfan.m3x.backdrop.highlight.HighlightTiles
import top.ltfan.m3x.backdrop.highlight.bakeHighlightCpuSdf
import top.ltfan.m3x.capsule.ContinuousCapsule

internal class HighlightSdfBandTest {
    @Test
    fun conservativeSegmentBoxesSkipOnlySaturatedSamples() = runBlocking {
        for ((width, height) in listOf(360 to 96, 945 to 252, 560 to 360, 1600 to 1000)) {
            val size = Size(width.toFloat(), height.toFloat())
            val geometry =
                HighlightBoundaryGeometry(
                    requireNotNull(
                        ContinuousCapsule.createContour(size, LayoutDirection.Ltr, Density(1f)),
                    ),
                )
            val visibleWidth = 8f
            val tiles =
                HighlightTiles().apply {
                    prepare(IntSize(width, height), geometry.contourFreeBounds, visibleWidth)
                }
            val layout = HighlightSdfLayout(List(tiles.count) { tiles[it] }, 2, visibleWidth)
            val complete = bakeHighlightCpuSdf(layout, geometry, cullFarSamples = false)
            val culled = bakeHighlightCpuSdf(layout, geometry, cullFarSamples = true)
            var omitted = 0
            for (tile in layout.tiles) {
                for (y in 0 until tile.size.height) {
                    for (x in 0 until tile.size.width) {
                        val i = (tile.offset.y + y) * layout.size.width + tile.offset.x + x
                        if (culled[i] != 0) assertEquals(complete[i], culled[i])
                        else {
                            omitted++
                            val distance = abs(((complete[i] ushr 16) and 255) - 128)
                            assertEquals(127, distance, "Unsaturated $i")
                        }
                    }
                }
                if (omitted == 0) continue
                for (y in tile.output.top until tile.output.bottom) {
                    for (x in tile.output.left until tile.output.right) {
                        val sx = floor((x + .5f - tile.origin.x) / layout.divisor - .5f).toInt()
                        val sy = floor((y + .5f - tile.origin.y) / layout.divisor - .5f).toInt()
                        val i = (tile.offset.y + sy) * layout.size.width + tile.offset.x + sx
                        val indices =
                            intArrayOf(i, i + 1, i + layout.size.width, i + layout.size.width + 1)
                        if (indices.any { culled[it] == 0 }) {
                            assertTrue(
                                geometry.nearestBoundary(Offset(x + .5f, y + .5f)).distance >
                                    visibleWidth + .5f,
                            )
                        }
                    }
                }
            }

            assertTrue(omitted > 0)
        }
    }
}
