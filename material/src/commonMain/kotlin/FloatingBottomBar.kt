/*
 * Copyright 2025 Kyant
 *
 * Modified for M3X.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package top.ltfan.m3x.material

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.launch
import top.ltfan.m3x.compose.animation.core.withDefaultVisibilityThreshold
import top.ltfan.m3x.compose.foundation.gestures.DampedDragState
import top.ltfan.m3x.compose.ui.accessibility.SpatialMotionEnabled
import top.ltfan.m3x.material.theme.extended

/**
 * A content-width floating navigation bar with a draggable liquid indicator.
 *
 * The selected renderer supplies the surface and indicator optics. The bar reads its full shape
 * from [MaterialTheme.extended], and derives its colors from [MaterialTheme.colorScheme]. The
 * [content] DSL supplies the tabs, so their count and width always come from the same entries being
 * drawn. Every tab is at least [tabMinWidth] wide. A wider label increases every equal slot to the
 * width it needs. Short labels retain the minimum slot width, keeping the bar's proportions
 * independent of label length. The complete Backdrop renderer uses its translucent fallback tint
 * and a solid selection indicator while preserving the same tab interactions.
 *
 * [selectedTabIndex] controls the indicator and every tab's selection semantics. [onTabSelected] is
 * called for every tab click and when a drag selects a different tab. An out-of-range index shows
 * no selected tab or indicator until the caller supplies a valid index or a tab is clicked. The
 * indicator samples the already-rendered tabs and uses press-driven refraction; the bar's blur
 * stays beneath tab content. [indicatorEffects] overrides that refraction and receives press
 * progress. Pressing a different tab brings the indicator to the touch position; dragging then
 * continuously retargets the same selection animation to the latest pointer position. An empty DSL
 * draws no bar; a single tab is drawn without drag gestures.
 *
 * @param selectedTabIndex the index of the selected tab, or an out-of-range value for no selection
 * @param onTabSelected called when a tab is clicked or the indicator drag selects another tab
 * @param modifier the [Modifier] applied to the bar
 * @param indicatorEffects optional effects that replace the indicator's press-driven refraction;
 *   the receiver provides the current press progress
 * @param tabMinWidth the minimum width of one tab; the bar wraps to at least `tabCount *
 *   tabMinWidth` plus its 4dp inset on each side, and `0.dp` sizes the bar purely to its tab
 *   content
 * @param content the tab entries that make up the bar
 */
