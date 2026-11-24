package top.ltfan.m3x.compose.ui.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalInspectionMode

/** System accessibility preferences that affect visual presentation. A null value is unknown. */
@Immutable
public data class SystemAccessibilitySettings(
    public val reduceTransparency: Boolean? = null,
    public val increaseContrast: Boolean? = null,
    public val reduceMotion: Boolean? = null,
)

/** A composable reader for system accessibility preferences. */
public typealias SystemAccessibilitySettingsHook = @Composable () -> SystemAccessibilitySettings

/** Overrides the platform reader for the current composition when non-null. */
public val LocalSystemAccessibilitySettingsHook:
    ProvidableCompositionLocal<SystemAccessibilitySettingsHook?> =
    staticCompositionLocalOf {
        null
    }

/** Observes the system accessibility preferences for the lifetime of this composition. */
@Composable
public fun rememberSystemAccessibilitySettings(): SystemAccessibilitySettings {
    val hook = LocalSystemAccessibilitySettingsHook.current
    return when {
        hook != null -> hook()
        LocalInspectionMode.current -> SystemAccessibilitySettings()
        else -> rememberSystemAccessibilitySettingsPlatform()
    }
}

@Composable
internal expect fun rememberSystemAccessibilitySettingsPlatform(): SystemAccessibilitySettings
