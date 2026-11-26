package top.ltfan.m3x.demo

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import top.ltfan.m3x.backdrop.ProgressiveBlurMode
import top.ltfan.m3x.backdrop.highlight.HighlightSdfBackend
import top.ltfan.m3x.backdrop.highlight.HighlightSdfMode
import top.ltfan.m3x.backdrop.highlight.HighlightSdfSettings
import top.ltfan.m3x.datastore.annotation.Store
import top.ltfan.m3x.demo.generated.resources.MR
import top.ltfan.m3x.demo.generated.resources.demo_accessibility
import top.ltfan.m3x.demo.generated.resources.demo_adaptive_foreground_color
import top.ltfan.m3x.demo.generated.resources.demo_adaptive_foreground_description
import top.ltfan.m3x.demo.generated.resources.demo_appearance
import top.ltfan.m3x.demo.generated.resources.demo_background
import top.ltfan.m3x.demo.generated.resources.demo_background_adaptation
import top.ltfan.m3x.demo.generated.resources.demo_background_adaptation_description
import top.ltfan.m3x.demo.generated.resources.demo_basic
import top.ltfan.m3x.demo.generated.resources.demo_blur
import top.ltfan.m3x.demo.generated.resources.demo_blur_animation
import top.ltfan.m3x.demo.generated.resources.demo_capsule
import top.ltfan.m3x.demo.generated.resources.demo_centered
import top.ltfan.m3x.demo.generated.resources.demo_color_theme
import top.ltfan.m3x.demo.generated.resources.demo_colorful
import top.ltfan.m3x.demo.generated.resources.demo_compose_runtime
import top.ltfan.m3x.demo.generated.resources.demo_contour
import top.ltfan.m3x.demo.generated.resources.demo_control_blur_radius_dp
import top.ltfan.m3x.demo.generated.resources.demo_dark
import top.ltfan.m3x.demo.generated.resources.demo_disabled
import top.ltfan.m3x.demo.generated.resources.demo_edge_highlights
import top.ltfan.m3x.demo.generated.resources.demo_enabled
import top.ltfan.m3x.demo.generated.resources.demo_follow_system
import top.ltfan.m3x.demo.generated.resources.demo_glass_clear
import top.ltfan.m3x.demo.generated.resources.demo_glass_regular
import top.ltfan.m3x.demo.generated.resources.demo_hdr_headroom_ratio
import top.ltfan.m3x.demo.generated.resources.demo_hdr_headroom_ratio_description
import top.ltfan.m3x.demo.generated.resources.demo_hdr_interaction_light
import top.ltfan.m3x.demo.generated.resources.demo_hdr_output_android
import top.ltfan.m3x.demo.generated.resources.demo_hdr_output_android_description
import top.ltfan.m3x.demo.generated.resources.demo_hdr_refresh_interval_description
import top.ltfan.m3x.demo.generated.resources.demo_hdr_refresh_interval_ms
import top.ltfan.m3x.demo.generated.resources.demo_hdr_shared_light_description
import top.ltfan.m3x.demo.generated.resources.demo_highlight_sdf_backend
import top.ltfan.m3x.demo.generated.resources.demo_highlight_sdf_mode
import top.ltfan.m3x.demo.generated.resources.demo_highlight_type
import top.ltfan.m3x.demo.generated.resources.demo_increase_contrast
import top.ltfan.m3x.demo.generated.resources.demo_inner_shadows
import top.ltfan.m3x.demo.generated.resources.demo_large
import top.ltfan.m3x.demo.generated.resources.demo_layered_blur_levels
import top.ltfan.m3x.demo.generated.resources.demo_layered_blur_levels_description
import top.ltfan.m3x.demo.generated.resources.demo_light
import top.ltfan.m3x.demo.generated.resources.demo_link_composer
import top.ltfan.m3x.demo.generated.resources.demo_link_composer_description
import top.ltfan.m3x.demo.generated.resources.demo_live_slider_updates
import top.ltfan.m3x.demo.generated.resources.demo_material_blur
import top.ltfan.m3x.demo.generated.resources.demo_material_shapes
import top.ltfan.m3x.demo.generated.resources.demo_material_style
import top.ltfan.m3x.demo.generated.resources.demo_medium
import top.ltfan.m3x.demo.generated.resources.demo_override_preset_blur_radii
import top.ltfan.m3x.demo.generated.resources.demo_panel_blur_radius_dp
import top.ltfan.m3x.demo.generated.resources.demo_progressive_alpha
import top.ltfan.m3x.demo.generated.resources.demo_progressive_auto
import top.ltfan.m3x.demo.generated.resources.demo_progressive_blur
import top.ltfan.m3x.demo.generated.resources.demo_progressive_blur_description
import top.ltfan.m3x.demo.generated.resources.demo_progressive_full
import top.ltfan.m3x.demo.generated.resources.demo_progressive_layered
import top.ltfan.m3x.demo.generated.resources.demo_reduce_motion
import top.ltfan.m3x.demo.generated.resources.demo_reduce_motion_description
import top.ltfan.m3x.demo.generated.resources.demo_reduce_transparency
import top.ltfan.m3x.demo.generated.resources.demo_refraction
import top.ltfan.m3x.demo.generated.resources.demo_scroll_edge_blur_radius_dp
import top.ltfan.m3x.demo.generated.resources.demo_scroll_edge_fade_below_the_bar_dp
import top.ltfan.m3x.demo.generated.resources.demo_scroll_edge_plateau_dp
import top.ltfan.m3x.demo.generated.resources.demo_sdf_auto
import top.ltfan.m3x.demo.generated.resources.demo_sdf_cpu
import top.ltfan.m3x.demo.generated.resources.demo_sdf_gpu
import top.ltfan.m3x.demo.generated.resources.demo_sdf_off
import top.ltfan.m3x.demo.generated.resources.demo_sdf_on
import top.ltfan.m3x.demo.generated.resources.demo_sdf_progressive
import top.ltfan.m3x.demo.generated.resources.demo_shadows
import top.ltfan.m3x.demo.generated.resources.demo_shape_family
import top.ltfan.m3x.demo.generated.resources.demo_shared_interaction_light
import top.ltfan.m3x.demo.generated.resources.demo_single_row
import top.ltfan.m3x.demo.generated.resources.demo_surface_blur_override_description
import top.ltfan.m3x.demo.generated.resources.demo_system_expressive_default
import top.ltfan.m3x.demo.generated.resources.demo_text_scale
import top.ltfan.m3x.demo.generated.resources.demo_theme_color
import top.ltfan.m3x.demo.generated.resources.demo_top_app_bar
import top.ltfan.m3x.demo.generated.resources.demo_ui_palette
import top.ltfan.m3x.demo.generated.resources.demo_vibrancy
import top.ltfan.m3x.demo.generated.resources.demo_visual_effects
import top.ltfan.m3x.resource.Resource
import top.ltfan.m3x.settingspage.RangeBasedNumberInfo.Type.Slider
import top.ltfan.m3x.settingspage.SettingsResetMode
import top.ltfan.m3x.settingspage.SettingsResources
import top.ltfan.m3x.settingspage.annotation.FiniteFloat
import top.ltfan.m3x.settingspage.annotation.FiniteInt
import top.ltfan.m3x.settingspage.annotation.SettingsItem
import top.ltfan.m3x.settingspage.annotation.SettingsItemType
import top.ltfan.m3x.settingspage.annotation.SettingsStore
import top.ltfan.m3x.settingspage.annotation.SettingsValue

