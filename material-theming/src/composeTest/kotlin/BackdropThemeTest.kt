package top.ltfan.m3x.material.theming

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.backdrop.BackdropPresets
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropTheme
import top.ltfan.m3x.material.backdrop.theme.ProvideBackdropMaterial
import top.ltfan.m3x.material.backdrop.theme.backdrop
import top.ltfan.m3x.material.bundle.M3xMaterialTheme
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.theme.extended

class BackdropThemeTest {
    @Test
    fun rolesInheritAndFollowRuntimePresetChanges() = runComposeUiTest {
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
}
