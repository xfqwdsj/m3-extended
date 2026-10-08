package top.ltfan.m3x.demo

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import top.ltfan.backdrop.highlight.HighlightStyle
import top.ltfan.m3x.datastore.annotation.Store
import top.ltfan.m3x.demo.generated.resources.Res
import top.ltfan.m3x.demo.generated.resources.demo_accessibility
import top.ltfan.m3x.demo.generated.resources.demo_adapt_surface_appearance_to_the_local_background
import top.ltfan.m3x.demo.generated.resources.demo_adaptive_foreground_color
import top.ltfan.m3x.demo.generated.resources.demo_ambient
import top.ltfan.m3x.demo.generated.resources.demo_appearance
import top.ltfan.m3x.demo.generated.resources.demo_backdrop_color_amount
import top.ltfan.m3x.demo.generated.resources.demo_background
import top.ltfan.m3x.demo.generated.resources.demo_background_adaptation
import top.ltfan.m3x.demo.generated.resources.demo_blur
import top.ltfan.m3x.demo.generated.resources.demo_blur_animation
import top.ltfan.m3x.demo.generated.resources.demo_capsule
import top.ltfan.m3x.demo.generated.resources.demo_centered
import top.ltfan.m3x.demo.generated.resources.demo_choose_foreground_color_from_local_sdr_background_samples_when_adaptation_is_enabled
import top.ltfan.m3x.demo.generated.resources.demo_color_theme
import top.ltfan.m3x.demo.generated.resources.demo_colorful
import top.ltfan.m3x.demo.generated.resources.demo_compose_runtime
import top.ltfan.m3x.demo.generated.resources.demo_control_blur_radius_dp
import top.ltfan.m3x.demo.generated.resources.demo_dark
import top.ltfan.m3x.demo.generated.resources.demo_default
import top.ltfan.m3x.demo.generated.resources.demo_disabled
import top.ltfan.m3x.demo.generated.resources.demo_draw_the_shared_press_and_pointer_light_above_sdr_white_when_hdr_is_active
import top.ltfan.m3x.demo.generated.resources.demo_edge_highlights
import top.ltfan.m3x.demo.generated.resources.demo_enabled
import top.ltfan.m3x.demo.generated.resources.demo_follow_backdrop_color
import top.ltfan.m3x.demo.generated.resources.demo_follow_system
import top.ltfan.m3x.demo.generated.resources.demo_glass_clear
import top.ltfan.m3x.demo.generated.resources.demo_glass_regular
import top.ltfan.m3x.demo.generated.resources.demo_hdr_headroom_ratio
import top.ltfan.m3x.demo.generated.resources.demo_hdr_interaction_light
import top.ltfan.m3x.demo.generated.resources.demo_hdr_output_android
import top.ltfan.m3x.demo.generated.resources.demo_hdr_refresh_interval_ms
import top.ltfan.m3x.demo.generated.resources.demo_highlight_type
import top.ltfan.m3x.demo.generated.resources.demo_increase_contrast
import top.ltfan.m3x.demo.generated.resources.demo_inner_shadows
import top.ltfan.m3x.demo.generated.resources.demo_large
import top.ltfan.m3x.demo.generated.resources.demo_light
import top.ltfan.m3x.demo.generated.resources.demo_link_composer
import top.ltfan.m3x.demo.generated.resources.demo_link_composer_description
import top.ltfan.m3x.demo.generated.resources.demo_live_slider_updates
import top.ltfan.m3x.demo.generated.resources.demo_material_blur
import top.ltfan.m3x.demo.generated.resources.demo_material_shapes
import top.ltfan.m3x.demo.generated.resources.demo_material_style
import top.ltfan.m3x.demo.generated.resources.demo_medium
import top.ltfan.m3x.demo.generated.resources.demo_minimum_interval_between_backdrop_resource_refreshes_after_display_headroom_changes_android_14_and_later
import top.ltfan.m3x.demo.generated.resources.demo_override_preset_blur_radii
import top.ltfan.m3x.demo.generated.resources.demo_panel_blur_radius_dp
import top.ltfan.m3x.demo.generated.resources.demo_plain
import top.ltfan.m3x.demo.generated.resources.demo_reduce_motion
import top.ltfan.m3x.demo.generated.resources.demo_reduce_motion_description
import top.ltfan.m3x.demo.generated.resources.demo_reduce_transparency
import top.ltfan.m3x.demo.generated.resources.demo_refraction
import top.ltfan.m3x.demo.generated.resources.demo_request_hdr_color_mode_for_the_android_app_window_the_display_must_support_hdr
import top.ltfan.m3x.demo.generated.resources.demo_requested_hdr_headroom_relative_to_sdr_white_android_15_and_later
import top.ltfan.m3x.demo.generated.resources.demo_scroll_edge_blur_radius_dp
import top.ltfan.m3x.demo.generated.resources.demo_scroll_edge_fade_below_the_bar_dp
import top.ltfan.m3x.demo.generated.resources.demo_scroll_edge_plateau_dp_of_the_bar_that_keeps_full_strength
import top.ltfan.m3x.demo.generated.resources.demo_shadows
import top.ltfan.m3x.demo.generated.resources.demo_shape_family
import top.ltfan.m3x.demo.generated.resources.demo_shared_interaction_light
import top.ltfan.m3x.demo.generated.resources.demo_single_row
import top.ltfan.m3x.demo.generated.resources.demo_surface_blur_radius_dp
import top.ltfan.m3x.demo.generated.resources.demo_system_expressive_default
import top.ltfan.m3x.demo.generated.resources.demo_text_scale
import top.ltfan.m3x.demo.generated.resources.demo_theme_color
import top.ltfan.m3x.demo.generated.resources.demo_top_app_bar
import top.ltfan.m3x.demo.generated.resources.demo_ui_palette
import top.ltfan.m3x.demo.generated.resources.demo_use_the_following_per_surface_blur_radii_instead_of_the_selected_material_preset
import top.ltfan.m3x.demo.generated.resources.demo_vibrancy
import top.ltfan.m3x.demo.generated.resources.demo_visual_effects
import top.ltfan.m3x.resource.Resource
import top.ltfan.m3x.settingspage.RangeBasedNumberInfo.Type.Slider
import top.ltfan.m3x.settingspage.SettingsResetMode
import top.ltfan.m3x.settingspage.SettingsResources
import top.ltfan.m3x.settingspage.annotation.FiniteFloat
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
        @SettingsItem(LensLabel::class) val lens: Boolean = true,
        @SettingsItem(VibrancyLabel::class) val vibrancy: Boolean = true,
        @SettingsItem(HighlightLabel::class) val highlight: Boolean = true,
        @SettingsItem(HighlightTypeLabel::class, dependsOn = ["highlight"])
        val highlightType: DemoHighlightType = DemoHighlightType.Default,
        @SettingsItem(FollowBackdropColorLabel::class) val followBackdropColor: Boolean = true,
        @SettingsItem(BackdropColorAmountLabel::class, dependsOn = ["followBackdropColor"])
        @FiniteFloat(Slider, 0.0f, 1.0f, 0.01f)
        val backdropColorAmount: Float = DemoBackdropDefaults.highlight.colorResponse.amount,
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
        @SettingsItem(SurfaceBlurRadiusLabel::class, dependsOn = ["customBlurRadii"])
        @FiniteFloat(Slider, 0.0f, 64.0f, 1.0f)
        val surfaceBlurRadiusDp: Float = DemoBackdropDefaults.surfaceBlurRadius,
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
    @SettingsValue System(demoResource(Res.string.demo_follow_system)),
    @SettingsValue Enabled(demoResource(Res.string.demo_enabled)),
    @SettingsValue Disabled(demoResource(Res.string.demo_disabled)),
}

