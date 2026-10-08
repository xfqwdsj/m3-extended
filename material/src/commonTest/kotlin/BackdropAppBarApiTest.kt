package top.ltfan.m3x.material

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.backdrop.BackdropCenterAlignedTopAppBar
import top.ltfan.m3x.material.backdrop.BackdropLargeFlexibleTopAppBar
import top.ltfan.m3x.material.backdrop.BackdropMediumFlexibleTopAppBar
import top.ltfan.m3x.material.backdrop.BackdropTopAppBar
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering

class BackdropAppBarApiTest {
    @Test
    fun subtitleSelectsTheNativeHeightAndUnspecifiedHeightsUseDefaults() =
        runInitializedComposeUiTest {
            var variant by mutableIntStateOf(0)
            setContent {
                ProvideM3xMaterialRendering {
                    MaterialTheme {
                        when (variant) {
                            0 ->
                                BackdropTopAppBar(
                                    title = { Text("Title") },
                                    subtitle = { Text("Subtitle") },
                                    modifier = Modifier.testTag("bar"),
                                    expandedHeight = Dp.Unspecified,
                                    windowInsets = WindowInsets(0),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                )
                            1 ->
                                BackdropCenterAlignedTopAppBar(
                                    title = { Text("Title") },
                                    subtitle = { Text("Subtitle") },
                                    modifier = Modifier.testTag("bar"),
                                    expandedHeight = Dp.Infinity,
                                    windowInsets = WindowInsets(0),
                                )
                            2 ->
                                BackdropMediumFlexibleTopAppBar(
                                    title = { Text("Title") },
                                    subtitle = { Text("Subtitle") },
                                    modifier = Modifier.testTag("bar"),
                                    collapsedHeight = Dp.Unspecified,
                                    expandedHeight = Dp.Infinity,
                                    windowInsets = WindowInsets(0),
                                )
                            else ->
                                BackdropLargeFlexibleTopAppBar(
                                    title = { Text("Title") },
                                    subtitle = { Text("Subtitle") },
                                    modifier = Modifier.testTag("bar"),
                                    collapsedHeight = Dp.Infinity,
                                    expandedHeight = Dp.Unspecified,
                                    windowInsets = WindowInsets(0),
                                )
                        }
                    }
                }
            }
            val heights =
                listOf(
                    TopAppBarDefaults.TopAppBarExpandedHeight,
                    TopAppBarDefaults.TopAppBarExpandedHeight,
                    TopAppBarDefaults.MediumFlexibleAppBarWithSubtitleExpandedHeight,
                    TopAppBarDefaults.LargeFlexibleAppBarWithSubtitleExpandedHeight,
                )
            for (index in heights.indices) {
                runOnIdle { variant = index }
                onNodeWithTag("bar").assertHeightIsEqualTo(heights[index])
                assertTrue(onAllNodesWithText("Subtitle").fetchSemanticsNodes().isNotEmpty())
            }
        }
}
