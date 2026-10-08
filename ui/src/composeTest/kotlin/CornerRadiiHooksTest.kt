package top.ltfan.m3x.compose.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.compose.ui.layout.CornerRadii
import top.ltfan.m3x.compose.ui.layout.LocalDisplayCornerRadiiHook
import top.ltfan.m3x.compose.ui.layout.LocalWindowCornerRadiiHook
import top.ltfan.m3x.compose.ui.layout.rememberDisplayCornerRadii
import top.ltfan.m3x.compose.ui.layout.rememberWindowCornerRadii

class CornerRadiiHooksTest {
    @Test
    fun hooksAreScopedAndUnknownDoesNotFallThroughToAnOuterHook() = runInitializedComposeUiTest {
        val outer = CornerRadii(12.dp, 12.dp, 12.dp, 12.dp)
        val inner = CornerRadii(24.dp, 24.dp, 24.dp, 24.dp)
        var rootWindow: CornerRadii? = null
        var nestedWindow: CornerRadii? = null
        var unknownWindow: CornerRadii? = null
        var innerDisplay: CornerRadii? = null

        setContent {
            CompositionLocalProvider(
                LocalDisplayCornerRadiiHook provides { outer },
                LocalWindowCornerRadiiHook provides { outer },
            ) {
                rootWindow = rememberWindowCornerRadii()
                CompositionLocalProvider(LocalWindowCornerRadiiHook provides { inner }) {
                    nestedWindow = rememberWindowCornerRadii()
                    CompositionLocalProvider(LocalWindowCornerRadiiHook provides { null }) {
                        unknownWindow = rememberWindowCornerRadii()
                    }
                }
                CompositionLocalProvider(LocalDisplayCornerRadiiHook provides { inner }) {
                    innerDisplay = rememberDisplayCornerRadii()
                }
            }
        }
        waitForIdle()

        assertEquals(outer, rootWindow)
        assertEquals(inner, nestedWindow)
        assertNull(unknownWindow)
        assertEquals(inner, innerDisplay)
    }

    @Test
    fun hookScopeChangeReportsInnerValueUnknownAndThenRestoresParent() =
        runInitializedComposeUiTest {
            val parent = CornerRadii(18.dp, 18.dp, 18.dp, 18.dp)
            val nested = CornerRadii(30.dp, 30.dp, 30.dp, 30.dp)
            var nestedMode by mutableStateOf(0)
            var observed: CornerRadii? = null

            setContent {
                CompositionLocalProvider(LocalWindowCornerRadiiHook provides { parent }) {
                    when (nestedMode) {
                        1 -> {
                            CompositionLocalProvider(
                                LocalWindowCornerRadiiHook provides { nested },
                            ) {
                                observed = rememberWindowCornerRadii()
                            }
                        }
                        2 -> {
                            CompositionLocalProvider(LocalWindowCornerRadiiHook provides { null }) {
                                observed = rememberWindowCornerRadii()
                            }
                        }
                        else -> observed = rememberWindowCornerRadii()
                    }
                }
            }
            waitForIdle()
            assertEquals(parent, observed)

            runOnIdle { nestedMode = 1 }
            waitForIdle()
            assertEquals(nested, observed)

            runOnIdle { nestedMode = 2 }
            waitForIdle()
            assertNull(observed)

            runOnIdle { nestedMode = 0 }
            waitForIdle()
            assertEquals(parent, observed)
        }
}