@Serializable
enum class DemoHighlightType(override val label: Resource<String>) : SettingsResources {
    @SettingsValue Default(demoResource(Res.string.demo_default)),
    @SettingsValue Ambient(demoResource(Res.string.demo_ambient)),
    @SettingsValue Plain(demoResource(Res.string.demo_plain));

    fun style(color: Color? = null): HighlightStyle =
        when (this) {
            Default -> color?.let(HighlightStyle::Solid) ?: HighlightStyle.Solid
            Ambient -> color?.let(HighlightStyle::TwoTone) ?: HighlightStyle.TwoTone
            Plain -> color?.let(HighlightStyle::Solid) ?: HighlightStyle.Solid
        }
}

@Serializable
enum class ThemeMode(override val label: Resource<String>) : SettingsResources {
    @SettingsValue System(demoResource(Res.string.demo_system_expressive_default)),
    @SettingsValue Light(demoResource(Res.string.demo_light)),
    @SettingsValue Dark(demoResource(Res.string.demo_dark)),
}

@Serializable
enum class MaterialPreset(override val label: Resource<String>) : SettingsResources {
    @SettingsValue Regular(demoResource(Res.string.demo_glass_regular)),
    @SettingsValue Blur(demoResource(Res.string.demo_material_blur)),
    @SettingsValue Clear(demoResource(Res.string.demo_glass_clear)),
}

@Serializable
enum class ShapePreset(override val label: Resource<String>) : SettingsResources {
    @SettingsValue Capsule(demoResource(Res.string.demo_capsule)),
    @SettingsValue Material(demoResource(Res.string.demo_material_shapes)),
}

