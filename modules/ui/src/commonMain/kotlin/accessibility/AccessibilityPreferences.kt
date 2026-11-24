package top.ltfan.m3x.compose.ui.accessibility

import androidx.compose.runtime.Immutable

/** Selects a system preference or an explicit accessibility override. */
public enum class AccessibilityPreference {
    System,
    Enabled,
    Disabled,
}

/** Accessibility preferences that can be provided independently of Material theming. */
@Immutable
public data class AccessibilityPreferences(
    public val reduceTransparency: AccessibilityPreference = AccessibilityPreference.System,
    public val increaseContrast: AccessibilityPreference = AccessibilityPreference.System,
    public val reduceMotion: AccessibilityPreference = AccessibilityPreference.System,
)

/** Resolved accessibility values for the current composition. */
@Immutable
public data class AccessibilitySettings(
    public val reduceTransparency: Boolean = false,
    public val increaseContrast: Boolean = false,
    public val reduceMotion: Boolean = false,
)

/** Resolves explicit overrides and known system values for these preferences. */
public fun AccessibilityPreferences.resolveAccessibility(
    system: SystemAccessibilitySettings,
): AccessibilitySettings =
    AccessibilitySettings(
        reduceTransparency = resolve(reduceTransparency, system.reduceTransparency),
        increaseContrast = resolve(increaseContrast, system.increaseContrast),
        reduceMotion = resolve(reduceMotion, system.reduceMotion),
    )

private fun resolve(preference: AccessibilityPreference, system: Boolean?): Boolean =
    when (preference) {
        AccessibilityPreference.System -> system == true
        AccessibilityPreference.Enabled -> true
        AccessibilityPreference.Disabled -> false
    }