@Serializable
@SettingsStore(resetMode = SettingsResetMode.Menu)
@Store(name = "appearance", codec = DemoJsonCodec::class)
data class DemoPreferences(
    @SettingsItem(AppearanceLabel::class, type = SettingsItemType.Category)
    val appearance: Appearance = Appearance(),
    @SettingsItem(EffectsLabel::class, type = SettingsItemType.Category)
    val effects: Effects = Effects(),
    @SettingsItem(AccessibilityLabel::class, type = SettingsItemType.Category)
    val accessibility: Accessibility = Accessibility(),
    @SettingsItem(RuntimeLabel::class, type = SettingsItemType.Category)
    val runtime: Runtime = Runtime(),
) {
    @Serializable
    data class Runtime(
        @SettingsItem(LinkComposerLabel::class) val linkComposer: Boolean = false,
    )

    @Serializable
    data class Appearance(
        @SettingsItem(ThemeLabel::class) val theme: ThemeMode = ThemeMode.System,
        @SettingsItem(UiPaletteLabel::class) val uiPalette: UiPalette = UiPalette.Theme,
        @SettingsItem(PresetLabel::class) val preset: MaterialPreset = MaterialPreset.Regular,
        @SettingsItem(ShapeLabel::class) val shape: ShapePreset = ShapePreset.Capsule,
        @SettingsItem(BarLabel::class) val bar: BarVariant = BarVariant.Small,
        @SettingsItem(FontScaleLabel::class)
        @FiniteFloat(Slider, 0.8f, 2f, 0.1f)
        val fontScale: Float = 1f,
        @SettingsItem(BackgroundLabel::class)
        val background: PreviewBackground = PreviewBackground.Artwork,
        @SettingsItem(LiveSlidersLabel::class) val liveSliders: Boolean = false,
    )

    @Serializable
    data class Effects(
        @SettingsItem(BlurLabel::class) val blur: Boolean = true,
        @SettingsItem(BlurAnimationLabel::class) val blurAnimation: Boolean = true,
        @SettingsItem(ProgressiveBlurLabel::class, dependsOn = ["blur"])
        val progressiveBlur: DemoProgressiveBlurMode = DemoProgressiveBlurMode.Auto,
        @SettingsItem(LayeredBlurLevelsLabel::class, dependsOn = ["blur"])
        @FiniteInt(Slider, 2, 16, 1)
        val layeredBlurLevels: Int = 8,
        @SettingsItem(LensLabel::class) val lens: Boolean = true,
        @SettingsItem(VibrancyLabel::class) val vibrancy: Boolean = true,
        @SettingsItem(HighlightLabel::class) val highlight: Boolean = true,
        @SettingsItem(HighlightTypeLabel::class, dependsOn = ["highlight"])
        val highlightType: DemoHighlightType = DemoHighlightType.Contour,
        @SettingsItem(HighlightSdfModeLabel::class, dependsOn = ["highlight"])
        val highlightSdfMode: DemoHighlightSdfMode = DemoHighlightSdfMode.Auto,
        @SettingsItem(HighlightSdfBackendLabel::class, dependsOn = ["highlight"])
        val highlightSdfBackend: DemoHighlightSdfBackend = DemoHighlightSdfBackend.Auto,
        @SettingsItem(ShadowLabel::class) val shadow: Boolean = true,
        @SettingsItem(InnerShadowLabel::class) val innerShadow: Boolean = true,
        @SettingsItem(AdaptationLabel::class) val adaptation: Boolean = false,
        @SettingsItem(AdaptiveContentColorLabel::class, dependsOn = ["adaptation"])
        val adaptiveContentColor: Boolean = false,
        @SettingsItem(CustomBlurRadiiLabel::class) val customBlurRadii: Boolean = false,
        @SettingsItem(ControlBlurRadiusLabel::class, dependsOn = ["customBlurRadii"])
        @FiniteFloat(Slider, 0.0f, 64.0f, 1.0f)
        val controlBlurRadiusDp: Float = DemoBackdropDefaults.controlBlurRadius,
        @SettingsItem(PanelBlurRadiusLabel::class, dependsOn = ["customBlurRadii"])
        @FiniteFloat(Slider, 0.0f, 64.0f, 1.0f)
        val panelBlurRadiusDp: Float = DemoBackdropDefaults.panelBlurRadius,
        @SettingsItem(ScrollEdgeBlurRadiusLabel::class, dependsOn = ["customBlurRadii"])
        @FiniteFloat(Slider, 0.0f, 64.0f, 1.0f)
        val scrollEdgeBlurRadiusDp: Float = DemoBackdropDefaults.scrollEdgeBlurRadius,
        @SettingsItem(InteractionLabel::class) val interaction: Boolean = true,
        @SettingsItem(HdrOutputLabel::class) val hdrOutput: Boolean = false,
        @SettingsItem(InteractionHdrLabel::class, dependsOn = ["hdrOutput"])
        val interactionHdr: Boolean = false,
        @SettingsItem(HdrHeadroomLabel::class, dependsOn = ["hdrOutput"])
        @FiniteFloat(Slider, 1.0f, 10.0f, 0.1f)
        val hdrHeadroom: Float = DemoBackdropDefaults.HDR_HEADROOM,
        @SettingsItem(HdrRefreshIntervalLabel::class, dependsOn = ["hdrOutput"])
        @FiniteFloat(Slider, 50.0f, 1000.0f, 50.0f)
        val hdrRefreshIntervalMs: Float = DemoBackdropDefaults.HDR_REFRESH_INTERVAL_MS,
        @SettingsItem(ScrollEdgePlateauLabel::class)
        @FiniteFloat(Slider, 0.0f, 64.0f, 1.0f)
        val scrollEdgePlateauDp: Float = DemoBackdropDefaults.SCROLL_EDGE_PLATEAU,
        @SettingsItem(ScrollEdgeFadeLabel::class)
        @FiniteFloat(Slider, 0.0f, 64.0f, 1.0f)
        val scrollEdgeFadeDp: Float = DemoBackdropDefaults.SCROLL_EDGE_FADE,
    )

    @Serializable
    data class Accessibility(
        @SettingsItem(ReduceTransparencyLabel::class)
        val reduceTransparency: AccessibilityMode = AccessibilityMode.System,
        @SettingsItem(IncreaseContrastLabel::class)
        val increaseContrast: AccessibilityMode = AccessibilityMode.System,
        @SettingsItem(ReduceMotionLabel::class)
        val reduceMotion: AccessibilityMode = AccessibilityMode.System,
    )
}

