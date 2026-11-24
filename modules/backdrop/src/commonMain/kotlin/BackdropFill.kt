package top.ltfan.m3x.backdrop

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.isSpecified

/**
 * A surface fill with one fill source and an optional progressive ramp. A null [brush] delegates
 * the fill to consumer policy. [alpha] multiplies the fill's own opacity; null delegates that
 * strength to the consumer. Graph and styled-surface fill draw no brush for null and use alpha one
 * for null strength. Consumers can resolve those values through their theme before applying the
 * fill. [ramp] and [edge] control fill coverage independently of blur.
 */
@Immutable
public data class BackdropFill(
    public val brush: Brush? = null,
    public val alpha: Float? = null,
    public val ramp: BackdropRamp? = null,
    public val edge: BackdropEdge = Top,
) {
    public companion object {
        /** Explicit transparent fill without a ramp. */
        @Stable public val None: BackdropFill = BackdropFill(Color.Transparent)
    }

    /**
     * Creates a solid fill. [Color.Unspecified] delegates the fill to the consumer's theme.
     *
     * @param color the fill color.
     */
    public constructor(
        color: Color,
        alpha: Float? = null,
        ramp: BackdropRamp? = null,
        edge: BackdropEdge = Top,
    ) : this(if (color.isSpecified) SolidColor(color) else null, alpha, ramp, edge)
}
