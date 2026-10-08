package top.ltfan.m3x.material.bundle

import kotlin.test.Test
import kotlin.test.assertSame
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.LocalFloatingBottomBarRenderer
import top.ltfan.m3x.material.LocalMaterialSurfaceRenderer
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.PlainFloatingBottomBarRenderer
import top.ltfan.m3x.material.PlainMaterialSurfaceRenderer
import top.ltfan.m3x.material.ProvidePlainMaterialRendering
import top.ltfan.m3x.material.backdrop.BackdropFloatingBottomBarRenderer
import top.ltfan.m3x.material.backdrop.BackdropMaterialRenderer
import top.ltfan.m3x.material.backdrop.ProvideBackdropRendering
import top.ltfan.m3x.material.backdrop.theme.LocalM3xBackdropTheme
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropTheme
import top.ltfan.m3x.material.backdrop.theme.ProvideBackdropMaterial
import top.ltfan.m3x.material.backdrop.theme.ProvideBackdropTheme
import top.ltfan.m3x.overlay.LocalOverlayHostDecoration
import top.ltfan.m3x.overlay.backdrop.BackdropOverlayDecoration
import top.ltfan.m3x.overlay.backdrop.ProvideBackdropOverlay

class RenderingSelectionTest {
    @Test
    fun scopedPlainRenderingRestoresTheCompleteRenderingAndHostSelection() =
        runInitializedComposeUiTest {
            setContent {
                ProvideM3xMaterialRendering {
                    assertSame(BackdropMaterialRenderer, LocalMaterialSurfaceRenderer.current)
                    assertSame(
                        BackdropFloatingBottomBarRenderer,
                        LocalFloatingBottomBarRenderer.current,
                    )
                    assertSame(BackdropOverlayDecoration, LocalOverlayHostDecoration.current)
                    ProvidePlainMaterialRendering {
                        assertSame(
                            PlainMaterialSurfaceRenderer,
                            LocalMaterialSurfaceRenderer.current,
                        )
                        assertSame(
                            PlainFloatingBottomBarRenderer,
                            LocalFloatingBottomBarRenderer.current,
                        )
                        assertSame(BackdropOverlayDecoration, LocalOverlayHostDecoration.current)
                    }
                    assertSame(BackdropMaterialRenderer, LocalMaterialSurfaceRenderer.current)
                    assertSame(
                        BackdropFloatingBottomBarRenderer,
                        LocalFloatingBottomBarRenderer.current,
                    )
                }
            }
        }

    @Test
    fun independentProvidersKeepThemeAndHostSelectionSeparate() = runInitializedComposeUiTest {
        val theme = M3xBackdropTheme()
        setContent {
            val host = LocalOverlayHostDecoration.current
            ProvideBackdropTheme(theme) {
                assertSame(theme, LocalM3xBackdropTheme.current)
                assertSame(PlainMaterialSurfaceRenderer, LocalMaterialSurfaceRenderer.current)
                ProvideBackdropRendering {
                    assertSame(BackdropMaterialRenderer, LocalMaterialSurfaceRenderer.current)
                    assertSame(host, LocalOverlayHostDecoration.current)
                    ProvideBackdropOverlay {
                        assertSame(BackdropOverlayDecoration, LocalOverlayHostDecoration.current)
                        ProvideBackdropMaterial(MaterialSurfaceRole.Panel, theme.control) {
                            val configured = LocalM3xBackdropTheme.current!!
                            assertSame(theme.control, configured.panel)
                            assertSame(theme.surface, configured.surface)
                            assertSame(theme.inversePanel, configured.inversePanel)
                        }
                    }
                    assertSame(theme, LocalM3xBackdropTheme.current)
                }
            }
        }
    }
}