@Serializable
enum class AccessibilityMode(override val label: Resource<String>) : SettingsResources {
    @SettingsValue System(demoResource(MR.strings.demo_follow_system)),
    @SettingsValue Enabled(demoResource(MR.strings.demo_enabled)),
    @SettingsValue Disabled(demoResource(MR.strings.demo_disabled)),
}

@Serializable
enum class DemoHighlightType(override val label: Resource<String>) : SettingsResources {
    @SettingsValue Contour(demoResource(MR.strings.demo_contour)),
    @SettingsValue Basic(demoResource(MR.strings.demo_basic)),
}

@Serializable
enum class DemoHighlightSdfMode(
    override val label: Resource<String>,
    internal val mode: HighlightSdfMode,
) : SettingsResources {
    @SettingsValue Off(demoResource(MR.strings.demo_sdf_off), HighlightSdfMode.Off),
    @SettingsValue On(demoResource(MR.strings.demo_sdf_on), HighlightSdfMode.On),
    @SettingsValue Auto(demoResource(MR.strings.demo_sdf_auto), HighlightSdfMode.Auto),
    @SettingsValue
    Progressive(demoResource(MR.strings.demo_sdf_progressive), HighlightSdfMode.Progressive),
}

@Serializable
enum class DemoHighlightSdfBackend(
    override val label: Resource<String>,
    internal val backend: HighlightSdfBackend,
) : SettingsResources {
    @SettingsValue Auto(demoResource(MR.strings.demo_sdf_auto), HighlightSdfBackend.Auto),
    @SettingsValue Cpu(demoResource(MR.strings.demo_sdf_cpu), HighlightSdfBackend.Cpu),
    @SettingsValue Gpu(demoResource(MR.strings.demo_sdf_gpu), HighlightSdfBackend.Gpu),
}

