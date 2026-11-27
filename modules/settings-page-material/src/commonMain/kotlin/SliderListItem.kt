package top.ltfan.m3x.settingspage.material

import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.SegmentedListItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.ltfan.m3x.compose.material3.ListItemColorsTransparent

/** One Material list-item surface for a numeric value and its inline slider controls. */
@Composable
internal fun SliderListItem(
    onClick: () -> Unit,
    shapes: ListItemShapes?,
    modifier: Modifier,
    enabled: Boolean,
    leadingContent: (@Composable () -> Unit)?,
    supportingContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors =
        if (shapes == null) ListItemColorsTransparent else ListItemDefaults.segmentedColors()
    val resolvedShapes = shapes ?: ListItemDefaults.shapes()
    SegmentedListItem(
        onClick = onClick,
        shapes = resolvedShapes,
        modifier = modifier,
        enabled = enabled,
        leadingContent = leadingContent,
        supportingContent = supportingContent,
        colors = colors,
        content = content,
    )
}
