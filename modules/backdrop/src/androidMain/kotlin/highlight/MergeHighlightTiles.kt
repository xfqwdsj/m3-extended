package top.ltfan.m3x.backdrop.highlight

import androidx.compose.ui.graphics.RenderEffect
import top.ltfan.m3x.backdrop.BackdropImageInput
import top.ltfan.m3x.backdrop.RuntimeShader

internal actual val supportsMergedHighlightTiles: Boolean = false

internal actual fun mergeHighlightTiles(
    environment: BackdropImageInput,
    shader: RuntimeShader,
    tiles: HighlightTiles,
    field: HighlightSdfField?,
): RenderEffect? = null
