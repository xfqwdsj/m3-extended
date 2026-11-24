# Standalone Backdrop surfaces

A backdrop has two participants: a source that supplies pixels and a surface that processes them.
Recording the background separately lets several surfaces sample the same content without including
their own foreground in that source.

## Add the dependency

Add `top.ltfan.m3x:backdrop:<version>` using the `backdrop` group's version. Follow the
[repository setup](../../README.md#getting-started) for dependency and repository configuration.
For an M3X Material page, use the [page host and capture](../backdrops.md).

## Record and sample

This example uses only Compose Foundation and UI alongside Backdrop:

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.ltfan.m3x.backdrop.BackdropBlur
import top.ltfan.m3x.backdrop.BackdropStyle
import top.ltfan.m3x.backdrop.backdrops.layerBackdrop
import top.ltfan.m3x.backdrop.backdrops.rememberLayerBackdrop
import top.ltfan.m3x.backdrop.drawBackdrop
import top.ltfan.m3x.backdrop.styledSurface

@Composable
fun BackdropExample() {
    val backdrop = rememberLayerBackdrop()
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.matchParentSize()
                .layerBackdrop(backdrop)
                .background(
                    Brush.linearGradient(listOf(Color(0xFF356DF3), Color(0xFFF19CCB)))
                )
        )
        Box(
            Modifier.align(Alignment.Center).size(160.dp).drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(24.dp) },
            ) {
                styledSurface(BackdropStyle { blur = { BackdropBlur.Uniform(12.dp) } })
            }
        )
    }
}
```

`layerBackdrop` records the content after that modifier. `drawBackdrop` maps the recorded layer
into the receiver's coordinates, processes it, and draws the receiver's content above it. Put the
background and receiver in separate branches as shown. Give both a real layout size; recording an
empty source provides no pixels to sample.

`BackdropStyle { }` constructs delayed appearance overrides, with distances expressed in dp.
`styledSurface` inherits unspecified fields from the receiving [theme](styles-and-decorations.md)
and applies a fixed order: color controls, blur, vibrancy and lens, followed by shape clipping.
Outer shadow, processed source, fill, interior lighting, and the highlight draw behind
children. The inner shadow draws above them.

Use `surface { }` to configure optical stages and drawing within surface phases, or use the
[image DSL](render-graphs.md) for independent branches, masks, arbitrary ordering and export.
Inside an `effects { }` block, the scope is a `Density`; convert dp distances with `toPx()` there.

For a uniform gradient, blur has little visible structure to soften; replace it with textured or
scrolling background content to inspect the sampling. Capability and fallback behavior are covered
in [Platforms and HDR](platforms-and-hdr.md).

## Continue building a component

Use [styles](styles-and-decorations.md) to share a visual configuration across receivers. Use
[sources and surfaces](sources-and-surfaces.md) for transparent boundaries and
exported sources. For fade regions such as scrolling bars, combine a ramp and progressive blur as
described in [effects and sampling](effects-and-sampling.md).