@Serializable
enum class BarVariant(override val label: Resource<String>) : SettingsResources {
    @SettingsValue Small(demoResource(Res.string.demo_single_row)),
    @SettingsValue Centered(demoResource(Res.string.demo_centered)),
    @SettingsValue Medium(demoResource(Res.string.demo_medium)),
    @SettingsValue Large(demoResource(Res.string.demo_large)),
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
enum class PreviewBackground(override val label: Resource<String>) : SettingsResources {
    @SettingsValue Artwork(demoResource(Res.string.demo_colorful)),
    @JsonNames("Light", "Dark", "Text")
    @SettingsValue
    Theme(demoResource(Res.string.demo_theme_color)),
}

@Serializable
enum class UiPalette(override val label: Resource<String>) : SettingsResources {
    @SettingsValue Theme(demoResource(Res.string.demo_theme_color)),
    @SettingsValue Colorful(demoResource(Res.string.demo_colorful)),
}

object UiPaletteLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_ui_palette)
}

object AppearanceLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_appearance)
}

object RuntimeLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_compose_runtime)
}

object LinkComposerLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_link_composer)
    override val description: Resource<String?> =
        demoResource(Res.string.demo_link_composer_description)
}

object EffectsLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_visual_effects)
}

object AccessibilityLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_accessibility)
}

object ThemeLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_color_theme)
}

object PresetLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_material_style)
}

object ShapeLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_shape_family)
}

object BarLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_top_app_bar)
}

object FontScaleLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_text_scale)
}

object BackgroundLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_background)
}

object BlurAnimationLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_blur_animation)
}

object BlurLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_blur)
}

object LensLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_refraction)
}

object VibrancyLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_vibrancy)
}

object HighlightLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_edge_highlights)
}

object HighlightTypeLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_highlight_type)
}

object FollowBackdropColorLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_follow_backdrop_color)
}

object BackdropColorAmountLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_backdrop_color_amount)
}

object ShadowLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_shadows)
}

object InnerShadowLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_inner_shadows)
}

object AdaptationLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_background_adaptation)
    override val description: Resource<String?> =
        demoResource(Res.string.demo_adapt_surface_appearance_to_the_local_background)
}

object AdaptiveContentColorLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_adaptive_foreground_color)
    override val description: Resource<String?> =
        demoResource(
            Res.string
                .demo_choose_foreground_color_from_local_sdr_background_samples_when_adaptation_is_enabled,
        )
}

object CustomBlurRadiiLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_override_preset_blur_radii)
    override val description: Resource<String?> =
        demoResource(
            Res.string
                .demo_use_the_following_per_surface_blur_radii_instead_of_the_selected_material_preset,
        )
}

object ControlBlurRadiusLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_control_blur_radius_dp)
}

object PanelBlurRadiusLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_panel_blur_radius_dp)
}

object SurfaceBlurRadiusLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_surface_blur_radius_dp)
}

object ScrollEdgeBlurRadiusLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_scroll_edge_blur_radius_dp)
}

object InteractionLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_shared_interaction_light)
}

object HdrOutputLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_hdr_output_android)
    override val description: Resource<String?> =
        demoResource(
            Res.string
                .demo_request_hdr_color_mode_for_the_android_app_window_the_display_must_support_hdr,
        )
}

object InteractionHdrLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_hdr_interaction_light)
    override val description: Resource<String?> =
        demoResource(
            Res.string
                .demo_draw_the_shared_press_and_pointer_light_above_sdr_white_when_hdr_is_active,
        )
}

object HdrHeadroomLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_hdr_headroom_ratio)
    override val description: Resource<String?> =
        demoResource(
            Res.string.demo_requested_hdr_headroom_relative_to_sdr_white_android_15_and_later,
        )
}

object HdrRefreshIntervalLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_hdr_refresh_interval_ms)
    override val description: Resource<String?> =
        demoResource(
            Res.string
                .demo_minimum_interval_between_backdrop_resource_refreshes_after_display_headroom_changes_android_14_and_later,
        )
}

object ScrollEdgePlateauLabel : SettingsResources {
    override val label: Resource<String> =
        demoResource(Res.string.demo_scroll_edge_plateau_dp_of_the_bar_that_keeps_full_strength)
}

object ScrollEdgeFadeLabel : SettingsResources {
    override val label: Resource<String> =
        demoResource(Res.string.demo_scroll_edge_fade_below_the_bar_dp)
}

object ReduceTransparencyLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_reduce_transparency)
}

object IncreaseContrastLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_increase_contrast)
}

object ReduceMotionLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_reduce_motion)
    override val description: Resource<String?> =
        demoResource(Res.string.demo_reduce_motion_description)
}

object LiveSlidersLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_live_slider_updates)
}
