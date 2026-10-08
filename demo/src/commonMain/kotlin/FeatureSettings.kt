package top.ltfan.m3x.demo

import kotlinx.serialization.Serializable
import top.ltfan.m3x.datastore.annotation.Store
import top.ltfan.m3x.demo.generated.resources.Res
import top.ltfan.m3x.demo.generated.resources.demo_accent
import top.ltfan.m3x.demo.generated.resources.demo_appearance_and_scale
import top.ltfan.m3x.demo.generated.resources.demo_balanced
import top.ltfan.m3x.demo.generated.resources.demo_channel
import top.ltfan.m3x.demo.generated.resources.demo_display
import top.ltfan.m3x.demo.generated.resources.demo_editor_demo_only_these_settings_do_not_affect_the_app
import top.ltfan.m3x.demo.generated.resources.demo_enable_dependent_profile_options_in_this_editor_demonstration
import top.ltfan.m3x.demo.generated.resources.demo_enable_profile
import top.ltfan.m3x.demo.generated.resources.demo_features
import top.ltfan.m3x.demo.generated.resources.demo_focused
import top.ltfan.m3x.demo.generated.resources.demo_general
import top.ltfan.m3x.demo.generated.resources.demo_interface_scale
import top.ltfan.m3x.demo.generated.resources.demo_mode
import top.ltfan.m3x.demo.generated.resources.demo_notifications
import top.ltfan.m3x.demo.generated.resources.demo_preferred_range
import top.ltfan.m3x.demo.generated.resources.demo_profile_name
import top.ltfan.m3x.demo.generated.resources.demo_quiet
import top.ltfan.m3x.demo.generated.resources.demo_retry_count
import top.ltfan.m3x.demo.generated.resources.demo_sync
import top.ltfan.m3x.demo.generated.resources.demo_visible_rows
import top.ltfan.m3x.resource.Resource
import top.ltfan.m3x.settingspage.RangeBasedNumberInfo
import top.ltfan.m3x.settingspage.RangeBasedNumberInfo.Type.Slider
import top.ltfan.m3x.settingspage.SettingsResetMode
import top.ltfan.m3x.settingspage.SettingsResources
import top.ltfan.m3x.settingspage.SliderMode
import top.ltfan.m3x.settingspage.annotation.CustomSettingsItem
import top.ltfan.m3x.settingspage.annotation.FiniteFloat
import top.ltfan.m3x.settingspage.annotation.FiniteNumber
import top.ltfan.m3x.settingspage.annotation.SettingsDependencyType
import top.ltfan.m3x.settingspage.annotation.SettingsItem
import top.ltfan.m3x.settingspage.annotation.SettingsItemType
import top.ltfan.m3x.settingspage.annotation.SettingsStore
import top.ltfan.m3x.settingspage.annotation.SettingsValue
import top.ltfan.m3x.settingspage.annotation.TextInput

@Serializable
@SettingsStore(resetMode = SettingsResetMode.Menu)
@Store(name = "feature-catalog", codec = DemoJsonCodec::class)
data class FeatureSettings(
    @SettingsItem(GeneralResources::class, type = SettingsItemType.Category)
    val general: General = General(),
    @SettingsItem(DisplayResources::class, type = SettingsItemType.Category)
    val display: Display = Display(),
) {
    @Serializable
    data class General(
        @SettingsItem(EnabledResources::class) val enabled: Boolean = true,
        @SettingsItem(NameResources::class, dependsOn = ["enabled"])
        @TextInput(minLength = 2, maxLength = 40)
        val name: String = "Example profile",
        @SettingsItem(ModeResources::class) val mode: Mode = Mode.Balanced,
        @SettingsItem(RetryResources::class) val retryCount: Int = 3,
        @SettingsItem(
            FeaturesResources::class,
            dependsOn = ["enabled"],
            dependencyType = SettingsDependencyType.ShowHide,
        )
        val features: Set<Feature> = setOf(Feature.Sync),
    )

    @Serializable
    data class Display(
        @SettingsItem(ScaleResources::class)
        @FiniteFloat(Slider, 0.5f, 2f, 0.1f, sliderMode = SliderMode.Steps)
        val scale: Float = 1f,
        @SettingsItem(RowsResources::class) @FiniteNumber<Int>(RowsBounds::class) val rows: Int = 4,
        @SettingsItem(RangeResources::class)
        @FiniteFloat(Slider, 0f, 1f, 0.05f)
        @FiniteFloat(Slider, 0f, 1f, 0.05f)
        val range: Pair<Float, Float> = 0.25f to 0.75f,
        @SettingsItem(AccentResources::class)
        @CustomSettingsItem(rendererId = "accent")
        val accent: Accent = Accent.Blue,
    )
}

@Serializable
enum class Mode(override val label: Resource<String>) : SettingsResources {
    @SettingsValue Balanced(demoResource(Res.string.demo_balanced)),
    @SettingsValue Focused(demoResource(Res.string.demo_focused)),
    @SettingsValue Quiet(demoResource(Res.string.demo_quiet)),
}

@Serializable
sealed interface Feature {
    @SettingsValue
    @Serializable
    data object Sync : Feature, SettingsResources {
        override val label: Resource<String> = demoResource(Res.string.demo_sync)
    }

    @SettingsValue
    @Serializable
    data class Notifications(
        @SettingsItem(ChannelResources::class)
        @TextInput(minLength = 2, maxLength = 20)
        val channel: String = "Primary",
    ) : Feature {
        companion object : SettingsResources {
            override val label: Resource<String> = demoResource(Res.string.demo_notifications)
        }
    }
}

@Serializable
enum class Accent {
    Blue,
    Mint,
    Amber,
}

object RowsBounds : RangeBasedNumberInfo<Int> {
    override val type: RangeBasedNumberInfo.Type = RangeBasedNumberInfo.Type.Wheel
    override val min: Int = 1
    override val max: Int = 12
    override val step: Int = 1
    override val allowInput: Boolean = false
}

object GeneralResources : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_general)
    override val description: Resource<String?> =
        demoResource(Res.string.demo_editor_demo_only_these_settings_do_not_affect_the_app)
}

object DisplayResources : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_display)
    override val description: Resource<String?> = demoResource(Res.string.demo_appearance_and_scale)
}

object EnabledResources : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_enable_profile)
    override val description: Resource<String?> =
        demoResource(Res.string.demo_enable_dependent_profile_options_in_this_editor_demonstration)
}

object NameResources : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_profile_name)
}

object ModeResources : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_mode)
}

object FeaturesResources : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_features)
}

object ScaleResources : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_interface_scale)
}

object RetryResources : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_retry_count)
}

object RowsResources : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_visible_rows)
}

object ChannelResources : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_channel)
}

object RangeResources : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_preferred_range)
}

object AccentResources : SettingsResources {
    override val label: Resource<String> = demoResource(Res.string.demo_accent)
}
