package top.ltfan.m3x.material

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Switch as MaterialSwitch
import androidx.compose.material3.SwitchDefaults as MaterialSwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering

class SwitchUpstreamParityTest {
    @Test
    fun colorsAndThumbContentMatchUpstreamInEveryEnabledAndCheckedState() =
        runInitializedComposeUiTest {
            var checked by mutableStateOf(false)
            var enabled by mutableStateOf(true)
            setContent {
                ProvideM3xMaterialRendering {
                    MaterialExpressiveTheme {
                        val colors =
                            MaterialSwitchDefaults.colors(
                                checkedThumbColor = Color.Red,
                                checkedTrackColor = Color.Green,
                                checkedBorderColor = Color.Blue,
                                checkedIconColor = Color.Yellow,
                                uncheckedThumbColor = Color.Cyan,
                                uncheckedTrackColor = Color.Magenta,
                                uncheckedBorderColor = Color.Yellow,
                                uncheckedIconColor = Color.Blue,
                                disabledCheckedThumbColor = Color.Green,
                                disabledCheckedTrackColor = Color.Red,
                                disabledCheckedBorderColor = Color.Yellow,
                                disabledCheckedIconColor = Color.Blue,
                                disabledUncheckedThumbColor = Color.Magenta,
                                disabledUncheckedTrackColor = Color.Cyan,
                                disabledUncheckedBorderColor = Color.Blue,
                                disabledUncheckedIconColor = Color.Yellow,
                            )
                        Column {
                            MaterialSwitch(
                                checked,
                                {},
                                Modifier.testTag("upstream"),
                                thumbContent = { Text("x") },
                                enabled = enabled,
                                colors = colors,
                            )
                            Switch(
                                checked,
                                {},
                                Modifier.testTag("m3x"),
                                thumbContent = { Text("x") },
                                enabled = enabled,
                                colors = colors,
                            )
                        }
                    }
                }
            }
            for (checkState in listOf(false, true)) for (enableState in listOf(false, true)) {
                runOnIdle {
                    checked = checkState
                    enabled = enableState
                }
                mainClock.advanceTimeBy(2000)
                val upstream = onNodeWithTag("upstream").captureToImage().toPixelMap()
                val actual = onNodeWithTag("m3x").captureToImage().toPixelMap()
                assertEquals(upstream.width, actual.width)
                assertEquals(upstream.height, actual.height)
                var maxDifference = 0f
                for (y in 0 until actual.height) for (x in 0 until actual.width) {
                    val a = actual[x, y]
                    val b = upstream[x, y]
                    maxDifference =
                        maxOf(
                            maxDifference,
                            abs(a.red - b.red),
                            abs(a.green - b.green),
                            abs(a.blue - b.blue),
                            abs(a.alpha - b.alpha),
                        )
                }
                assertTrue(
                    maxDifference < 0.01f,
                    "checked=$checkState enabled=$enableState difference=$maxDifference",
                )
            }
        }
}
