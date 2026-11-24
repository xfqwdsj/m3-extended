package top.ltfan.m3x.compose.shape

import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.abs

public typealias PathSegments = List<PathSegment>

/** Writes the segments to a path, retaining the sequence's open or closed geometry. */
public fun PathSegments.toPath(): Path = Path().apply { addSegments(this@toPath) }

internal fun Path.addSegments(segments: PathSegments) {
    if (segments.isEmpty()) return
    val start = segments.first().start
    moveTo(start.x.toFloat(), start.y.toFloat())
    segments.forEach { it.drawTo(this) }
}

public fun PathSegments.toSvg(asDocument: Boolean = false): String = buildString {
    if (asDocument) {
        val bounds = this@toSvg.toPath().getBounds()
        append("""<svg xmlns="http://www.w3.org/2000/svg" """)
        appendLine("""viewBox="${bounds.left} ${bounds.top} ${bounds.width} ${bounds.height}">""")
    }

    if (isNotEmpty()) {
        if (asDocument) {
            append("""  <path d="""")
        }

        val startPoint = this@toSvg.first().start
        append("M${startPoint.x} ${startPoint.y}")

        this@toSvg.forEach { segment ->
            val string =
                when (segment) {
                    is PathSegment.Line -> "L${segment.end.x} ${segment.end.y}"
                    is PathSegment.Arc -> {
                        val largeArcFlag = if (abs(segment.sweepRadians) > PI) 1 else 0
                        val sweepFlag = if (segment.sweepRadians > 0) 1 else 0
                        "A${segment.radius} ${segment.radius} 0 $largeArcFlag $sweepFlag ${segment.end.x} ${segment.end.y}"
                    }

                    is PathSegment.Circle -> {
                        val cx = segment.center.x
                        val cy = segment.center.y
                        val r = segment.radius
                        "M${cx + r} $cy A$r $r 0 1 0 ${cx - r} $cy A$r $r 0 1 0 ${cx + r} $cy Z"
                    }

                    is PathSegment.Cubic ->
                        "C${segment.control1.x} ${segment.control1.y}, ${segment.control2.x} ${segment.control2.y}, ${segment.end.x} ${segment.end.y}"
                }
            append(string)
        }

        if (asDocument) {
            appendLine(""""/>""")
        }
    }
    if (asDocument) {
        appendLine("""</svg>""")
    }
}
