/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicatorDefaults as MaterialLoadingIndicatorDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox as MaterialPullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults as MaterialPullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import top.ltfan.m3x.material.theme.extended

/**
 * [PullToRefreshBox] is a container that expects a scrollable layout as content and adds gesture
 * support for manually refreshing when the user swipes downward at the beginning of the content. By
 * default, it uses [PullToRefreshDefaults.LoadingIndicator] as the refresh indicator. The
 * [indicator] slot supports a custom indicator.
 *
 * @param isRefreshing whether a refresh is occurring
 * @param onRefresh callback invoked when the user gesture crosses the threshold, thereby requesting
 *   a refresh.
 * @param modifier the [Modifier] to be applied to this container
 * @param state the state that keeps track of distance pulled
 * @param contentAlignment The default alignment inside the Box.
 * @param indicator the indicator that will be drawn on top of the content when the user begins a
 *   pull or a refresh is occurring
 * @param enabled whether nested scroll events should be consumed by this component
 * @param threshold how much distance can be scrolled down before [onRefresh] is invoked
 * @param content the content of the pull refresh container, typically a scrollable layout such as
 *   [LazyColumn] or a layout using [verticalScroll]
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
public fun PullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: PullToRefreshState = rememberPullToRefreshState(),
    contentAlignment: Alignment = Alignment.TopStart,
    indicator: @Composable BoxScope.() -> Unit = {
        PullToRefreshDefaults.LoadingIndicator(
            modifier = Modifier.align(Alignment.TopCenter),
            isRefreshing = isRefreshing,
            state = state,
        )
    },
    enabled: Boolean = true,
    threshold: Dp = MaterialPullToRefreshDefaults.PositionalThreshold,
    content: @Composable BoxScope.() -> Unit,
): Unit =
    MaterialPullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        contentAlignment = contentAlignment,
        indicator = indicator,
        enabled = enabled,
        threshold = threshold,
        content = content,
    )

/** Theme-aware expressive pull-to-refresh indicator. */
public object PullToRefreshDefaults {
    /**
     * A container that handles the size, offset, clipping, shadow, and background drawing for a
     * pull-to-refresh indicator, useful when implementing custom indicators. Its default shape
     * follows the M3X full shape.
     *
     * @param state the state of this modifier, will use `state.distanceFraction` and [maxDistance]
     *   to calculate the offset
     * @param isRefreshing whether a refresh is occurring
     * @param modifier the modifier applied to this layout
     * @param maxDistance the max distance the indicator can be pulled down before a refresh is
     *   triggered on release
     * @param shape the [Shape] of this indicator
     * @param containerColor the container color of this indicator
     * @param elevation the elevation for the indicator
     * @param content content drawn inside this indicator container
     */
    @Composable
    public fun IndicatorBox(
        state: PullToRefreshState,
        isRefreshing: Boolean,
        modifier: Modifier = Modifier,
        maxDistance: Dp = MaterialPullToRefreshDefaults.IndicatorMaxDistance,
        shape: Shape = MaterialTheme.extended.shapes.full,
        containerColor: Color = Color.Unspecified,
        elevation: Dp = MaterialPullToRefreshDefaults.Elevation,
        content: @Composable BoxScope.() -> Unit,
    ): Unit =
        MaterialPullToRefreshDefaults.IndicatorBox(
            state,
            isRefreshing,
            modifier,
            maxDistance,
            shape,
            containerColor,
            elevation,
            content,
        )

    /**
     * A [LoadingIndicator] indicator for [PullToRefreshBox].
     *
     * @param state the state of this modifier, will use `state.distanceFraction` and [maxDistance]
     *   to calculate the offset
     * @param isRefreshing whether a refresh is occurring
     * @param modifier the modifier applied to this layout
     * @param containerColor the container color of this indicator
     * @param color the color of this indicator
     * @param elevation the elevation of this indicator
     * @param maxDistance the max distance the indicator can be pulled down before a refresh is
     *   triggered on release
     */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    public fun LoadingIndicator(
        state: PullToRefreshState,
        isRefreshing: Boolean,
        modifier: Modifier = Modifier,
        containerColor: Color = MaterialPullToRefreshDefaults.loadingIndicatorContainerColor,
        color: Color = MaterialPullToRefreshDefaults.loadingIndicatorColor,
        elevation: Dp = MaterialPullToRefreshDefaults.LoadingIndicatorElevation,
        maxDistance: Dp = MaterialPullToRefreshDefaults.IndicatorMaxDistance,
    ) {
        MaterialPullToRefreshDefaults.IndicatorBox(
            modifier =
                modifier.size(
                    width = MaterialLoadingIndicatorDefaults.ContainerWidth,
                    height = MaterialLoadingIndicatorDefaults.ContainerHeight,
                ),
            state = state,
            isRefreshing = isRefreshing,
            containerColor = containerColor,
            elevation = elevation,
            maxDistance = maxDistance,
            shape = MaterialTheme.extended.shapes.full,
        ) {
            Crossfade(
                targetState = isRefreshing,
                animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
            ) { refreshing ->
                if (refreshing) {
                    ContainedLoadingIndicator(
                        modifier =
                            Modifier.requiredSize(
                                width = MaterialLoadingIndicatorDefaults.ContainerWidth,
                                height = MaterialLoadingIndicatorDefaults.ContainerHeight,
                            ),
                        containerColor = containerColor,
                        indicatorColor = color,
                    )
                } else {
                    // The LoadingIndicator will rotate and morph for a coerced progress value of 0
                    // to 1. When the state's distanceFraction is above one, we rotate the entire
                    // component we have a continuous rotation until the refreshing flag is true.
                    ContainedLoadingIndicator(
                        progress = { state.distanceFraction },
                        modifier =
                            Modifier.requiredSize(
                                    width = MaterialLoadingIndicatorDefaults.ContainerWidth,
                                    height = MaterialLoadingIndicatorDefaults.ContainerHeight,
                                )
                                .drawWithContent {
                                    val progress = state.distanceFraction
                                    if (progress > 1f) {
                                        // Start the rotation on progress - 1 (i.e. 0) to avoid a
                                        // jump that would be more noticeable on some
                                        // LoadingIndicator shapes.
                                        rotate(-(progress - 1) * 180) {
                                            this@drawWithContent.drawContent()
                                        }
                                    } else {
                                        drawContent()
                                    }
                                },
                        containerColor = containerColor,
                        indicatorColor = color,
                    )
                }
            }
        }
    }
}