internal fun DemoPreferences.Effects.highlightSdfSettings(): HighlightSdfSettings =
    HighlightSdfSettings(highlightSdfMode.mode, highlightSdfBackend.backend)

object HighlightSdfModeLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_highlight_sdf_mode)
}

object HighlightSdfBackendLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_highlight_sdf_backend)
}

@Serializable
enum class DemoProgressiveBlurMode(override val label: Resource<String>) : SettingsResources {
    @SettingsValue Auto(demoResource(MR.strings.demo_progressive_auto)),
    @SettingsValue Full(demoResource(MR.strings.demo_progressive_full)),
    @SettingsValue Layered(demoResource(MR.strings.demo_progressive_layered)),
    @SettingsValue Alpha(demoResource(MR.strings.demo_progressive_alpha)),
}

internal fun DemoPreferences.Effects.progressiveBlurMode(): ProgressiveBlurMode =
    when (progressiveBlur) {
        DemoProgressiveBlurMode.Auto -> ProgressiveBlurMode.Auto
        DemoProgressiveBlurMode.Full -> ProgressiveBlurMode.Full
        DemoProgressiveBlurMode.Layered -> ProgressiveBlurMode.Layered(layeredBlurLevels)
        DemoProgressiveBlurMode.Alpha -> ProgressiveBlurMode.Alpha
    }

@Serializable
enum class ThemeMode(override val label: Resource<String>) : SettingsResources {
    @SettingsValue System(demoResource(MR.strings.demo_system_expressive_default)),
    @SettingsValue Light(demoResource(MR.strings.demo_light)),
    @SettingsValue Dark(demoResource(MR.strings.demo_dark)),
}

@Serializable
enum class MaterialPreset(override val label: Resource<String>) : SettingsResources {
    @SettingsValue Regular(demoResource(MR.strings.demo_glass_regular)),
    @SettingsValue Blur(demoResource(MR.strings.demo_material_blur)),
    @SettingsValue Clear(demoResource(MR.strings.demo_glass_clear)),
}

@Serializable
enum class ShapePreset(override val label: Resource<String>) : SettingsResources {
    @SettingsValue Capsule(demoResource(MR.strings.demo_capsule)),
    @SettingsValue Material(demoResource(MR.strings.demo_material_shapes)),
}

