package top.ltfan.m3x.material.backdrop

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.dp
import top.ltfan.m3x.backdrop.BackdropBlur
import top.ltfan.m3x.backdrop.BackdropLens
import top.ltfan.m3x.backdrop.highlight.Highlight
import top.ltfan.m3x.backdrop.shadow.Shadow
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropTheme

/** Material optical presets; Material colors, typography, and semantics remain configurable. */
public object BackdropPresets {
    @Stable public val GlassRegular: M3xBackdropTheme = M3xBackdropTheme()

    /** Thin glass with a translucent Material tint and the theme's stable foreground. */
    @Stable
    public val GlassClear: M3xBackdropTheme =
        GlassRegular.copy(
            control =
                GlassRegular.control.copy(
                    style =
                        GlassRegular.control.style {
                            blur = { BackdropBlur.Uniform(2.dp) }
                        },
                    tintAlpha = 0.16f,
                ),
            panel =
                GlassRegular.panel.copy(
                    style =
                        GlassRegular.panel.style {
                            blur = { BackdropBlur.Uniform(8.dp) }
                        },
                    tintAlpha = 0.28f,
                ),
            inversePanel =
                GlassRegular.inversePanel.copy(
                    style =
                        GlassRegular.inversePanel.style {
                            blur = { BackdropBlur.Uniform(8.dp) }
                        },
                ),
        )

    /** Soft Material surfaces with blur, stable Material foregrounds, and quiet decoration. */
    @Stable
    public val MaterialBlur: M3xBackdropTheme =
        GlassRegular.copy(
            control =
                GlassRegular.control.copy(
                    style =
                        GlassRegular.control.style {
                            blur = { BackdropBlur.Uniform(8.dp) }
                            vibrancy = { 1f }
                            lens = { BackdropLens.None }
                            highlight = { Highlight.None }
                            shadow = { Shadow(radius = 8.dp) }
                        },
                    tintAlpha = 0.6f,
                ),
            panel =
                GlassRegular.panel.copy(
                    style =
                        GlassRegular.panel.style {
                            vibrancy = { 1f }
                            lens = { BackdropLens.None }
                            highlight = { Highlight.None }
                        },
                    tintAlpha = 0.8f,
                ),
            inversePanel =
                GlassRegular.inversePanel.copy(
                    style =
                        GlassRegular.inversePanel.style {
                            vibrancy = { 1f }
                            lens = { BackdropLens.None }
                            highlight = { Highlight.None }
                        },
                ),
        )
}
