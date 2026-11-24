package top.ltfan.m3x.backdrop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import top.ltfan.m3x.backdrop.interaction.InteractionFieldRenderer
import top.ltfan.m3x.backdrop.interaction.InteractionLightEntry
import top.ltfan.m3x.backdrop.interaction.InteractionLightUniforms

class InteractionCoverageRenderingTest {
    @Test
    fun initiallySeparatedSourceDrawsWhenItMovesIntoTheReceiver() = runRenderTest {
        lateinit var coordinates: LayoutCoordinates
        val renderer = InteractionFieldRenderer()
        val light =
            InteractionLightEntry(
                Any(),
                Any(),
                10000f,
                10000f,
                80f,
                Color.Red,
                BackdropCurve.Linear,
                1f,
            )
        setContent {
            Box(
                Modifier.size(40.dp)
                    .testTag("coverage-receiver")
                    .onPlaced { coordinates = it }
                    .drawWithContent {
                        drawRect(Color.Blue)
                        renderer.draw(this, coordinates, listOf(light), 1f)
                    },
            )
        }
        waitForIdle()
        fun sample() = onNodeWithTag("coverage-receiver").captureToImage().toPixelMap()[20, 20]
        assertEquals(Color.Blue, sample())
        runOnIdle {
            val center = coordinates.localToWindow(Offset(20f, 20f))
            light.x = center.x
            light.y = center.y
        }
        waitForIdle()
        assertTrue(sample().red > 0.5f)
        runOnIdle { light.x += 10000f }
        waitForIdle()
        assertEquals(Color.Blue, sample())
        runOnIdle { renderer.release() }
    }

    @Test
    fun movingSourcesResizeTheRuntimeUniformPacketWithoutRetainingOldLight() = runRenderTest {
        lateinit var coordinates: LayoutCoordinates
        val renderer = InteractionFieldRenderer()
        val lights =
            List(9) {
                InteractionLightEntry(
                    Any(),
                    Any(),
                    10000f,
                    20f,
                    12f,
                    Color.Red,
                    BackdropCurve.Linear,
                    .4f,
                )
            }
        setContent {
            Box(
                Modifier.size(40.dp)
                    .testTag("light-packet")
                    .onPlaced { coordinates = it }
                    .drawWithContent {
                        drawRect(Color.Blue)
                        renderer.draw(this, coordinates, lights, 1f)
                    },
            )
        }
        waitForIdle()
        fun sample() = onNodeWithTag("light-packet").captureToImage().toPixelMap()[20, 20]
        fun move(active: Int) = runOnIdle {
            val center = coordinates.localToWindow(Offset(20f, 20f))
            lights.forEachIndexed { index, entry ->
                entry.x = if (index < active) center.x else center.x + 10000f
                entry.y = center.y
            }
        }
        move(9)
        val all = sample()
        move(2)
        val few = sample()
        assertTrue(few.red < all.red && few.red > 0f)
        move(9)
        assertEquals(all, sample())
        runOnIdle { renderer.release() }
    }

