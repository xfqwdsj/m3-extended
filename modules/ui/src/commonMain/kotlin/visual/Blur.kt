package top.ltfan.m3x.compose.ui.visual

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.ltfan.m3x.compose.ui.accessibility.BackgroundTransparencyEnabled

/** CompositionLocal to control whether blur effects are enabled. */
public val LocalBlurEnabled: ProvidableCompositionLocal<Boolean> = compositionLocalOf { true }

/** Whether the built-in backdrop blur is allowed by both preferences and accessibility. */
public val BackdropBlurEnabled: Boolean
    @Composable inline get() = LocalBlurEnabled.current && BackgroundTransparencyEnabled

/** Default maximum foreground blur radius during entrance and exit. */
public val DefaultEnterExitBlurRadius: Dp = 16.dp
