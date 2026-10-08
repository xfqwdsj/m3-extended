package top.ltfan.m3x.material.theming

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.backdrop.BackdropPresets
import top.ltfan.m3x.material.backdrop.theme.BackdropAdaptation
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropMaterial
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropTheme
import top.ltfan.m3x.material.backdrop.theme.ProvideBackdropMaterial
import top.ltfan.m3x.material.backdrop.theme.backdrop
import top.ltfan.m3x.material.bundle.M3xMaterialTheme
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.theme.extended

class BackdropThemeTest {
    @Test
    fun rolesInheritAndFollowRuntimePresetChanges() = runInitializedComposeUiTest {
        var preset by mutableStateOf(BackdropPresets.GlassRegular)
        var root: M3xBackdropTheme? = null
        var nested: M3xBackdropTheme? = null
        var override: M3xBackdropTheme? = null
        var inverseOverride: M3xBackdropTheme? = null
        setContent {
            ProvideM3xMaterialRendering {
                M3xMaterialTheme(backdrop = preset) {
                    ProvideBackdropMaterial(
                        MaterialSurfaceRole.InversePanel,
                        preset.inversePanel.copy(tint = Color.Blue),
                    ) {
                        M3xMaterialTheme { inverseOverride = MaterialTheme.extended.backdrop }
                    }
                    root = MaterialTheme.extended.backdrop
                    M3xMaterialTheme { nested = MaterialTheme.extended.backdrop }
                    ProvideBackdropMaterial(
                        MaterialSurfaceRole.Control,
                        preset.control.copy(tint = Color.Red),
                    ) {
                        override = MaterialTheme.extended.backdrop
                    }
                }
            }
        }
        waitForIdle()
        assertEquals(BackdropPresets.GlassRegular, root)
        assertEquals(root, nested)
        assertEquals(Color.Red, override?.control?.tint)
        assertEquals(root?.panel, override?.panel)
        assertEquals(Color.Blue, inverseOverride?.inversePanel?.tint)
        assertEquals(root?.panel, inverseOverride?.panel)
        assertEquals(root?.inversePanel, override?.inversePanel)
        runOnIdle { preset = BackdropPresets.MaterialBlur }
        waitForIdle()
        assertEquals(BackdropPresets.MaterialBlur, root)
        assertEquals(root, nested)
        assertEquals(root?.scrollEdge, override?.scrollEdge)
        assertEquals(preset.inversePanel.style, inverseOverride?.inversePanel?.style)
        assertEquals(root?.inversePanel, override?.inversePanel)
    }

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
