package top.ltfan.m3x.demo

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun ApplyDemoHdrWindowSettings(enabled: Boolean, headroom: Float) {
    val activity = LocalContext.current.findActivity()
    val window = activity?.window
    DisposableEffect(window) {
        val originalColorMode = window?.colorMode
        val originalHeadroom = if (Build.VERSION.SDK_INT >= 35) window?.desiredHdrHeadroom else null
        onDispose {
            if (Build.VERSION.SDK_INT >= 35 && originalHeadroom != null)
                window?.setDesiredHdrHeadroom(originalHeadroom)
            if (Build.VERSION.SDK_INT >= 26 && originalColorMode != null)
                window.colorMode = originalColorMode
        }
    }
    SideEffect {
        val activeWindow = window ?: return@SideEffect
        if (Build.VERSION.SDK_INT >= 26)
            activeWindow.colorMode =
                if (enabled) ActivityInfo.COLOR_MODE_HDR else ActivityInfo.COLOR_MODE_DEFAULT
        if (Build.VERSION.SDK_INT >= 35)
            activeWindow.setDesiredHdrHeadroom(if (enabled) headroom else 0f)
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
