package top.ltfan.m3x.compose.ui.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * Touch exploration and switch access are accessibility services of Android: this platform has none
 * to detect, so a tooltip only becomes focusable here through keyboard navigation.
 */
@Composable
public actual fun rememberTouchExplorationOrSwitchAccessState(): State<Boolean> = remember {
    mutableStateOf(false)
}