@Composable
public fun FloatingBottomBar(
    selectedTabIndex: Int,
    onTabSelected: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    indicatorEffects: MaterialSurfaceEffects? = null,
    tabMinWidth: Dp = 76.dp,
    content: FloatingBottomBarScope.() -> Unit,
) {
    val entries = buildFloatingBottomBarItems(content)
    val itemCount = entries.size
    if (itemCount == 0) return
    val hasSelection = selectedTabIndex in entries.indices
    val shape = MaterialTheme.extended.shapes.full
    val motionScheme = MaterialTheme.motionScheme
    val selectionMotion = remember(motionScheme) { motionScheme.defaultSpatialSpec<Float>() }
    val offsetMotion =
        remember(selectionMotion) { selectionMotion.withDefaultVisibilityThreshold(1f) }
    val effectsMotion = remember(motionScheme) { motionScheme.slowEffectsSpec<Float>() }
    val scaleMotion = remember(motionScheme) { motionScheme.slowSpatialSpec<Float>() }
    val spatialMotion = rememberUpdatedState(SpatialMotionEnabled)
    val currentOnTabSelected = rememberUpdatedState(onTabSelected)
    val visuals = LocalFloatingBottomBarRenderer.current.appearance(shape)
    val containerColor = visuals.containerColor
    val reduceMotion = !SpatialMotionEnabled
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val tabInsetPx = with(density) { 4.dp.toPx() }
    val animationScope = rememberCoroutineScope()

    var tabWidthPx by remember(itemCount) { mutableFloatStateOf(0f) }
    var totalWidthPx by remember(itemCount) { mutableFloatStateOf(0f) }

    val offsetAnimation = remember { Animatable(0f) }
    val rubberBandPx = with(density) { 4f.dp.toPx() }
    val panelOffset by
        remember(rubberBandPx) {
            derivedStateOf {
                if (totalWidthPx == 0f) {
                    0f
                } else {
                    val fraction = (offsetAnimation.value / totalWidthPx).fastCoerceIn(-1f, 1f)
                    rubberBandPx * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }

    var currentIndex by
        remember(itemCount) {
            mutableIntStateOf(selectedTabIndex)
        }
    val currentSelectedTabIndex = rememberUpdatedState(selectedTabIndex)
    val dampedDragAnimation =
        remember(
            animationScope,
            itemCount,
            selectionMotion,
            effectsMotion,
            scaleMotion,
            isLtr,
            tabInsetPx,
        ) {
            var dragged = false
            var startedOnSelected = true
            var dragValue = 0f

            fun tabCoordinate(x: Float): Float {
                val localX = x - panelOffset
                return (if (isLtr) localX - tabInsetPx else totalWidthPx - tabInsetPx - localX) /
                    tabWidthPx
            }

            fun fingerValue(x: Float): Float =
                (tabCoordinate(x) - 0.5f).fastCoerceIn(0f, (itemCount - 1).toFloat())

            DampedDragState(
                animationScope = animationScope,
                initialValue = selectedTabIndex.coerceIn(0, itemCount - 1).toFloat(),
                valueRange = 0f..(itemCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                valueAnimationSpec = selectionMotion,
                effectsAnimationSpec = effectsMotion,
                spatialAnimationSpec = scaleMotion,
                spatialMotionEnabled = { spatialMotion.value },
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStarted = { position ->
                    dragged = false
                    dragValue = targetValue
                    val selected = currentSelectedTabIndex.value
                    val coordinate =
                        if (tabWidthPx > 0f) tabCoordinate(position.x) else selected.toFloat()
                    startedOnSelected = coordinate >= selected && coordinate < selected + 1f
                    if (!startedOnSelected && tabWidthPx > 0f) {
                        updateValue(fingerValue(position.x))
                    }
                },
                onDragStopped = { cancelled, position ->
                    val target =
                        if (!startedOnSelected && position != null && tabWidthPx > 0f) {
                            fingerValue(position.x)
                        } else {
                            targetValue
                        }
                    val targetIndex = target.fastRoundToInt().fastCoerceIn(0, itemCount - 1)
                    if (cancelled) {
                        animateToValue(currentIndex.fastCoerceIn(0, itemCount - 1).toFloat())
                    } else if (targetIndex != currentIndex) {
                        currentIndex = targetIndex
                        currentOnTabSelected.value(targetIndex)
                        animateToValue(targetIndex.toFloat())
                    } else {
                        // The indicator covers the selected item and receives its tap.
                        if (!dragged) currentOnTabSelected.value(targetIndex)
                        animateToValue(targetIndex.toFloat())
                    }
                    animationScope.launch {
                        if (spatialMotion.value) offsetAnimation.animateTo(0f, offsetMotion)
                        else offsetAnimation.snapTo(0f)
                    }
                },
                onDrag = { _, position, dragAmount ->
                    if (dragAmount != Offset.Zero) dragged = true
                    if (tabWidthPx > 0f && dragAmount.x != 0f) {
                        if (startedOnSelected) {
                            dragValue += dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f
                            updateValue(dragValue)
                        } else {
                            updateValue(fingerValue(position.x))
                        }
                        // The panel sways under the finger on both drag paths, so the selection
                        // and the movement stay one gesture whether it began on the
                        // selected tab.
                        animationScope.launch {
                            offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                        }
                    }
                },
            )
        }
    LaunchedEffect(dampedDragAnimation, selectedTabIndex, hasSelection) {
        currentIndex = selectedTabIndex
        if (hasSelection) dampedDragAnimation.animateToValue(selectedTabIndex.toFloat())
    }

    val tabContent: @Composable RowScope.() -> Unit = {
        entries.forEachIndexed { index, entry ->
            key(entry.key ?: index) {
                FloatingBottomBarItem(
                    onClick = { currentOnTabSelected.value(index) },
                    // The floor sits outside the entry modifier so it raises the tab's intrinsic
                    // width, which is what the content-width root sums into the bar width.
                    modifier = Modifier.defaultMinSize(minWidth = tabMinWidth).then(entry.modifier),
                    selected = index == selectedTabIndex,
                    content = entry.content,
                )
            }
        }
    }

    val pressFraction = { if (reduceMotion) 0f else dampedDragAnimation.pressProgress }
    val spatialPressFraction = {
        if (reduceMotion) 0f else dampedDragAnimation.scaleProgress
    }
    val barSurfaceModifier = with(visuals) { Modifier.barModifier(spatialPressFraction) }
    val indicatorSurfaceModifier =
        with(visuals) {
            Modifier.indicatorModifier(dampedDragAnimation, pressFraction, indicatorEffects)
        }

    Box(
        modifier =
            modifier
                .width(IntrinsicSize.Max)
                .run { with(visuals) { interactionModifier() } }
                .then(
                    if (itemCount > 1 && hasSelection) dampedDragAnimation.modifier else Modifier,
                ),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (!visuals.hasSource) {
            Box(
                Modifier.matchParentSize()
                    .zIndex(-2f)
                    .graphicsLayer {
                        translationX = panelOffset
                    }
                    .background(containerColor, shape),
            )
        }
        Row(
            Modifier.onGloballyPositioned { coords ->
                    totalWidthPx = coords.size.width.toFloat()
                    val contentWidthPx = totalWidthPx - with(density) { 8.dp.toPx() }
                    tabWidthPx = (contentWidthPx / itemCount).coerceAtLeast(0f)
                }
                .graphicsLayer {
                    translationX = panelOffset
                }
                .then(barSurfaceModifier)
                .clip(shape)
                .height(64.dp)
                .padding(4.dp)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
            content = {
                CompositionLocalProvider(LocalContentColor provides visuals.contentColor) {
                    tabContent()
                }
            },
        )

        visuals.TabCapture({ panelOffset }, spatialPressFraction, pressFraction, tabContent)

        Box(
            Modifier.matchParentSize().zIndex(if (!visuals.hasIndicatorSource) -1f else 0f),
            contentAlignment = AbsoluteAlignment.CenterLeft,
        ) {
            if (tabWidthPx > 0f && hasSelection) {
                val tabWidthDp = with(density) { tabWidthPx.toDp() }
                Box(
                    Modifier.padding(horizontal = 4.dp)
                        // Sampling follows layout coordinates, including a snap back to the origin.
                        .absoluteOffset {
                            val x =
                                if (isLtr) dampedDragAnimation.value * tabWidthPx + panelOffset
                                else
                                    totalWidthPx -
                                        8.dp.toPx() -
                                        (dampedDragAnimation.value + 1f) * tabWidthPx + panelOffset
                            IntOffset(x.fastRoundToInt(), 0)
                        }
                        .then(indicatorSurfaceModifier)
                        .height(56.dp)
                        .width(tabWidthDp),
                )
            }
        }
    }
}
