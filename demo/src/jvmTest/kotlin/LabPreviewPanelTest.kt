package top.ltfan.m3x.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue
import org.jetbrains.compose.resources.getString
import top.ltfan.m3x.datastore.compose.storeHost
import top.ltfan.m3x.demo.generated.resources.Res
import top.ltfan.m3x.demo.generated.resources.demo_lab_scroll_hint
import top.ltfan.m3x.demo.generated.resources.demo_lab_surface_hint
import top.ltfan.m3x.demo.generated.resources.demo_material_surfaces
import top.ltfan.m3x.demo.generated.resources.demo_scroll_edges
import top.ltfan.m3x.material.LocalMaterialSurfaceRenderer
import top.ltfan.m3x.material.MaterialSurfaceAppearance
import top.ltfan.m3x.material.MaterialSurfaceEdge
import top.ltfan.m3x.material.MaterialSurfaceEffects
import top.ltfan.m3x.material.MaterialSurfaceRenderer
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.MaterialSurfaceStyle
import top.ltfan.m3x.material.bundle.M3xMaterialTheme
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayHostState

class LabPreviewPanelTest {
    @Test fun narrowPreviewKeepsModesAndGuidanceVisible() = panel(400)

    @Test fun widePreviewKeepsModesAndGuidanceVisible() = panel(1100)

    private fun panel(width: Int) = runComposeUiTest {
        val surfaces = getString(Res.string.demo_material_surfaces)
        val edges = getString(Res.string.demo_scroll_edges)
        val surfaceHint = getString(Res.string.demo_lab_surface_hint)
        val scrollHint = getString(Res.string.demo_lab_scroll_hint)
        var inheritedRendererCalls = 0
        setContent {
            val scope = rememberCoroutineScope()
            val host = remember(scope) { storeHost(scope, DemoStoreFile({ null }, { _, _ -> })) }
            DisposableEffect(host) { onDispose { host.close() } }
            val store = remember(host) { host.mutable(LabParametersStore) }
            M3xMaterialTheme {
                val inherited = LocalMaterialSurfaceRenderer.current
                val renderer =
                    remember(inherited) {
                        object : MaterialSurfaceRenderer by inherited {
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
                                inheritedRendererCalls++
                                return inherited.appearance(
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
                    }
                CompositionLocalProvider(LocalMaterialSurfaceRenderer provides renderer) {
                    OverlayHost(OverlayHostState()) {
                        Box(Modifier.size(width.dp, 760.dp).testTag("scene")) {
                            BackdropLabPage(store, hdrOutput = false)
                        }
                    }
                }
            }
        }
        assertTrue(
            inheritedRendererCalls > 0,
            "The parameter panel must keep the application surface renderer",
        )
        onNodeWithText(surfaces).assertIsDisplayed().assertIsSelected()
        onNodeWithText(surfaceHint).assertIsDisplayed()
        fun snapshot(mode: String) {
            val pixels = onNodeWithTag("scene").captureToImage().toPixelMap()
            val image = BufferedImage(pixels.width, pixels.height, BufferedImage.TYPE_INT_ARGB)
            for (y in 0 until pixels.height) for (x in 0 until pixels.width) image.setRGB(
                x,
                y,
                pixels[x, y].toArgb(),
            )
            ImageIO.write(
                image,
                "png",
                File(System.getProperty("java.io.tmpdir"), "m3x-lab-$width-$mode.png"),
            )
        }
        snapshot("surfaces")
        onNode(hasText(edges) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .performClick()
            .assertIsSelected()
        onNodeWithText(scrollHint).assertIsDisplayed()
        snapshot("edges")
        onNodeWithTag("lab-scroll-options").performSemanticsAction(SemanticsActions.ScrollBy) {
            assertTrue(it(120f, 0f))
        }
        mainClock.advanceTimeBy(500)
        snapshot("edges-scrolled")
        onNodeWithTag("lab-scroll-content").performSemanticsAction(SemanticsActions.ScrollBy) {
            assertTrue(it(0f, 120f))
        }
        mainClock.advanceTimeBy(500)
        snapshot("edges-both-scrolled")
        onNodeWithText(surfaces).performClick().assertIsSelected()
        onNodeWithText(surfaceHint).assertIsDisplayed()
    }
}
