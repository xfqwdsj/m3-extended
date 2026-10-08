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

package top.ltfan.m3x.material.backdrop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import kotlin.time.Duration
import top.ltfan.backdrop.BackdropEffectScope
import top.ltfan.backdrop.backdrops.layerBackdrop
import top.ltfan.backdrop.backdrops.rememberCombinedBackdrop
import top.ltfan.backdrop.backdrops.rememberLayerBackdrop
import top.ltfan.backdrop.drawBackdrop
import top.ltfan.backdrop.effects.lens
import top.ltfan.backdrop.highlight.Highlight
import top.ltfan.backdrop.interaction.InteractionLight
import top.ltfan.backdrop.interaction.interactionLightSource
import top.ltfan.backdrop.shadow.Shadow
import top.ltfan.m3x.backdrop.LocalBackdrop
import top.ltfan.m3x.compose.foundation.gestures.DampedDragState
import top.ltfan.m3x.compose.ui.accessibility.BackgroundTransparencyEnabled
import top.ltfan.m3x.material.FloatingBottomBarRenderer
import top.ltfan.m3x.material.FloatingBottomBarVisuals
import top.ltfan.m3x.material.MaterialSurfaceEffects
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.ProvideFloatingBottomBarTabScale

/** Backdrop-specific effects for the floating indicator's press progress. */
public class BackdropIndicatorEffects(public val block: BackdropEffectScope.(Float) -> Unit) :
    MaterialSurfaceEffects

/** Supplies a Backdrop indicator effect chain to the renderer-independent bar. */
public fun backdropIndicatorEffects(
    block: BackdropEffectScope.(Float) -> Unit,
): MaterialSurfaceEffects = BackdropIndicatorEffects(block)

/**
 * Optical integration for the ordinary floating bar's single interaction and layout implementation.
 */
