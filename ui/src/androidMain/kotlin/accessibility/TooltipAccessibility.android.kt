package top.ltfan.m3x.compose.ui.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityManager
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Reads whether touch exploration or switch access is enabled, and keeps the state up to date while
 * the listeners are registered.
 *
 * Switch access is only tracked from API 33, where the accessibility manager reports a change of
 * the enabled services; before that only touch exploration is read.
 */
@Composable
public actual fun rememberTouchExplorationOrSwitchAccessState(): State<Boolean> {
    val context = LocalContext.current
    val manager =
        remember(context) {
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        }
    val state = remember(manager) { mutableStateOf(manager.readTooltipAccessibilityEnabled()) }
    DisposableEffect(manager) {
        if (manager == null) {
            onDispose {}
        } else {
            val listener = AccessibilityListener(state)
            listener.register(manager)
            state.value = manager.readTooltipAccessibilityEnabled()
            onDispose { listener.unregister(manager) }
        }
    }
    return state
}

private fun AccessibilityManager?.readTooltipAccessibilityEnabled(): Boolean =
    this != null && isEnabled && (isTouchExplorationEnabled || readSwitchAccessEnabled())

private fun AccessibilityManager.readSwitchAccessEnabled(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC).any {
            it.settingsActivityName?.contains(SwitchAccessActivityName, ignoreCase = true) == true
        }

/** Keeps [state] in sync with the accessibility services of the manager it is registered with. */
private class AccessibilityListener(private val state: MutableState<Boolean>) :
    AccessibilityManager.AccessibilityStateChangeListener {
    private var manager: AccessibilityManager? = null
    private var touchExplorationEnabled = false
    private var switchAccessEnabled = false

    private val touchExplorationListener =
        AccessibilityManager.TouchExplorationStateChangeListener { enabled ->
            touchExplorationEnabled = enabled
            update()
        }

    // The listener of the enabled services only exists from API 33, where the interface exists too.
    private val servicesStateListener =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AccessibilityManager.AccessibilityServicesStateChangeListener { am ->
                switchAccessEnabled = am.readSwitchAccessEnabled()
                update()
            }
        } else {
            null
        }

    override fun onAccessibilityStateChanged(enabled: Boolean) = update()

    @SuppressLint("NewApi")
    fun register(am: AccessibilityManager) {
        manager = am
        touchExplorationEnabled = am.isTouchExplorationEnabled
        switchAccessEnabled = am.readSwitchAccessEnabled()
        am.addAccessibilityStateChangeListener(this)
        am.addTouchExplorationStateChangeListener(touchExplorationListener)
        servicesStateListener?.let { Api33.addAccessibilityServicesStateChangeListener(am, it) }
        update()
    }

    @SuppressLint("NewApi")
    fun unregister(am: AccessibilityManager) {
        am.removeAccessibilityStateChangeListener(this)
        am.removeTouchExplorationStateChangeListener(touchExplorationListener)
        servicesStateListener?.let { Api33.removeAccessibilityServicesStateChangeListener(am, it) }
        manager = null
    }

    private fun update() {
        state.value = manager.readTooltipAccessibilityEnabled()
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object Api33 {
    @JvmStatic
    fun addAccessibilityServicesStateChangeListener(
        am: AccessibilityManager,
        listener: AccessibilityManager.AccessibilityServicesStateChangeListener,
    ) {
        am.addAccessibilityServicesStateChangeListener(listener)
    }

    @JvmStatic
    fun removeAccessibilityServicesStateChangeListener(
        am: AccessibilityManager,
        listener: AccessibilityManager.AccessibilityServicesStateChangeListener,
    ) {
        am.removeAccessibilityServicesStateChangeListener(listener)
    }
}

private const val SwitchAccessActivityName = "SwitchAccess"