    @Test
    fun affineCoverageTracksMovingLightsAndRetainsZeroIntensitySources() = runRenderTest {
        val angle = mutableFloatStateOf(0f)
        lateinit var coordinates: LayoutCoordinates
        setContent {
            Box(
                Modifier.size(40.dp)
                    .graphicsLayer {
                        translationX = 20f
                        translationY = 30f
                        scaleX = 1.5f
                        scaleY = 0.75f
                        rotationZ = angle.floatValue
                    }
                    .onPlaced { coordinates = it },
            )
        }
        waitForIdle()
        for (rotation in listOf(0f, 37f, 90f, 180f)) {
            runOnIdle { angle.floatValue = rotation }
            waitForIdle()
            runOnIdle {
                val center = coordinates.localToWindow(Offset(20f, 20f))
                val light =
                    InteractionLightEntry(
                        Any(),
                        Any(),
                        center.x,
                        center.y,
                        8f,
                        Color.Red,
                        BackdropCurve.Linear,
                        0f,
                    )
                val uniforms = InteractionLightUniforms(4)
                fun prepare() {
                    assertTrue(uniforms.prepare(listOf(light), coordinates, Size(40f, 40f)))
                }
                prepare()
                assertTrue(
                    uniforms.mayCoverReceiver,
                    "Zero intensity retains a configured nearby light",
                )
                assertEquals(1, uniforms.count)
                assertTrue(uniforms.coverageBounds.width <= 30f)
                assertTrue(uniforms.coverageBounds.height <= 30f)
                assertTrue(uniforms.coverageBounds.contains(Offset(20f, 20f)))
                light.x += 1000f
                prepare()
                assertFalse(
                    uniforms.mayCoverReceiver,
                    "Separated support is outside every affine receiver sample",
                )
                assertEquals(0, uniforms.count)
                assertTrue(uniforms.coverageBounds.isEmpty)
                light.x = center.x
                prepare()
                assertTrue(
                    uniforms.mayCoverReceiver,
                    "Movement restores coverage on the retained packet",
                )
                assertEquals(1, uniforms.count)
                light.radiusPx = -1f
                prepare()
                assertFalse(uniforms.mayCoverReceiver)
                assertEquals(0, uniforms.count)
                assertTrue(uniforms.prepare(emptyList(), coordinates, Size(40f, 40f)))
                assertFalse(uniforms.mayCoverReceiver)
            }
        }
    }

    @Test
    fun perspectiveReceiversRetainTheGeneralFieldPath() = runRenderTest {
        lateinit var coordinates: LayoutCoordinates
        setContent {
            Box(
                Modifier.size(40.dp)
                    .graphicsLayer {
                        rotationY = 35f
                        cameraDistance = 120f
                    }
                    .onPlaced { coordinates = it },
            )
        }
        waitForIdle()
        runOnIdle {
            // Backends can expose a camera rotation as an affine LayoutCoordinates map.
            // Supply a projective map explicitly to exercise the shared pole-safe fallback.
            val projective =
                object : LayoutCoordinates by coordinates {
                    override fun localToWindow(relativeToLocal: Offset): Offset {
                        val denominator = 1f + relativeToLocal.x * 0.005f
                        return coordinates.localToWindow(relativeToLocal / denominator)
                    }
                }
            val center = projective.localToWindow(Offset(20f, 20f))
            val light =
                InteractionLightEntry(
                    Any(),
                    Any(),
                    center.x + 1000f,
                    center.y,
                    8f,
                    Color.Red,
                    BackdropCurve.Linear,
                    1f,
                )
            val uniforms = InteractionLightUniforms(4)
            assertTrue(uniforms.prepare(listOf(light), projective, Size(40f, 40f)))
            assertTrue(uniforms.mayCoverReceiver)
            assertEquals(1, uniforms.count)
            assertEquals(Rect(0f, 0f, 40f, 40f), uniforms.coverageBounds)
        }
    }

    @Test
    fun affineCullingSelectsUniformCapacityFromCoveringSources() = runRenderTest {
        lateinit var coordinates: LayoutCoordinates
        setContent {
            Box(
                Modifier.size(40.dp).onPlaced { coordinates = it },
            )
        }
        waitForIdle()
        runOnIdle {
            val center = coordinates.localToWindow(Offset(20f, 20f))
            val entries =
                List(256) { index ->
                    InteractionLightEntry(
                        Any(),
                        Any(),
                        if (index < 2) center.x + index * 4f else center.x + 10_000f + index,
                        center.y,
                        8f,
                        Color.Red,
                        BackdropCurve.Linear,
                        1f,
                    )
                }
            val uniforms = InteractionLightUniforms(256)
            assertTrue(uniforms.prepare(entries, coordinates, Size(40f, 40f)))
            assertEquals(2, uniforms.count)
            assertEquals(4, uniforms.shaderCapacity)
            for (index in 2..8) entries[index].x = center.x
            assertTrue(uniforms.prepare(entries, coordinates, Size(40f, 40f)))
            assertEquals(9, uniforms.count)
            assertEquals(16, uniforms.shaderCapacity)
            assertTrue(uniforms.mayCoverReceiver)
            assertFalse(uniforms.coverageBounds.isEmpty)
        }
    }
}