@Serializable
enum class BarVariant(override val label: Resource<String>) : SettingsResources {
    @SettingsValue Small(demoResource(MR.strings.demo_single_row)),
    @SettingsValue Centered(demoResource(MR.strings.demo_centered)),
    @SettingsValue Medium(demoResource(MR.strings.demo_medium)),
    @SettingsValue Large(demoResource(MR.strings.demo_large)),
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
enum class PreviewBackground(override val label: Resource<String>) : SettingsResources {
    @SettingsValue Artwork(demoResource(MR.strings.demo_colorful)),
    @JsonNames("Light", "Dark", "Text")
    @SettingsValue
    Theme(demoResource(MR.strings.demo_theme_color)),
}

@Serializable
enum class UiPalette(override val label: Resource<String>) : SettingsResources {
    @SettingsValue Theme(demoResource(MR.strings.demo_theme_color)),
    @SettingsValue Colorful(demoResource(MR.strings.demo_colorful)),
}

object UiPaletteLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_ui_palette)
}

object AppearanceLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_appearance)
}

object RuntimeLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_compose_runtime)
}

object LinkComposerLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_link_composer)
    override val description: Resource<String?> =
        demoResource(MR.strings.demo_link_composer_description)
}

object EffectsLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_visual_effects)
}

object AccessibilityLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_accessibility)
}

object ThemeLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_color_theme)
}

object PresetLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_material_style)
}

object ShapeLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_shape_family)
}

object BarLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_top_app_bar)
}

object FontScaleLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_text_scale)
}

object BackgroundLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_background)
}

object BlurAnimationLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_blur_animation)
}

object BlurLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_blur)
}

object LensLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_refraction)
}

object VibrancyLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_vibrancy)
}

object HighlightLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_edge_highlights)
}

object HighlightTypeLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_highlight_type)
}

object ShadowLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_shadows)
}

object InnerShadowLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_inner_shadows)
}

object AdaptationLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_background_adaptation)
    override val description: Resource<String?> =
        demoResource(MR.strings.demo_background_adaptation_description)
}

object AdaptiveContentColorLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_adaptive_foreground_color)
    override val description: Resource<String?> =
        demoResource(
            MR.strings.demo_adaptive_foreground_description,
        )
}

object CustomBlurRadiiLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_override_preset_blur_radii)
    override val description: Resource<String?> =
        demoResource(
            MR.strings.demo_surface_blur_override_description,
        )
}

object ControlBlurRadiusLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_control_blur_radius_dp)
}

object PanelBlurRadiusLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_panel_blur_radius_dp)
}

object ScrollEdgeBlurRadiusLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_scroll_edge_blur_radius_dp)
}

object InteractionLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_shared_interaction_light)
}

object HdrOutputLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_hdr_output_android)
    override val description: Resource<String?> =
        demoResource(
            MR.strings.demo_hdr_output_android_description,
        )
}

object InteractionHdrLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_hdr_interaction_light)
    override val description: Resource<String?> =
        demoResource(
            MR.strings.demo_hdr_shared_light_description,
        )
}

object HdrHeadroomLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_hdr_headroom_ratio)
    override val description: Resource<String?> =
        demoResource(
            MR.strings.demo_hdr_headroom_ratio_description,
        )
}

object HdrRefreshIntervalLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_hdr_refresh_interval_ms)
    override val description: Resource<String?> =
        demoResource(
            MR.strings.demo_hdr_refresh_interval_description,
        )
}

object ScrollEdgePlateauLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_scroll_edge_plateau_dp)
}

object ScrollEdgeFadeLabel : SettingsResources {
    override val label: Resource<String> =
        demoResource(MR.strings.demo_scroll_edge_fade_below_the_bar_dp)
}

object ReduceTransparencyLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_reduce_transparency)
}

object IncreaseContrastLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_increase_contrast)
}

object ReduceMotionLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_reduce_motion)
    override val description: Resource<String?> =
        demoResource(MR.strings.demo_reduce_motion_description)
}

object LiveSlidersLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_live_slider_updates)
}

object ProgressiveBlurLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_progressive_blur)
    override val description: Resource<String> =
        demoResource(MR.strings.demo_progressive_blur_description)
}

object LayeredBlurLevelsLabel : SettingsResources {
    override val label: Resource<String> = demoResource(MR.strings.demo_layered_blur_levels)
    override val description: Resource<String> =
        demoResource(MR.strings.demo_layered_blur_levels_description)
}
