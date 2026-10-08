package top.ltfan.m3x.compose.ui.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

/**
 * Whether an accessibility service that reads the screen by touch exploration or switch access is
 * enabled.
 *
 * A tooltip that has an action becomes focusable while such a service is enabled, so that the
 * action is reachable without the user having to find the tooltip by swiping. On the platforms
 * without these services the state is always `false`, and only keyboard navigation makes a tooltip
 * with an action focusable.
 */
@Composable public expect fun rememberTouchExplorationOrSwitchAccessState(): State<Boolean>
