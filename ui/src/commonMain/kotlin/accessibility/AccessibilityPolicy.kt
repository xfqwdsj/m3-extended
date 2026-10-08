package top.ltfan.m3x.compose.ui.accessibility

import androidx.compose.runtime.Composable

/** Whether programmatic spatial motion is allowed by the resolved accessibility settings. */
public val SpatialMotionEnabled: Boolean
    @Composable inline get() = !LocalAccessibilitySettings.current.reduceMotion

/** Whether background materials may reveal content behind them. */
public val BackgroundTransparencyEnabled: Boolean
    @Composable inline get() = !LocalAccessibilitySettings.current.reduceTransparency

/** Whether semantic colors and material boundaries need increased contrast. */
public val IncreasedContrastEnabled: Boolean
    @Composable inline get() = LocalAccessibilitySettings.current.increaseContrast
