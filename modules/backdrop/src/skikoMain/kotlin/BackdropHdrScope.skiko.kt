package top.ltfan.m3x.backdrop

import androidx.compose.runtime.Composable
import kotlin.time.Duration

@Composable
internal actual fun rememberBackdropHdrState(interval: Duration): BackdropHdrState =
    BackdropHdrState()
