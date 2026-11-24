package top.ltfan.m3x.settingspage.processor

import com.google.devtools.ksp.impl.KotlinSymbolProcessing
import com.google.devtools.ksp.processing.KSPJvmConfig
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSNode
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.junit.Rule
import org.junit.rules.TemporaryFolder

/** Runs the actual processor against Kotlin source, including its source diagnostics. */
class EditableSettingsPropertyTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun bodyPropertiesAreRejectedThroughoutTheSettingsTree() {
        val source =
            """
            @SettingsStore
            data class BodyValue(val seed: Int = 0) {
                @SettingsItem(TestResources::class) val bodyValue: Int = 1
            }
            @SettingsStore
            data class BodyMutable(val seed: Int = 0) {
                @SettingsItem(TestResources::class) var bodyMutable: Int = 1
            }
            @SettingsStore
            data class Computed(val seed: Int = 0) {
                @SettingsItem(TestResources::class) val computed: Int get() = seed + 1
            }
            @SettingsStore
            data class Delegated(val seed: Int = 0) {
                @SettingsItem(TestResources::class) val delegated: Int by lazy { seed + 1 }
            }
            data class Category(@SettingsItem(TestResources::class) val count: Int = 0)
            @SettingsStore
            data class BodyCategory(val seed: Int = 0) {
                @SettingsItem(TestResources::class, type = SettingsItemType.Category)
                val category: Category = Category()
            }
            data class NestedBody(val seed: Int = 0) {
                @SettingsItem(TestResources::class) val nestedBody: Int = 0
            }
            @SettingsStore
            data class NestedStore(
                @SettingsItem(TestResources::class, type = SettingsItemType.Category)
                val nested: NestedBody = NestedBody(),
            )
            sealed interface SingleChoice {
                @SettingsValue
                data class Option(val seed: Int = 0) : SingleChoice {
                    @SettingsItem(TestResources::class) val singleBody: Int = 0
                    companion object : SettingsResources by TestResources
                }
            }
            @SettingsStore
            data class SingleStore(
                @SettingsItem(TestResources::class) val single: SingleChoice = SingleChoice.Option(),
            )
            sealed interface MultiChoice {
                @SettingsValue
                data class Option(val seed: Int = 0) : MultiChoice {
                    @SettingsItem(TestResources::class) val multiBody: Int = 0
                    companion object : SettingsResources by TestResources
                }
            }
            @SettingsStore
            data class MultiStore(
                @SettingsItem(TestResources::class) val multi: Set<MultiChoice> = emptySet(),
            )
            """
                .trimIndent()
        val result = process(source)
        assertEquals(KotlinSymbolProcessing.ExitCode.PROCESSING_ERROR, result.exitCode)
        val paths =
            mapOf(
                "bodyValue" to "bodyValue",
                "bodyMutable" to "bodyMutable",
                "computed" to "computed",
                "delegated" to "delegated",
                "category" to "category",
                "nestedBody" to "nested.nestedBody",
                "singleBody" to "single.Option.singleBody",
                "multiBody" to "multi.Option.multiBody",
            )
        assertEquals(
            paths.keys,
            result.errors.map { it.declaration }.toSet(),
            result.errors.toString(),
        )
        paths.forEach { (property, path) ->
            val error = result.errors.single { it.declaration == property }
            assertTrue(error.message.contains(path), error.message)
            assertTrue(error.message.contains("primary constructor"), error.message)
            assertTrue(error.location?.filePath?.endsWith("Models.kt") == true, error.toString())
            val line = result.source.lines()[error.location!!.lineNumber - 1]
            assertTrue(line.contains(property), "Diagnostic must point to the property: $line")
        }
        assertTrue(result.generated.isEmpty(), "Invalid models must not produce describers")
    }

    @Test
    fun inaccessiblePropertiesAndCopyFunctionsAreRejected() {
        val result =
            process(
                """
                @SettingsStore
                data class HiddenProperty(
                    @SettingsItem(TestResources::class) private val hidden: Int = 0,
                )
                @SettingsStore
                @ConsistentCopyVisibility
                data class PrivateCopy private constructor(
                    @SettingsItem(TestResources::class) val locked: Int = 0,
                )
                @SettingsStore
                data class DefaultCopy private constructor(
                    @SettingsItem(TestResources::class) val defaultLocked: Int = 0,
                )
                @SettingsStore
                @ExposedCopyVisibility
                data class ExposedCopy private constructor(
                    @SettingsItem(TestResources::class) val exposedLocked: Int = 0,
                )
                """
                    .trimIndent(),
            )
        assertEquals(KotlinSymbolProcessing.ExitCode.PROCESSING_ERROR, result.exitCode)
        assertEquals(
            setOf("hidden", "locked", "defaultLocked", "exposedLocked"),
            result.errors.map { it.declaration }.toSet(),
            result.errors.toString(),
        )
        assertTrue(
            result.errors.single { it.declaration == "hidden" }.message.contains("accessible"),
        )
        result.errors
            .filter { it.declaration != "hidden" }
            .forEach {
                assertTrue(it.message.contains("copy"), it.toString())
            }
        assertTrue(result.generated.isEmpty())
    }

    @Test
    fun constructorPropertiesRemainEditableAndUnannotatedBodyPropertiesAreAllowed() {
        val result =
            process(
                """
                data class Category(
                    @SettingsItem(TestResources::class) val count: Int = 0,
                    @SettingsItem(TestResources::class) var text: String = "value",
                    @SettingsItem(TestResources::class) val `when`: Boolean = false,
                )
                sealed interface Choice {
                    @SettingsValue
                    data class Option(
                        @SettingsItem(TestResources::class) val amount: Int = 0,
                    ) : Choice {
                        companion object : SettingsResources by TestResources
                    }
                }
                @SettingsStore
                data class ValidStore internal constructor(
                    @SettingsItem(TestResources::class, type = SettingsItemType.Category)
                    val category: Category = Category(),
                    @SettingsItem(TestResources::class) val single: Choice = Choice.Option(),
                    @SettingsItem(TestResources::class) val multi: Set<Choice> = emptySet(),
                ) {
                    val computed: Int get() = category.count + 1
                    var unannotated: Int = 0
                }
                """
                    .trimIndent(),
                compileGenerated = true,
            )
        assertEquals(KotlinSymbolProcessing.ExitCode.OK, result.exitCode, result.errors.toString())
        assertTrue(result.errors.isEmpty())
        val generated = result.generated.single().replace(Regex("\\s+"), " ")
        assertTrue(
            generated.contains("current.copy(category = current.category.copy(count = newValue))"),
            generated,
        )
        assertTrue(generated.contains("current.single.copy(amount = newValue)"), generated)
        assertTrue(generated.contains("it.copy(amount = newValue)"), generated)
        assertTrue(generated.contains("copy(`when` = newValue)"), generated)
        assertFalse(generated.contains("UnannotatedItem"))
        assertFalse(generated.contains("ComputedItem"))
    }

    @Test
    fun menuDefaultsGenerateFreshValuesForNestedAndChoiceItems() {
        val result =
            process(
                """
                @SettingsStore(resetMode = SettingsResetMode.Menu)
                data class MenuStore(
                    @SettingsItem(TestResources::class) val enabled: Boolean = false,
                    @SettingsItem(TestResources::class, type = SettingsItemType.Category)
                    val category: Category = Category(),
                    @SettingsItem(TestResources::class) val selected: Choice = Choice.Notifications("Specific choice"),
                    @SettingsItem(TestResources::class) val selectedMany: Set<Choice> =
                        setOf(Choice.Notifications("Specific multi")),
                )
                data class Category(
                    @SettingsItem(TestResources::class) val title: String = "Default title",
                )
                sealed interface Choice {
                    @SettingsValue
                    data class Notifications(
                        @SettingsItem(TestResources::class) val channel: String = "Primary",
                    ) : Choice {
                        companion object : SettingsResources by TestResources
                    }
                    @SettingsValue
                    data object Sync : Choice, SettingsResources by TestResources
                }
                @SettingsStore
                data class RequiredStore(
                    val constructorInput: String,
                    @SettingsItem(TestResources::class) val enabled: Boolean = false,
                )
                """
                    .trimIndent(),
                compileGenerated = true,
            )
        assertEquals(KotlinSymbolProcessing.ExitCode.OK, result.exitCode, result.errors.toString())
        assertTrue(result.errors.isEmpty())
        val menuSource = result.generated.first { "MenuStoreDescriber" in it }
        assertTrue(menuSource.contains("SettingsResetMode.Menu"), menuSource)
        assertTrue(menuSource.contains("MenuStore().category.title"), menuSource)
        assertTrue(menuSource.contains("describeValue(MenuStore().selected)"), menuSource)
        assertTrue(
            menuSource.contains("MenuStore().selectedMany.map { describeValue(it) }.toSet()"),
            menuSource,
        )
        assertTrue(
            menuSource.contains("MenuStore().selected as? Choice.Notifications"),
            menuSource,
        )
        assertTrue(menuSource.contains("Choice.Notifications()).channel"), menuSource)
        assertTrue(
            menuSource.contains(
                "MenuStore().selectedMany.filterIsInstance<Choice.Notifications>()",
            ),
            menuSource,
        )
        val requiredSource = result.generated.first { "RequiredStoreDescriber" in it }
        assertFalse(requiredSource.contains("RequiredStore()"), requiredSource)
    }

    @Test
    fun menuDefaultsRequireAnAccessibleDefaultConstructor() {
        val result =
            process(
                """
                @SettingsStore(resetMode = SettingsResetMode.Menu)
                data class RequiredDefault(
                    val required: String,
                    @SettingsItem(TestResources::class) val enabled: Boolean = false,
                )
                """
                    .trimIndent(),
            )
        assertEquals(KotlinSymbolProcessing.ExitCode.PROCESSING_ERROR, result.exitCode)
        assertTrue(
            result.errors.single().message.contains("accessible no-argument default constructor"),
        )
        assertTrue(result.generated.isEmpty())
    }

    @Test
    fun independentDescriptorsCompileWithoutDataStoreOnTheirConsumerClasspath() {
        val result =
            process(
                """
                @SettingsStore(bindDataStore = false)
                data class IndependentSettings(
                    @SettingsItem(TestResources::class) val enabled: Boolean = false,
                )
                """
                    .trimIndent(),
                compileGenerated = true,
                includeDataStore = false,
            )
        assertEquals(KotlinSymbolProcessing.ExitCode.OK, result.exitCode)
        assertTrue(result.errors.isEmpty())
        val source = result.generated.single()
        assertTrue(source.contains("SettingsModel<IndependentSettings>"))
        assertTrue(!source.contains("top.ltfan.m3x.datastore"))
        assertTrue(!source.contains("top.ltfan.m3x.settingspage.datastore"))
    }

    private fun process(
        models: String,
        compileGenerated: Boolean = false,
        includeDataStore: Boolean = true,
    ): Result {
        val directory = temporaryFolder.newFolder()
        val source =
            """
            package fixtures

            import top.ltfan.m3x.resource.Resource
            import top.ltfan.m3x.settingspage.SettingsResetMode
            import top.ltfan.m3x.settingspage.SettingsResources
            import top.ltfan.m3x.settingspage.annotation.*

            object TestResources : SettingsResources {
                override val label: Resource<String> = Resource.Global.Static("Test")
            }

            """
                .trimIndent() + "\n" + models
        val sourceFile = directory.resolve("Models.kt").apply { writeText(source) }
        val config =
            KSPJvmConfig.Builder()
                .apply {
                    moduleName = "settings-test"
                    sourceRoots = listOf(sourceFile)
                    libraries =
                        System.getProperty("settings.processor.test.classpath")
                            .split(File.pathSeparator)
                            .map(::File)
                            .filterNot {
                                !includeDataStore &&
                                    (it.path.contains("/datastore-core/") ||
                                        it.path.contains("/settings-page-datastore/") ||
                                        it.name.startsWith("datastore-core") ||
                                        it.name.startsWith("settings-page-datastore"))
                            }
                    projectBaseDir = directory
                    outputBaseDir = directory.resolve("output")
                    cachesDir = directory.resolve("caches")
                    classOutputDir = outputBaseDir.resolve("classes")
                    kotlinOutputDir = outputBaseDir.resolve("kotlin")
                    javaOutputDir = outputBaseDir.resolve("java")
                    resourceOutputDir = outputBaseDir.resolve("resources")
                    jdkHome = File(System.getProperty("java.home"))
                    jvmTarget = "11"
                    languageVersion =
                        "${KotlinVersion.CURRENT.major}.${KotlinVersion.CURRENT.minor}"
                    apiVersion = languageVersion
                }
                .build()
        val errors = mutableListOf<Diagnostic>()
        val logger =
            object : KSPLogger {
                override fun logging(message: String, symbol: KSNode?) = Unit

                override fun info(message: String, symbol: KSNode?) = Unit

                override fun warn(message: String, symbol: KSNode?) = Unit

                override fun error(message: String, symbol: KSNode?) {
                    errors +=
                        Diagnostic(
                            message,
                            (symbol as? KSDeclaration)?.simpleName?.asString(),
                            symbol?.location as? FileLocation,
                        )
                }

                override fun exception(e: Throwable): Unit = throw AssertionError("KSP failed", e)
            }
        val exitCode =
            KotlinSymbolProcessing(config, listOf(SettingsStoreProcessor.Provider()), logger)
                .execute()
        val generated =
            config.kotlinOutputDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .toList()
        if (compileGenerated && exitCode == KotlinSymbolProcessing.ExitCode.OK) {
            val output = ByteArrayOutputStream()
            val arguments =
                listOf(
                    "-no-stdlib",
                    "-no-reflect",
                    "-jvm-target",
                    config.jvmTarget,
                    "-classpath",
                    config.libraries.joinToString(File.pathSeparator),
                    "-d",
                    directory.resolve("compiled").absolutePath,
                ) + (listOf(sourceFile) + generated).map { it.absolutePath }
            val compilation =
                PrintStream(output).use { K2JVMCompiler().exec(it, *arguments.toTypedArray()) }
            assertEquals(ExitCode.OK, compilation, output.toString(Charsets.UTF_8))
        }
        return Result(exitCode, errors, source, generated.map { it.readText() })
    }

    private data class Diagnostic(
        val message: String,
        val declaration: String?,
        val location: FileLocation?,
    )

    private data class Result(
        val exitCode: KotlinSymbolProcessing.ExitCode,
        val errors: List<Diagnostic>,
        val source: String,
        val generated: List<String>,
    )
}
