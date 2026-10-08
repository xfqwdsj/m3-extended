package top.ltfan.m3x.demo

import kotlinx.serialization.Serializable
import top.ltfan.backdrop.BackdropCurve
import top.ltfan.m3x.datastore.annotation.Store
import top.ltfan.m3x.demo.generated.resources.Res
import top.ltfan.m3x.demo.generated.resources.demo_backdrop_color_amount
import top.ltfan.m3x.demo.generated.resources.demo_background_adaptation
import top.ltfan.m3x.demo.generated.resources.demo_blur_radius
import top.ltfan.m3x.demo.generated.resources.demo_brightness
import top.ltfan.m3x.demo.generated.resources.demo_chromatic_aberration
import top.ltfan.m3x.demo.generated.resources.demo_contrast
import top.ltfan.m3x.demo.generated.resources.demo_custom_directional_strength_curve
import top.ltfan.m3x.demo.generated.resources.demo_custom_directional_width_curve
import top.ltfan.m3x.demo.generated.resources.demo_depth_effect
import top.ltfan.m3x.demo.generated.resources.demo_dimming_opacity
import top.ltfan.m3x.demo.generated.resources.demo_draw_a_striped_extended_range_patch_in_the_preview_enable_hdr_output_in_appearance_to_see_it_above_sdr_white
import top.ltfan.m3x.demo.generated.resources.demo_edge
import top.ltfan.m3x.demo.generated.resources.demo_edge_highlight
import top.ltfan.m3x.demo.generated.resources.demo_extend_the_preview_s_press_and_pointer_light_above_sdr_white
import top.ltfan.m3x.demo.generated.resources.demo_follow_backdrop_color
import top.ltfan.m3x.demo.generated.resources.demo_hdr_background_patch
import top.ltfan.m3x.demo.generated.resources.demo_hdr_contour_light
import top.ltfan.m3x.demo.generated.resources.demo_hdr_interaction_light
import top.ltfan.m3x.demo.generated.resources.demo_highlight_angle
import top.ltfan.m3x.demo.generated.resources.demo_highlight_falloff
import top.ltfan.m3x.demo.generated.resources.demo_highlight_opacity
import top.ltfan.m3x.demo.generated.resources.demo_highlight_strength_from
import top.ltfan.m3x.demo.generated.resources.demo_highlight_strength_to
import top.ltfan.m3x.demo.generated.resources.demo_highlight_type
import top.ltfan.m3x.demo.generated.resources.demo_highlight_width
import top.ltfan.m3x.demo.generated.resources.demo_highlight_width_falloff
import top.ltfan.m3x.demo.generated.resources.demo_highlight_width_from
import top.ltfan.m3x.demo.generated.resources.demo_highlight_width_to
import top.ltfan.m3x.demo.generated.resources.demo_inner_shadow
import top.ltfan.m3x.demo.generated.resources.demo_inner_shadow_opacity
import top.ltfan.m3x.demo.generated.resources.demo_inner_shadow_radius
import top.ltfan.m3x.demo.generated.resources.demo_interaction_contour_opacity
import top.ltfan.m3x.demo.generated.resources.demo_interior_light_opacity
import top.ltfan.m3x.demo.generated.resources.demo_light_radius
import top.ltfan.m3x.demo.generated.resources.demo_lighting
import top.ltfan.m3x.demo.generated.resources.demo_optics
import top.ltfan.m3x.demo.generated.resources.demo_refraction_amount
import top.ltfan.m3x.demo.generated.resources.demo_refraction_height
import top.ltfan.m3x.demo.generated.resources.demo_saturation
import top.ltfan.m3x.demo.generated.resources.demo_shadow
import top.ltfan.m3x.demo.generated.resources.demo_shadow_offset
import top.ltfan.m3x.demo.generated.resources.demo_shadow_opacity
import top.ltfan.m3x.demo.generated.resources.demo_shadow_radius
import top.ltfan.m3x.demo.generated.resources.demo_shadows
import top.ltfan.m3x.demo.generated.resources.demo_tint_opacity
import top.ltfan.m3x.demo.generated.resources.demo_use_extended_range_white_when_the_display_reports_hdr_headroom
import top.ltfan.m3x.demo.generated.resources.demo_vibrancy
import top.ltfan.m3x.resource.Resource
import top.ltfan.m3x.settingspage.RangeBasedNumberInfo
import top.ltfan.m3x.settingspage.SettingsResetMode
import top.ltfan.m3x.settingspage.SettingsResources
import top.ltfan.m3x.settingspage.annotation.FiniteFloat
import top.ltfan.m3x.settingspage.annotation.SettingsItem
import top.ltfan.m3x.settingspage.annotation.SettingsItemType
import top.ltfan.m3x.settingspage.annotation.SettingsStore

