package top.ltfan.m3x.datastore.processor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import top.ltfan.m3x.datastore.processor.util.derivedDefaultName
import top.ltfan.m3x.datastore.processor.util.resolveStoreName
import top.ltfan.m3x.datastore.processor.util.toSnakeCase

class NamesTest {
    @Test
    fun snakeCase() {
        assertEquals("app_list_settings", "AppListSettings".toSnakeCase())
        assertEquals("ui_settings_model", "UiSettingsModel".toSnakeCase())
        assertEquals("global_preferences", "GlobalPreferences".toSnakeCase())
        assertEquals("html_parser", "HTMLParser".toSnakeCase())
        assertEquals("app_v2_settings", "AppV2Settings".toSnakeCase())
        assertEquals("already_snake", "already_snake".toSnakeCase())
    }

    @Test
    fun storeName() {
        assertEquals("global_preferences", resolveStoreName("GlobalPreferences", ""))
        assertEquals("ui_settings", resolveStoreName("UiSettingsModel", "ui_settings"))
        assertEquals("app_list_settings", resolveStoreName("AppListSettings", ""))
    }

    @Test
    fun derivedNames() {
        assertEquals("blurEnabled", derivedDefaultName("withBlurEnabled"))
        assertEquals("blur", derivedDefaultName("withBlur"))
        assertNull(derivedDefaultName("blur"))
        assertNull(derivedDefaultName("with"))
    }
}
