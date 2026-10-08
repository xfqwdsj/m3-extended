package top.ltfan.m3x.settingspage.processor

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ksp.writeTo
import top.ltfan.m3x.settingspage.annotation.SettingsStore
import top.ltfan.m3x.settingspage.processor.error.SettingsPageProcessorError
import top.ltfan.m3x.settingspage.processor.error.processorError

/** Generates binding code; no reflection or Material UI is used at runtime. */
public class SettingsStoreProcessor(
    private val environment: SymbolProcessorEnvironment,
    private val processed: MutableSet<String>,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val deferred = mutableListOf<KSAnnotated>()
        resolver
            .getSymbolsWithAnnotation(SettingsStore::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { model ->
                if (!model.validate(enableNewFeatures = true)) {
                    deferred += model
                    return@forEach
                }
                val name =
                    model.qualifiedName?.asString()
                        ?: processorError("Settings model needs a qualified name")
                if (processed.add(name))
                    try {
                        SettingsStoreFileGenerator()
                            .generate(model)
                            .writeTo(
                                environment.codeGenerator,
                                aggregating = false,
                                originatingKSFiles = listOfNotNull(model.containingFile),
                            )
                    } catch (error: SettingsPageProcessorError) {
                        environment.logger.error(
                            error.message ?: "Invalid settings declaration",
                            error.node ?: model,
                        )
                    }
            }
        return deferred
    }

    public class Provider : SymbolProcessorProvider {
        private val processed = mutableSetOf<String>()

        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
            SettingsStoreProcessor(environment, processed)
    }
}
