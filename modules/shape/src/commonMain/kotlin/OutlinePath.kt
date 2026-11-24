package top.ltfan.m3x.compose.shape

import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline

/** Copies this outline into a Compose path, retaining the outline's fill rule. */
public fun Outline.toPath(): Path =
    Path().apply {
        addOutline(this@toPath)
        if (this@toPath is Outline.Generic) fillType = this@toPath.path.fillType
    }
