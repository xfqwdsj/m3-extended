package top.ltfan.m3x.backdrop.highlight

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

/** Controls when a contour highlight prepares a reusable signed distance field. */
public enum class HighlightSdfMode {
    /** Evaluate the contour on each draw. */
    Off,

    /** Prepare a half-resolution field after the first contour frame. */
    On,

    /** Prepare a half-resolution field when a complex contour has demonstrated reuse. */
    Auto,

    /** Present a complete quarter-resolution field, then refine to half resolution. */
    Progressive,
}

/** Selects the signed distance field preparation backend. */
public enum class HighlightSdfBackend {
    /** Select a backend according to the field's estimated preparation cost. */
    Auto,

    /** Bake distance and normals on a CPU worker. */
    Cpu,

    /** Use GPU baking where available, with contour rendering as the fallback. */
    Gpu,
}

/** Signed distance field policy for contour highlights in this Compose subtree. */
@Immutable
public data class HighlightSdfSettings(
    public val mode: HighlightSdfMode = Auto,
    public val backend: HighlightSdfBackend = Auto,
)

/** Supplies the signed distance field policy to Compose Backdrop renderers. */
public val LocalHighlightSdfSettings: ProvidableCompositionLocal<HighlightSdfSettings> =
    compositionLocalOf {
        HighlightSdfSettings()
    }
