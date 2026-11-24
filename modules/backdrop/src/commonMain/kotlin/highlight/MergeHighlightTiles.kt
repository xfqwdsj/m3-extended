package top.ltfan.m3x.backdrop.highlight

import androidx.compose.ui.graphics.RenderEffect
import top.ltfan.m3x.backdrop.BackdropImageInput
import top.ltfan.m3x.backdrop.RuntimeShader

internal expect val supportsMergedHighlightTiles: Boolean

/** Returns a shared environment filter on platforms that support merging cropped branches. */
internal expect fun mergeHighlightTiles(
    environment: BackdropImageInput,
    shader: RuntimeShader,
    tiles: HighlightTiles,
    field: HighlightSdfField?,
): RenderEffect?
