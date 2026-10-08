package top.ltfan.m3x.material

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.compose.ui.accessibility.AccessibilitySettings
import top.ltfan.m3x.compose.ui.accessibility.LocalAccessibilitySettings
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.overlay.OverlayBottomSheet
import top.ltfan.m3x.material.overlay.OverlayDialog
import top.ltfan.m3x.material.overlay.OverlayDialogProperties
import top.ltfan.m3x.material.overlay.OverlayMenu
import top.ltfan.m3x.material.overlay.OverlaySnackbar
import top.ltfan.m3x.material.overlay.OverlaySnackbarHost
import top.ltfan.m3x.material.overlay.rememberOverlayBottomSheetState
import top.ltfan.m3x.material.theme.LocalM3xThemeExtensions
import top.ltfan.m3x.material.theme.M3xThemeExtensions
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayHostState

class ContainerBlurRenderingTest {
    @Test fun unsharedDialogBlursItsContainerBackground() = verify(Kind.Dialog)

    @Test fun unsharedMenuBlursItsContainerBackground() = verify(Kind.Menu)

    @Test fun snackbarBlursItsContainerBackground() = verify(Kind.Snackbar)

    @Test fun bottomSheetKeepsSlidingContentSharp() = verify(Kind.Sheet)

    @Test fun reducedMotionSheetKeepsContentSharp() = verify(Kind.Sheet, reduceMotion = true)

    private enum class Kind {
        Dialog,
        Menu,
        Snackbar,
        Sheet,
    }

    private fun verify(kind: Kind, reduceMotion: Boolean = false) = runInitializedComposeUiTest {
        mainClock.autoAdvance = false
        var shown by mutableStateOf(false)
        var radius by mutableStateOf(0.dp)
        lateinit var snackbarState: SnackbarHostState
        lateinit var scope: CoroutineScope
        val motion =
            object : MotionScheme by MotionScheme.standard() {
                override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)

                override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)

                override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)

                override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)

                override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)

                override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)
            }
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(motionScheme = motion) {
                    CompositionLocalProvider(
                        LocalAccessibilitySettings provides
                            AccessibilitySettings(reduceMotion = reduceMotion),
                        LocalM3xThemeExtensions provides
                            M3xThemeExtensions(enterExitBlurRadius = radius),
                    ) {
                        Box(
                            Modifier.size(480.dp, 640.dp).background(Color.White).testTag("scene"),
                        ) {
                            OverlayHost(remember { OverlayHostState() }) {
                                when (kind) {
                                    Kind.Dialog ->
                                        OverlayDialog(
                                            visible = { shown },
                                            onDismissRequest = { shown = false },
                                            sharedClipShape = RectangleShape,
                                            containerColor = Color.Red,
                                            properties =
                                                OverlayDialogProperties(
                                                    scrimColor = Color.Transparent,
                                                ),
                                        ) {
                                            Spacer(Modifier.size(240.dp, 120.dp).testTag("panel"))
                                        }
                                    Kind.Menu ->
                                        Box(Modifier.padding(100.dp).size(40.dp)) {
                                            OverlayMenu(
                                                expanded = { shown },
                                                onDismissRequest = { shown = false },
                                                shape = RectangleShape,
                                                containerColor = Color.Red,
                                                shadowElevation = 0.dp,
                                            ) {
                                                Spacer(
                                                    Modifier.size(240.dp, 120.dp).testTag("panel"),
                                                )
                                            }
                                        }
                                    Kind.Sheet ->
                                        OverlayBottomSheet(
                                            visible = { shown },
                                            onDismissRequest = { shown = false },
                                            sheetState =
                                                rememberOverlayBottomSheetState(
                                                    skipPartiallyExpanded = true,
                                                ),
                                            sheetMaxWidth = 240.dp,
                                            shape = RectangleShape,
                                            containerColor = Color.Red,
                                            scrimColor = Color.Transparent,
                                            dragHandle = null,
                                            contentWindowInsets = { WindowInsets(0) },
                                        ) {
                                            Row(Modifier.size(240.dp, 120.dp).testTag("panel")) {
                                                Spacer(Modifier.size(120.dp).background(Color.Red))
                                                Spacer(
                                                    Modifier.size(120.dp).background(Color.White),
                                                )
                                            }
                                        }
                                    Kind.Snackbar -> {
                                        snackbarState = remember { SnackbarHostState() }
                                        scope = rememberCoroutineScope()
                                        OverlaySnackbarHost(
                                            snackbarState,
                                            snackbar = { data ->
                                                OverlaySnackbar(
                                                    data,
                                                    Modifier.width(240.dp).testTag("panel"),
                                                    shape = RectangleShape,
                                                    containerColor = Color.Red,
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        fun softEdgePixels(): Int {
            val bounds =
                onNodeWithTag("panel", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val pixels = onNodeWithTag("scene").captureToImage().toPixelMap()
            val row = bounds.center.y.toInt().coerceIn(0, pixels.height - 1)
            val reds =
                (0 until pixels.width).map { x ->
                    val c = pixels[x, row]
                    c.red - (c.green + c.blue) / 2f
                }
            val peak = reds.max()
            assertTrue(peak > 0.1f, "The background must be visible during $kind exit: $peak")
            return reds.count { it > peak * 0.05f && it < peak * 0.95f }
        }
        val softness = mutableListOf<Int>()
        for (value in listOf(0.dp, 16.dp)) {
            runOnIdle {
                radius = value
                if (kind == Kind.Snackbar)
                    scope.launch {
                        snackbarState.showSnackbar("", duration = SnackbarDuration.Indefinite)
                    }
                else shown = true
            }
            if (kind == Kind.Sheet) {
                mainClock.advanceTimeByFrame()
                mainClock.advanceTimeBy(500)
            } else {
                mainClock.advanceTimeBy(1300)
                runOnIdle {
                    if (kind == Kind.Snackbar) snackbarState.currentSnackbarData!!.dismiss()
                    else shown = false
                }
                mainClock.advanceTimeByFrame()
                mainClock.advanceTimeBy(500)
            }
            softness += softEdgePixels()
            mainClock.advanceTimeBy(1300)
            if (kind == Kind.Sheet) {
                runOnIdle { shown = false }
                mainClock.advanceTimeBy(1300)
            }
        }
        if (kind == Kind.Sheet) {
            assertTrue(
                abs(softness[1] - softness[0]) <= 2,
                "Sheet content must stay sharp regardless of the entrance blur radius: $softness",
            )
            return@runInitializedComposeUiTest
        }
        assertTrue(
            softness[1] >= softness[0] + 4,
            "$kind must blur the background edge using the theme radius: zero=${softness[0]} blurred=${softness[1]}",
        )
    }
}
