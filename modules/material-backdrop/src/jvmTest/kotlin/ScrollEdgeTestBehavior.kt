package top.ltfan.m3x.material.backdrop

import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable

/** Supplies the explicit overlap required by tests of progressive bar coverage. */
@Composable
internal fun scrolledAppBarBehavior(): TopAppBarScrollBehavior =
    TopAppBarDefaults.pinnedScrollBehavior(
        rememberTopAppBarState(initialHeightOffsetLimit = -64f, initialContentOffset = -1000f),
    )
