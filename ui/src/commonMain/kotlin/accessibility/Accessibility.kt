package top.ltfan.m3x.compose.ui.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

/** Accessibility preferences configured for the current composition. */
public val LocalAccessibilityPreferences: ProvidableCompositionLocal<AccessibilityPreferences> =
    compositionLocalOf {
        AccessibilityPreferences()
    }

/** Resolved accessibility settings for the current composition. */
public val LocalAccessibilitySettings: ProvidableCompositionLocal<AccessibilitySettings> =
    compositionLocalOf {
        AccessibilitySettings()
    }

internal val LocalSystemAccessibilitySnapshot:
    ProvidableCompositionLocal<SystemAccessibilitySettings?> =
    compositionLocalOf {
        null
    }

/** Provides accessibility preferences and observes system settings once at the composition root. */
@Composable
public fun ProvideAccessibility(
    preferences: AccessibilityPreferences? = null,
    content: @Composable () -> Unit,
) {
    val inheritedSystem = LocalSystemAccessibilitySnapshot.current
    val system = inheritedSystem ?: rememberSystemAccessibilitySettings()
    val configured = preferences ?: LocalAccessibilityPreferences.current
    val resolved = configured.resolveAccessibility(system)
    CompositionLocalProvider(
        LocalSystemAccessibilitySnapshot provides system,
        LocalAccessibilityPreferences provides configured,
        LocalAccessibilitySettings provides resolved,
        content = content,
    )
}