public object BackdropFloatingBottomBarRenderer : FloatingBottomBarRenderer {
    @Composable
    override fun appearance(shape: Shape): FloatingBottomBarVisuals {
        val colorScheme = MaterialTheme.colorScheme
        val accentColor = colorScheme.primary
        val appearance =
            rememberMaterialAppearance(
                MaterialSurfaceRole.Control,
                containerColor = colorScheme.surface,
                contentColor = colorScheme.onSurface,
                shape = shape,
            )
        val containerColor =
            if (LocalBackdrop.current == null) appearance.fallback else appearance.tint
        val highlightColor = colorScheme.onSurface

        val reduceTransparency = !BackgroundTransparencyEnabled
        val sourceBackdrop = LocalBackdrop.current?.backdrop
        // The tab layer contributes only its recorded area; enlarged indicators sample the page
        // beyond it.
        val tabsBackdrop =
            if (sourceBackdrop != null) rememberLayerBackdrop(tileMode = TileMode.Decal) else null
        val combinedBackdrop =
            if (sourceBackdrop != null && tabsBackdrop != null) {
                rememberCombinedBackdrop(sourceBackdrop, tabsBackdrop)
            } else {
                null
            }

        return object : FloatingBottomBarVisuals {
            override val hasSource: Boolean = sourceBackdrop != null
            override val hasIndicatorSource: Boolean = combinedBackdrop != null
            override val containerColor: Color = containerColor
            override val contentColor: Color = appearance.contentColor

            @Composable
            override fun Modifier.barModifier(spatialPressFraction: () -> Float): Modifier {
                return if (sourceBackdrop == null) this
                else
                    materialBackdropSurface(
                        appearance,
                        shape,
                        observeInput = false,
                        layerBlock = {
                            val progress = spatialPressFraction()
                            val scale = lerp(1f, 1f + 16.dp.toPx() / size.width, progress)
                            scaleX = scale
                            scaleY = scale
                        },
                    )
            }

            @Composable
            override fun Modifier.indicatorModifier(
                dampedDragAnimation: DampedDragState,
                pressFraction: () -> Float,
                indicatorEffects: MaterialSurfaceEffects?,
            ): Modifier {
                return if (combinedBackdrop == null) {
                    background(colorScheme.surfaceContainerHighest, shape)
                        .border(1.dp, colorScheme.outline, shape)
                } else {
                    drawBackdrop(
                        backdrop = combinedBackdrop,
                        shape = { shape },
                        effects = {
                            val progress = pressFraction()
                            if (!reduceTransparency) {
                                if (indicatorEffects != null)
                                    (indicatorEffects as BackdropIndicatorEffects).block(
                                        this,
                                        progress,
                                    )
                                else if (shape is CornerBasedShape)
                                    lens(
                                        10.dp.toPx() * progress,
                                        14.dp.toPx() * progress,
                                        chromaticAberration = true,
                                    )
                            }
                        },
                        highlight = {
                            val base = appearance.style.highlight()
                            if (base is Highlight.Config)
                                base.copy(opacity = base.opacity * pressFraction())
                            else base
                        },
                        shadow = {
                            val base = appearance.style.shadow()
                            if (base is Shadow.Config)
                                base.copy(alpha = base.alpha * pressFraction())
                            else base
                        },
                        innerShadow = appearance.style.innerShadow,
                        interactionLighting = appearance.style.interactionLighting,
                        layerBlock = {
                            scaleX = if (appearance.reduceMotion) 1f else dampedDragAnimation.scaleX
                            scaleY = if (appearance.reduceMotion) 1f else dampedDragAnimation.scaleY
                            val velocity =
                                if (appearance.reduceMotion) 0f
                                else dampedDragAnimation.velocity / 10f
                            scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                            scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                        },
                        onDrawSurface = {
                            val progress = pressFraction()
                            drawRect(
                                highlightColor.copy(alpha = 0.1f),
                                alpha = 1f - progress,
                            )
                            drawRect(highlightColor.copy(alpha = 0.03f * progress))
                        },
                    )
                }
            }

            @Composable
            override fun Modifier.interactionModifier(): Modifier =
                interactionLightSource(
                    light = {
                        if (
                            sourceBackdrop != null &&
                                appearance.material.interactiveLighting &&
                                appearance.lighting.enabled
                        )
                            if (appearance.reduceMotion)
                                InteractionLight.Config(
                                    color = appearance.lighting.color,
                                    radius = appearance.lighting.radius,
                                    releaseDuration = Duration.ZERO,
                                )
                            else
                                InteractionLight.Config(
                                    color = appearance.lighting.color,
                                    radius = appearance.lighting.radius,
                                )
                        else InteractionLight.None
                    },
                )

            @Composable
            override fun TabCapture(
                panelOffset: () -> Float,
                spatialPressFraction: () -> Float,
                pressFraction: () -> Float,
                tabContent: @Composable RowScope.() -> Unit,
            ) {
                if (sourceBackdrop != null && tabsBackdrop != null)
                    ProvideFloatingBottomBarTabScale(
                        scale = { lerp(1f, 1.2f, spatialPressFraction()) },
                    ) {
                        Row(
                            Modifier.clearAndSetSemantics {}
                                .alpha(0f)
                                // The sway transform sits outside the recording: the layer records
                                // the
                                // tabs in the row's own coordinates, where they fit its bounds, and
                                // the
                                // layer's position carries the movement to consumers like the
                                // indicator.
                                // A transform inside the recording draws past the layer's bounds
                                // and
                                // samples back shifted from where the bar draws its own tabs.
                                .graphicsLayer {
                                    translationX = panelOffset()
                                }
                                .layerBackdrop(tabsBackdrop)
                                .drawBackdrop(
                                    backdrop = sourceBackdrop,
                                    shape = { shape },
                                    effects = {
                                        appearance.style.effects(this)
                                    },
                                    highlight = {
                                        val highlight = appearance.style.highlight()
                                        if (highlight is Highlight.Config)
                                            highlight.copy(
                                                opacity = highlight.opacity * pressFraction(),
                                            )
                                        else highlight
                                    },
                                    onDrawSurface = { drawRect(containerColor) },
                                    interactionLighting = appearance.style.interactionLighting,
                                )
                                .height(56.dp)
                                .padding(horizontal = 4.dp)
                                .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
                            verticalAlignment = Alignment.CenterVertically,
                            content = {
                                CompositionLocalProvider(
                                    LocalContentColor provides appearance.contentColor,
                                ) {
                                    tabContent()
                                }
                            },
                        )
                    }
            }
        }
    }
}