@Serializable
@SettingsStore(resetMode = SettingsResetMode.Menu)
@Store(name = "backdrop-lab", codec = DemoJsonCodec::class)
data class LabParameters(
    @SettingsItem(LabOpticsLabel::class, type = SettingsItemType.Category)
    val optics: Optics = Optics(),
    @SettingsItem(LabEdgeLabel::class, type = SettingsItemType.Category) val edge: Edge = Edge(),
    @SettingsItem(LabShadowsLabel::class, type = SettingsItemType.Category)
    val shadows: Shadows = Shadows(),
    @SettingsItem(LabLightingLabel::class, type = SettingsItemType.Category)
    val lighting: Lighting = Lighting(),
) {
    @Serializable
    data class Optics(
        @SettingsItem(LabBlurRadiusLabel::class)
        @FiniteFloat(RangeBasedNumberInfo.Type.Slider, 0.0f, 64.0f, 1.0f)
        val blurRadius: Float = DemoBackdropDefaults.BLUR_RADIUS,
        @SettingsItem(LabRefractionHeightLabel::class)
        @FiniteFloat(RangeBasedNumberInfo.Type.Slider, 0.0f, 48.0f, 1.0f)
        val refractionHeight: Float = DemoBackdropDefaults.REFRACTION_HEIGHT,
        @SettingsItem(LabRefractionAmountLabel::class)
        @FiniteFloat(RangeBasedNumberInfo.Type.Slider, 0.0f, 96.0f, 1.0f)
        val refractionAmount: Float = DemoBackdropDefaults.REFRACTION_AMOUNT,
        @SettingsItem(LabTintAlphaLabel::class)
        @FiniteFloat(RangeBasedNumberInfo.Type.Slider, 0.0f, 1.0f, 0.01f)
        val tintAlpha: Float = DemoBackdropDefaults.TINT_ALPHA,
        @SettingsItem(LabDimmingAlphaLabel::class)
        @FiniteFloat(RangeBasedNumberInfo.Type.Slider, 0.0f, 1.0f, 0.01f)
        val dimmingAlpha: Float = DemoBackdropDefaults.DIMMING_ALPHA,
        @SettingsItem(LabBrightnessLabel::class)
        @FiniteFloat(RangeBasedNumberInfo.Type.Slider, -1.0f, 1.0f, 0.01f)
        val brightness: Float = DemoBackdropDefaults.BRIGHTNESS,
        @SettingsItem(LabContrastLabel::class)
        @FiniteFloat(RangeBasedNumberInfo.Type.Slider, 0.0f, 2.0f, 0.01f)
        val contrast: Float = DemoBackdropDefaults.CONTRAST,
        @SettingsItem(LabSaturationLabel::class)
        @FiniteFloat(RangeBasedNumberInfo.Type.Slider, 0.0f, 2.0f, 0.01f)
        val saturation: Float = DemoBackdropDefaults.SATURATION,
        @SettingsItem(LabDepthEffectLabel::class) val depthEffect: Boolean = false,
        @SettingsItem(LabChromaticAberrationLabel::class) val chromaticAberration: Boolean = false,
        @SettingsItem(LabVibrancyLabel::class) val vibrancy: Boolean = true,
        @SettingsItem(LabAdaptiveLabel::class) val adaptive: Boolean = false,
        @SettingsItem(LabHdrBackgroundLabel::class) val hdrBackground: Boolean = false,
    )

    @Serializable
    data class Edge(
        @SettingsItem(LabHighlightWidthLabel::class)
        @FiniteFloat(RangeBasedNumberInfo.Type.Slider, 0.0f, 8.0f, 0.01f)
        val highlightWidth: Float = DemoBackdropDefaults.highlight.width.value,
        @SettingsItem(LabHighlightAlphaLabel::class)
        @FiniteFloat(RangeBasedNumberInfo.Type.Slider, 0.0f, 1.0f, 0.01f)
        val highlightAlpha: Float = DemoBackdropDefaults.highlight.opacity,
        @SettingsItem(LabHighlightAngleLabel::class)
        @FiniteFloat(RangeBasedNumberInfo.Type.Slider, 0.0f, 360.0f, 1.0f)
        val highlightAngle: Float = DemoBackdropDefaults.direction.angle,
        @SettingsItem(LabHighlightStrengthFromLabel::class, dependsOn = ["highlight"])
        @FiniteFloat(RangeBasedNumberInfo.Type.Slider, 0.0f, 2.0f, 0.01f)
        val highlightStrengthFrom: Float = DemoBackdropDefaults.direction.strength.from,
        @SettingsItem(LabHighlightStrengthToLabel::class, dependsOn = ["highlight"])
        @FiniteFloat(RangeBasedNumberInfo.Type.Slider, 0.0f, 2.0f, 0.01f)
        val highlightStrengthTo: Float = DemoBackdropDefaults.direction.strength.to,
        @SettingsItem(LabHighlightWidthFromLabel::class, dependsOn = ["highlight"])
        @FiniteFloat(RangeBasedNumberInfo.Type.Slider, 0.0f, 2.0f, 0.01f)
        val highlightWidthFrom: Float = DemoBackdropDefaults.direction.widthScale.from,
        @SettingsItem(LabHighlightWidthToLabel::class, dependsOn = ["highlight"])
        @FiniteFloat(RangeBasedNumberInfo.Type.Slider, 0.0f, 2.0f, 0.01f)
        val highlightWidthTo: Float = DemoBackdropDefaults.direction.widthScale.to,
        @SettingsItem(LabCustomHighlightStrengthCurveLabel::class, dependsOn = ["highlight"])
        val customHighlightStrengthCurve: Boolean = false,
        @SettingsItem(
            LabHighlightFalloffLabel::class,
            dependsOn = ["highlight", "customHighlightStrengthCurve"],
        )
        @FiniteFloat(RangeBasedNumberInfo.Type.Slider, 0.0f, 4.0f, 0.01f)
        val highlightFalloff: Float = DemoBackdropDefaults.HIGHLIGHT_FALLOFF,
        @SettingsItem(LabCustomHighlightWidthCurveLabel::class, dependsOn = ["highlight"])
        val customHighlightWidthCurve: Boolean = false,
        @SettingsItem(
            LabHighlightWidthFalloffLabel::class,
            dependsOn = ["highlight", "customHighlightWidthCurve"],
        )
        @FiniteFloat(RangeBasedNumberInfo.Type.Slider, 1.0f, 3.0f, 0.01f)
        val highlightWidthFalloff: Float = DemoBackdropDefaults.HIGHLIGHT_WIDTH_FALLOFF,
        @SettingsItem(LabHighlightLabel::class) val highlight: Boolean = true,
        @SettingsItem(LabHighlightTypeLabel::class, dependsOn = ["highlight"])
        val highlightType: DemoHighlightType = DemoHighlightType.Default,
        @SettingsItem(LabHdrHighlightLabel::class, dependsOn = ["highlight"])
        val hdrHighlight: Boolean = false,
        @SettingsItem(LabFollowBackdropColorLabel::class, dependsOn = ["highlight"])
        val followBackdropColor: Boolean = true,
        @SettingsItem(LabBackdropColorAmountLabel::class, dependsOn = ["followBackdropColor"])
        @FiniteFloat(RangeBasedNumberInfo.Type.Slider, 0.0f, 1.0f, 0.01f)
        val backdropColorAmount: Float = DemoBackdropDefaults.highlight.colorResponse.amount,
    )

    @Serializable
    data class Shadows(
        @SettingsItem(LabShadowRadiusLabel::class)
        @FiniteFloat(RangeBasedNumberInfo.Type.Slider, 0.0f, 64.0f, 1.0f)
        val shadowRadius: Float = DemoBackdropDefaults.SHADOW_RADIUS,
        @SettingsItem(LabShadowOpacityLabel::class)
        @FiniteFloat(RangeBasedNumberInfo.Type.Slider, 0.0f, 1.0f, 0.01f)
        val shadowOpacity: Float = DemoBackdropDefaults.SHADOW_OPACITY,
        @SettingsItem(LabShadowOffsetLabel::class)
        @FiniteFloat(RangeBasedNumberInfo.Type.Slider, -32.0f, 32.0f, 1.0f)
        val shadowOffset: Float = DemoBackdropDefaults.SHADOW_OFFSET,
        @SettingsItem(LabInnerRadiusLabel::class)
        @FiniteFloat(RangeBasedNumberInfo.Type.Slider, 0.0f, 64.0f, 1.0f)
        val innerRadius: Float = DemoBackdropDefaults.INNER_RADIUS,
        @SettingsItem(LabInnerOpacityLabel::class)
        @FiniteFloat(RangeBasedNumberInfo.Type.Slider, 0.0f, 1.0f, 0.01f)
        val innerOpacity: Float = DemoBackdropDefaults.INNER_OPACITY,
        @SettingsItem(LabShadowLabel::class) val shadow: Boolean = true,
        @SettingsItem(LabInnerShadowLabel::class) val innerShadow: Boolean = false,
    )

    @Serializable
    data class Lighting(
        @SettingsItem(LabLightRadiusLabel::class)
        @FiniteFloat(RangeBasedNumberInfo.Type.Slider, 40.0f, 400.0f, 1.0f)
        val lightRadius: Float = DemoBackdropDefaults.LIGHT_RADIUS,
        @SettingsItem(LabInteriorAlphaLabel::class)
        @FiniteFloat(RangeBasedNumberInfo.Type.Slider, 0.0f, 1.0f, 0.01f)
        val interiorAlpha: Float = DemoBackdropDefaults.INTERIOR_ALPHA,
        @SettingsItem(LabEdgeAlphaLabel::class)
        @FiniteFloat(RangeBasedNumberInfo.Type.Slider, 0.0f, 1.0f, 0.01f)
        val edgeAlpha: Float = DemoBackdropDefaults.EDGE_ALPHA,
        @SettingsItem(LabLightingLabel::class) val lighting: Boolean = true,
        @SettingsItem(LabHdrInteractionLabel::class, dependsOn = ["lighting"])
        val hdrInteraction: Boolean = false,
    )
}

