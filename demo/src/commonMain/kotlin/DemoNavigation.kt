package top.ltfan.m3x.demo

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import top.ltfan.m3x.demo.generated.resources.Res
import top.ltfan.m3x.demo.generated.resources.demo_backdrop_lab
import top.ltfan.m3x.demo.generated.resources.demo_components
import top.ltfan.m3x.demo.generated.resources.demo_home
import top.ltfan.m3x.demo.generated.resources.demo_lab
import top.ltfan.m3x.demo.generated.resources.demo_overlays
import top.ltfan.m3x.demo.generated.resources.demo_settings
import top.ltfan.m3x.demo.generated.resources.demo_settings_catalog

@Serializable
sealed interface DemoRoute : NavKey {
    @Serializable data object Components : DemoRoute

    @Serializable data object Overlays : DemoRoute

    @Serializable data object Lab : DemoRoute

    @Serializable data object Settings : DemoRoute

    @Serializable data object SettingsCatalog : DemoRoute
}

internal val DemoNavigationConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(DemoRoute.Components::class, DemoRoute.Components.serializer())
            subclass(DemoRoute.Overlays::class, DemoRoute.Overlays.serializer())
            subclass(DemoRoute.Lab::class, DemoRoute.Lab.serializer())
            subclass(DemoRoute.Settings::class, DemoRoute.Settings.serializer())
            subclass(DemoRoute.SettingsCatalog::class, DemoRoute.SettingsCatalog.serializer())
        }
    }
}

internal val DemoTabs: List<DemoRoute> =
    listOf(DemoRoute.Components, DemoRoute.Overlays, DemoRoute.Lab, DemoRoute.Settings)

@Composable
internal fun DemoRoute.title(): String =
    when (this) {
        DemoRoute.Components -> demoText(Res.string.demo_components)
        DemoRoute.Overlays -> demoText(Res.string.demo_overlays)
        DemoRoute.Lab -> demoText(Res.string.demo_backdrop_lab)
        DemoRoute.Settings -> demoText(Res.string.demo_settings)
        DemoRoute.SettingsCatalog -> demoText(Res.string.demo_settings_catalog)
    }

/**
 * Label for the floating bottom bar, which sizes itself to the tabs it draws. A phone-width window
 * cannot fit the full page titles.
 */
@Composable
internal fun DemoRoute.tabTitle(): String =
    when (this) {
        DemoRoute.Components -> demoText(Res.string.demo_home)
        DemoRoute.Overlays -> demoText(Res.string.demo_overlays)
        DemoRoute.Lab -> demoText(Res.string.demo_lab)
        DemoRoute.Settings,
        DemoRoute.SettingsCatalog -> demoText(Res.string.demo_settings)
    }
