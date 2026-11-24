package top.ltfan.m3x.material.theming

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.backdrop.BackdropPresets
import top.ltfan.m3x.material.backdrop.theme.BackdropAdaptation
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropMaterial
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropTheme

class BackdropPresetTest {
    @Test
    fun defaultMaterialsAndPresetsKeepStableThemeContent() {
        assertEquals(BackdropAdaptation.Fixed, M3xBackdropMaterial().adaptation)
        assertFalse(M3xBackdropMaterial().adaptiveContentColor)
        val roles =
            listOf(
                MaterialSurfaceRole.Control,
                MaterialSurfaceRole.Panel,
                MaterialSurfaceRole.Surface,
                MaterialSurfaceRole.InversePanel,
                MaterialSurfaceRole.ScrollEdge,
            )
        listOf(
                BackdropPresets.GlassRegular,
                BackdropPresets.GlassClear,
                BackdropPresets.MaterialBlur,
                M3xBackdropTheme(),
            )
            .forEach { theme ->
                roles.forEach { role ->
                    assertEquals(BackdropAdaptation.Fixed, theme.material(role).adaptation)
                    assertFalse(theme.material(role).adaptiveContentColor)
                }
            }
        val explicitAdaptation =
            M3xBackdropMaterial()
                .copy(
                    adaptation = BackdropAdaptation.Background,
                    adaptiveContentColor = true,
                )
        assertEquals(BackdropAdaptation.Background, explicitAdaptation.adaptation)
        assertTrue(explicitAdaptation.adaptiveContentColor)
        val themeWithAdaptivePanel = M3xBackdropTheme(panel = explicitAdaptation)
        assertEquals(BackdropAdaptation.Fixed, themeWithAdaptivePanel.inversePanel.adaptation)
        assertFalse(themeWithAdaptivePanel.inversePanel.adaptiveContentColor)
        assertEquals(0.16f, BackdropPresets.GlassClear.control.tintAlpha)
        assertEquals(0.28f, BackdropPresets.GlassClear.panel.tintAlpha)
        assertEquals(0f, BackdropPresets.GlassClear.control.dimmingAlpha)
        assertEquals(0f, BackdropPresets.GlassClear.panel.dimmingAlpha)
        assertFalse(BackdropPresets.GlassClear.control.adaptiveContentColor)
        assertFalse(BackdropPresets.GlassRegular.scrollEdge.interactiveLighting)
        listOf(
                BackdropPresets.GlassRegular,
                BackdropPresets.GlassClear,
                BackdropPresets.MaterialBlur,
            )
            .forEach { preset ->
                assertEquals(0.8f, preset.inversePanel.tintAlpha)
                assertEquals(0.8f, preset.inversePanel.fallbackAlpha)
                assertEquals(BackdropAdaptation.Fixed, preset.inversePanel.adaptation)
                assertFalse(preset.inversePanel.adaptiveContentColor)
                assertEquals(
                    preset.inversePanel,
                    preset.material(MaterialSurfaceRole.InversePanel),
                )
            }
    }
}
