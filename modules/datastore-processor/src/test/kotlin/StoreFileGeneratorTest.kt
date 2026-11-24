package top.ltfan.m3x.datastore.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import top.ltfan.m3x.datastore.processor.generator.DerivedModel
import top.ltfan.m3x.datastore.processor.generator.PropertyModel
import top.ltfan.m3x.datastore.processor.generator.StoreFileGenerator
import top.ltfan.m3x.datastore.processor.generator.StoreModel

class StoreFileGeneratorTest {
    private val generator = StoreFileGenerator()

    @Test
    fun definitionAndConstructorDelegates() {
        val text =
            generator
                .generate(
                    globalPreferencesModel(
                        properties = listOf(PropertyModel("useGlobalPreferences", BOOLEAN)),
                    ),
                )
                .toString()

        assertContains(
            text,
            "public object GlobalPreferencesStore : StoreDefinition<GlobalPreferences>",
        )
        assertContains(text, "override val name: String = \"global_preferences\"")
        assertContains(text, "override val defaultValue: GlobalPreferences = GlobalPreferences()")
        assertContains(text, "override val codec: StoreCodec<GlobalPreferences>")
        assertContains(text, "AppCodec.create(GlobalPreferences.serializer())")
        assertContains(text, "public val MutableStore<GlobalPreferences>.useGlobalPreferences:")
        assertContains(text, "ReadWriteProperty<Any?, Boolean>")
        assertContains(text, "it.useGlobalPreferences")
        assertContains(text, "settings.copy(useGlobalPreferences = value)")
    }

    @Test
    fun explicitStoreName() {
        val text =
            generator
                .generate(
                    globalPreferencesModel(
                        name = "ui_settings",
                        className = ClassName("com.example", "UiSettingsModel"),
                    ),
                )
                .toString()

        assertContains(text, "override val name: String = \"ui_settings\"")
    }

    @Test
    fun readOnlyDerived() {
        val sources = ClassName("com.example", "DownloadSource")
        val listOfSources = ClassName("kotlin.collections", "List").parameterizedBy(sources)
        val text =
            generator
                .generate(
                    globalPreferencesModel(
                        className = ClassName("com.example", "DownloadSourceOrder"),
                        derivations =
                            listOf(
                                DerivedModel.ReadOnly(
                                    "orderedSources",
                                    listOfSources,
                                    "orderedSources",
                                ),
                            ),
                    ),
                )
                .toString()

        assertContains(
            text,
            "public val MutableStore<DownloadSourceOrder>.orderedSources: List<DownloadSource>",
        )
        assertContains(text, "value.orderedSources")
        assertFalse(text.contains("var MutableStore<DownloadSourceOrder>.orderedSources"))
    }

    @Test
    fun writableDerived() {
        val text =
            generator
                .generate(
                    globalPreferencesModel(
                        className = ClassName("com.example", "UiSettingsModel"),
                        derivations =
                            listOf(
                                DerivedModel.Writable(
                                    "blurEnabled",
                                    BOOLEAN,
                                    getter = "blur",
                                    function = "withBlurEnabled",
                                ),
                            ),
                    ),
                )
                .toString()

        assertContains(text, "public var MutableStore<UiSettingsModel>.blurEnabled: Boolean")
        assertContains(text, "value.blur")
        assertContains(text, "update { current -> current.withBlurEnabled(v) }")
    }

    @Test
    fun renamedDerivedWithExplicitGetter() {
        val text =
            generator
                .generate(
                    globalPreferencesModel(
                        className = ClassName("com.example", "UiSettingsModel"),
                        derivations =
                            listOf(
                                DerivedModel.Writable(
                                    "blurEnabled",
                                    BOOLEAN,
                                    getter = "blur",
                                    function = "withBlurEnabled",
                                ),
                            ),
                    ),
                )
                .toString()

        assertContains(text, "MutableStore<UiSettingsModel>.blurEnabled")
        assertContains(text, "value.blur")
    }

    @Test
    fun providerDerived() {
        val derivations = ClassName("com.example", "UiSettingsDerivations")
        val text =
            generator
                .generate(
                    globalPreferencesModel(
                        className = ClassName("com.example", "UiSettingsModel"),
                        derivations =
                            listOf(
                                DerivedModel.ReadOnly(
                                    name = "effectiveMotion",
                                    type = BOOLEAN,
                                    getter = "effectiveMotion",
                                    owner = derivations,
                                ),
                            ),
                    ),
                )
                .toString()

        assertContains(text, "public val MutableStore<UiSettingsModel>.effectiveMotion: Boolean")
        assertContains(text, "with(UiSettingsDerivations) { value.effectiveMotion }")
    }

    @Test
    fun settingsDescriberIsGeneratedBySettingsProcessor() {
        val withSettings =
            generator.generate(globalPreferencesModel(hasSettingsStore = true)).toString()
        assertFalse(withSettings.contains("settingsDescriber"))
    }

    private fun globalPreferencesModel(
        className: ClassName = ClassName("com.example", "GlobalPreferences"),
        name: String = "global_preferences",
        properties: List<PropertyModel> = emptyList(),
        derivations: List<DerivedModel> = emptyList(),
        hasSettingsStore: Boolean = false,
    ) =
        StoreModel(
            className = className,
            storeName = name,
            codec = ClassName("com.example", "AppCodec"),
            codecFunction = "create",
            properties = properties,
            derivations = derivations,
            hasSettingsStore = hasSettingsStore,
        )

    private companion object {
        val BOOLEAN = ClassName("kotlin", "Boolean")
    }
}
