package top.ltfan.m3x.backdrop.internal

import androidx.compose.ui.geometry.Rect

@PublishedApi
internal val Rect.hasFiniteEdges: Boolean
    get() = left.isFinite() && top.isFinite() && right.isFinite() && bottom.isFinite()

@PublishedApi
internal val Rect.hasFiniteSize: Boolean
    get() = width.isFinite() && height.isFinite()

@PublishedApi
internal val Rect.isOrdered: Boolean
    get() = left <= right && top <= bottom