internal fun highlightStrengthCurve(falloff: Float): BackdropCurve {
    val exponent = falloff.coerceIn(1f, 3f)
    return when {
        exponent == 1f -> BackdropCurve.Linear
        exponent <= 2f -> {
            val fraction = exponent - 1f
            BackdropCurve.CubicBezier(
                1f / 3f,
                (1f - fraction) / 3f,
                2f / 3f,
                (2f - fraction) / 3f,
            )
        }
        else -> BackdropCurve.CubicBezier(1f / 3f, 0f, 2f / 3f, (3f - exponent) / 3f)
    }
}

object LabBlurRadiusLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_blur_radius)
}

object LabRefractionHeightLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_refraction_height)
}

object LabRefractionAmountLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_refraction_amount)
}

object LabTintAlphaLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_tint_opacity)
}

object LabDimmingAlphaLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_dimming_opacity)
}

object LabBrightnessLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_brightness)
}

object LabContrastLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_contrast)
}

object LabSaturationLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_saturation)
}

object LabDepthEffectLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_depth_effect)
}

object LabChromaticAberrationLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_chromatic_aberration)
}

object LabVibrancyLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_vibrancy)
}

object LabAdaptiveLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_background_adaptation)
}

object LabHighlightWidthLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_highlight_width)
}

object LabHighlightAlphaLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_highlight_opacity)
}

object LabHighlightAngleLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_highlight_angle)
}

object LabHighlightStrengthFromLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_highlight_strength_from)
}

object LabHighlightStrengthToLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_highlight_strength_to)
}

object LabHighlightWidthFromLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_highlight_width_from)
}

object LabHighlightWidthToLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_highlight_width_to)
}

object LabCustomHighlightStrengthCurveLabel : SettingsResources {
    override val label: Resource<String> =
        demoResource(Res.string.demo_custom_directional_strength_curve)
}

object LabCustomHighlightWidthCurveLabel : SettingsResources {
    override val label: Resource<String> =
        demoResource(Res.string.demo_custom_directional_width_curve)
}

object LabHighlightFalloffLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_highlight_falloff)
}

object LabHighlightWidthFalloffLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_highlight_width_falloff)
}

object LabHighlightLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_edge_highlight)
}

object LabHighlightTypeLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_highlight_type)
}

object LabHdrHighlightLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_hdr_contour_light)
    override val description: Resource<String?> =
        demoResource(Res.string.demo_use_extended_range_white_when_the_display_reports_hdr_headroom)
}

object LabHdrInteractionLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_hdr_interaction_light)
    override val description: Resource<String?> =
        demoResource(Res.string.demo_extend_the_preview_s_press_and_pointer_light_above_sdr_white)
}

object LabHdrBackgroundLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_hdr_background_patch)
    override val description: Resource<String?> =
        demoResource(
            Res.string
                .demo_draw_a_striped_extended_range_patch_in_the_preview_enable_hdr_output_in_appearance_to_see_it_above_sdr_white,
        )
}

object LabFollowBackdropColorLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_follow_backdrop_color)
}

object LabBackdropColorAmountLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_backdrop_color_amount)
}

object LabShadowRadiusLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_shadow_radius)
}

object LabShadowOpacityLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_shadow_opacity)
}

object LabShadowOffsetLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_shadow_offset)
}

object LabInnerRadiusLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_inner_shadow_radius)
}

object LabInnerOpacityLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_inner_shadow_opacity)
}

object LabShadowLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_shadow)
}

object LabInnerShadowLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_inner_shadow)
}

object LabLightRadiusLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_light_radius)
}

object LabInteriorAlphaLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_interior_light_opacity)
}

object LabEdgeAlphaLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_interaction_contour_opacity)
}

object LabLightingLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_lighting)
}

object LabOpticsLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_optics)
}

object LabEdgeLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_edge)
}

object LabShadowsLabel : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_shadows)
}
