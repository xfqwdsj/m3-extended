/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import top.ltfan.m3x.compose.material3.CardColorsLowest

/**
 * The resolved shape of the nearest M3X card, including trimmed grouped corners. Shared-transition
 * descendants can apply this shape after their shared-bounds modifier.
 */
public val LocalCardShape: ProvidableCompositionLocal<Shape> = staticCompositionLocalOf {
    RectangleShape
}

/**
 * A Material card whose backing uses the selected [MaterialSurfaceRenderer] when [backdrop] is
 * enabled. [backdropMaterial] overrides the renderer's [MaterialSurfaceRole.Surface] style. The
 * renderer determines the drawing and interaction effects; a Backdrop renderer can use captured
 * background pixels and interaction lighting.
 *
 * Cards follow the [Material 3 card guidelines](https://m3.material.io/components/cards/overview).
 *
 * @param modifier the [Modifier] applied to the card
 * @param containerModifier the [Modifier] applied to the column containing the card content
 * @param shape the shape of the card
 * @param colors the colors used for the card container and content
 * @param elevation the elevation of the card
 * @param border the optional border drawn around the card
 * @param backdrop whether the selected renderer draws the backing; otherwise Material draws
 *   [colors] directly
 * @param backdropMaterial the renderer-specific style used when [backdrop] is enabled, or `null` to
 *   use the renderer's surface style
 * @param content the content displayed inside the card
 */
@Composable
public fun Card(
    modifier: Modifier = Modifier,
    containerModifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardColorsLowest,
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    backdrop: Boolean = true,
    backdropMaterial: MaterialSurfaceStyle? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val appearance =
        if (backdrop) {
            rememberSurfaceAppearance(
                MaterialSurfaceRole.Panel,
                backdropMaterial
                    ?: LocalMaterialSurfaceRenderer.current.style(MaterialSurfaceRole.Surface),
                colors.containerColor,
                colors.contentColor,
                shape = shape,
            )
        } else null
    Card(
        modifier =
            if (appearance != null) modifier.materialSurface(appearance, shape) else modifier,
        shape = shape,
        colors =
            if (appearance != null)
                colors.copy(
                    containerColor = Color.Transparent,
                    contentColor = appearance.contentColor,
                )
            else colors,
        elevation = elevation,
        border = border,
    ) {
        CompositionLocalProvider(LocalCardShape provides shape) {
            Column(
                modifier = containerModifier,
                content = content,
            )
        }
    }
}
