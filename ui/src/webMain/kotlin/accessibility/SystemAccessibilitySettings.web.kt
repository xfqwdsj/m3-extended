package top.ltfan.m3x.compose.ui.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.browser.window
import org.w3c.dom.MediaQueryList
import org.w3c.dom.events.Event

@Composable
internal actual fun rememberSystemAccessibilitySettingsPlatform(): SystemAccessibilitySettings {
    val queries = remember { mediaQueries() }
    val state = remember(queries) { mutableStateOf(readSettings(queries)) }
    DisposableEffect(queries) {
        var disposed = false
        val listener: (Event) -> Unit = { if (!disposed) state.value = readSettings(queries) }
        queries.filterNotNull().forEach { it.addEventListener("change", listener) }
        state.value = readSettings(queries)
        onDispose {
            disposed = true
            queries.filterNotNull().forEach { it.removeEventListener("change", listener) }
        }
    }
    return state.value
}

private fun mediaQueries(): List<MediaQueryList?> =
    listOf(
            "(prefers-reduced-transparency: reduce)",
            "(prefers-contrast: more)",
            "(prefers-reduced-motion: reduce)",
        )
        .map { runCatching { window.matchMedia(it) }.getOrNull() }

private fun readSettings(queries: List<MediaQueryList?>): SystemAccessibilitySettings =
    SystemAccessibilitySettings(
        reduceTransparency = queries[0]?.matches,
        increaseContrast = queries[1]?.matches,
        reduceMotion = queries[2]?.matches,
    )
