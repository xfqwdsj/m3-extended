package top.ltfan.m3x.compose.ui.accessibility

import android.app.UiModeManager
import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberSystemAccessibilitySettingsPlatform(): SystemAccessibilitySettings {
    val context = LocalContext.current.applicationContext
    val state = remember(context) { mutableStateOf(readSettings(context)) }
    DisposableEffect(context) {
        var disposed = false
        val update = { if (!disposed) state.value = readSettings(context) }
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) = update()
            }
        val uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        context.contentResolver.registerContentObserver(uri, false, observer)
        val removeContrast =
            if (Build.VERSION.SDK_INT >= 34)
                (context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager)?.let {
                    observeContrast(it, context, update)
                }
            else null
        val removeTextContrast =
            if (Build.VERSION.SDK_INT >= 36)
                (context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager)
                    ?.let {
                        observeTextContrast(it, context, update)
                    }
            else null
        update()
        onDispose {
            disposed = true
            context.contentResolver.unregisterContentObserver(observer)
            removeContrast?.invoke()
            removeTextContrast?.invoke()
        }
    }
    return state.value
}

private fun readSettings(context: Context): SystemAccessibilitySettings {
    val animator = runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
        ) == 0f
    }
        .getOrNull()
    val contrast =
        if (Build.VERSION.SDK_INT >= 34)
            runCatching {
                (context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager)?.let(
                    ::readContrast,
                )
            }
                .getOrNull()
        else null
    val textContrast =
        if (Build.VERSION.SDK_INT >= 36)
            runCatching {
                (context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager)
                    ?.let(::readTextContrast)
            }
                .getOrNull()
        else null
    return SystemAccessibilitySettings(
        increaseContrast =
            when {
                contrast == true || textContrast == true -> true
                contrast == false || textContrast == false -> false
                else -> null
            },
        reduceMotion = animator,
    )
}

@RequiresApi(34) private fun readContrast(manager: UiModeManager): Boolean = manager.contrast > 0f

@RequiresApi(36)
private fun readTextContrast(manager: AccessibilityManager): Boolean =
    manager.isHighContrastTextEnabled

@RequiresApi(34)
private fun observeContrast(
    manager: UiModeManager,
    context: Context,
    update: () -> Unit,
): () -> Unit {
    val listener = UiModeManager.ContrastChangeListener { _ -> update() }
    manager.addContrastChangeListener(context.mainExecutor, listener)
    return { manager.removeContrastChangeListener(listener) }
}

@RequiresApi(36)
private fun observeTextContrast(
    manager: AccessibilityManager,
    context: Context,
    update: () -> Unit,
): () -> Unit {
    val listener = AccessibilityManager.HighContrastTextStateChangeListener { update() }
    manager.addHighContrastTextStateChangeListener(context.mainExecutor, listener)
    return { manager.removeHighContrastTextStateChangeListener(listener) }
}
