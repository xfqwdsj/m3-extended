package top.ltfan.m3x.datastore.processor

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import top.ltfan.m3x.datastore.annotation.Derived
import top.ltfan.m3x.datastore.annotation.Store
import top.ltfan.m3x.datastore.processor.error.processorError
import top.ltfan.m3x.datastore.processor.generator.DerivedModel
import top.ltfan.m3x.datastore.processor.generator.PropertyModel
import top.ltfan.m3x.datastore.processor.generator.StoreFileGenerator
import top.ltfan.m3x.datastore.processor.generator.StoreModel
import top.ltfan.m3x.datastore.processor.util.derivedDefaultName
import top.ltfan.m3x.datastore.processor.util.derivedFunctionError
import top.ltfan.m3x.datastore.processor.util.derivedGetterError
import top.ltfan.m3x.datastore.processor.util.derivedGetterOnPropertyError
import top.ltfan.m3x.datastore.processor.util.derivedNameError
import top.ltfan.m3x.datastore.processor.util.derivedPropertyError
import top.ltfan.m3x.datastore.processor.util.derivedWithoutStoreError
import top.ltfan.m3x.datastore.processor.util.duplicateNameError
import top.ltfan.m3x.datastore.processor.util.genericStoreError
import top.ltfan.m3x.datastore.processor.util.resolveStoreName
import top.ltfan.m3x.processor.findAnnotation
import top.ltfan.m3x.processor.getAllArguments

class StoreProcessor(
    private val processed: MutableSet<KSClassDeclaration>,
    private val processedDerived: MutableSet<KSAnnotated>,
    private val environment: SymbolProcessorEnvironment,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val stores =
            resolver
                .getSymbolsWithAnnotation(Store::class.qualifiedName!!)
                .filterIsInstance<KSClassDeclaration>()
                .toList()

        val providers = mutableSetOf<KSClassDeclaration>()
        stores.forEach { symbol -> symbol.derivationsProvider()?.let(providers::add) }

        stores.forEach { symbol ->
            if (processed.add(symbol)) processStore(symbol)
        }

        resolver.getSymbolsWithAnnotation(Derived::class.qualifiedName!!).forEach { symbol ->
            if (!processedDerived.add(symbol)) return@forEach
            val parent = (symbol as? KSDeclaration)?.parentDeclaration as? KSClassDeclaration
            if (parent == null || parent in providers) return@forEach
            if (parent.annotations.findAnnotation(Store::class) == null) {
                processorError(derivedWithoutStoreError())
            }
        }

        return emptyList()
    }

    private fun processStore(symbol: KSClassDeclaration) {
        if (Modifier.DATA !in symbol.modifiers) {
            processorError(
                "@Store can only be applied to a data class: ${symbol.qualifiedName?.asString()}",
            )
        }
        if (symbol.typeParameters.isNotEmpty()) {
            processorError(genericStoreError(symbol.simpleName.asString()))
        }

        val modelType = symbol.toClassName()
        val arguments = symbol.annotations.findAnnotation(Store::class)!!.getAllArguments()
        val codec =
            (arguments["codec"] as? KSType)?.toClassName()
                ?: processorError("@Store must declare a codec")
        val codecFunction = (arguments["codecFunction"] as? String).orEmpty().ifEmpty { "create" }
        val storeName =
            resolveStoreName(symbol.simpleName.asString(), (arguments["name"] as? String).orEmpty())

        val parameters =
            symbol.primaryConstructor?.parameters
                ?: processorError(
                    "@Store class must have a primary constructor: ${symbol.simpleName.asString()}",
                )
        val constructorParameters = parameters.mapNotNull { it.name?.asString() }.toSet()
        val names = mutableSetOf<String>()
        val properties = parameters.map { parameter ->
            val parameterName =
                parameter.name?.asString()
                    ?: processorError("@Store constructor parameters must be named")
            if (!parameter.hasDefault) {
                processorError(
                    "@Store model '${modelType.simpleName}' constructor parameter '$parameterName' " +
                        "must declare a default value",
                )
            }
            if (parameter.annotations.findAnnotation(Derived::class) != null) {
                processorError(
                    derivedPropertyError(
                        parameterName,
                        isConstructorParameter = true,
                        hasBackingField = false,
                    )!!,
                )
            }
            names += parameterName
            PropertyModel(parameterName, parameter.type.resolve().toTypeName())
        }

        val derivations = buildList {
            addAll(modelDerivations(symbol, modelType, constructorParameters, names))
            symbol.derivationsProvider()?.let { provider ->
                addAll(
                    providerDerivations(provider, symbol, modelType, constructorParameters, names),
                )
            }
        }

        val hasSettingsStore =
            symbol.annotations.any {
                it.annotationType.resolve().declaration.qualifiedName?.asString() == SETTINGS_STORE
            }

        val file =
            StoreFileGenerator()
                .generate(
                    StoreModel(
                        className = modelType,
                        storeName = storeName,
                        codec = codec,
                        codecFunction = codecFunction,
                        properties = properties,
                        derivations = derivations,
                        hasSettingsStore = hasSettingsStore,
                    ),
                )
        val originatingFiles =
            listOfNotNull(
                    symbol.containingFile,
                    symbol.derivationsProvider()?.containingFile,
                )
                .distinct()
        try {
            file.writeTo(environment.codeGenerator, false, originatingFiles)
        } catch (e: Exception) {
            processorError("Failed to generate file: ${e.message}")
        }
    }

    private fun modelDerivations(
        symbol: KSClassDeclaration,
        modelType: ClassName,
        constructorParameters: Set<String>,
        names: MutableSet<String>,
    ): List<DerivedModel> = buildList {
        for (member in symbol.declarations) {
            val annotation = member.annotations.findAnnotation(Derived::class) ?: continue
            val arguments = annotation.getAllArguments()
            val explicitName = (arguments["name"] as? String).orEmpty()
            val explicitGetter = (arguments["getter"] as? String).orEmpty()
            when (member) {
                is KSPropertyDeclaration ->
                    add(
                        readOnlyDerivation(
                            member,
                            constructorParameters,
                            explicitName,
                            explicitGetter,
                            names,
                        ),
                    )

                is KSFunctionDeclaration ->
                    add(
                        writableDerivation(
                            model = symbol,
                            function = member,
                            modelType = modelType,
                            explicitName = explicitName,
                            explicitGetter = explicitGetter,
                            names = names,
                            owner = null,
                        ),
                    )
            }
        }
    }

    private fun providerDerivations(
        provider: KSClassDeclaration,
        model: KSClassDeclaration,
        modelType: ClassName,
        constructorParameters: Set<String>,
        names: MutableSet<String>,
    ): List<DerivedModel> = buildList {
        val owner = provider.toClassName()
        for (member in provider.declarations) {
            val annotation = member.annotations.findAnnotation(Derived::class) ?: continue
            val arguments = annotation.getAllArguments()
            val explicitName = (arguments["name"] as? String).orEmpty()
            val explicitGetter = (arguments["getter"] as? String).orEmpty()
            val receiverType =
                when (member) {
                        is KSPropertyDeclaration -> member.extensionReceiver
                        is KSFunctionDeclaration -> member.extensionReceiver
                        else -> null
                    }
                    ?.resolve()
                    ?.toTypeName()
            if (receiverType != modelType) {
                processorError(
                    "@Derived members of the ${provider.simpleName.asString()} provider " +
                        "must extend ${modelType.simpleName}",
                )
            }
            when (member) {
                is KSPropertyDeclaration ->
                    add(
                        readOnlyDerivation(
                            member,
                            constructorParameters,
                            explicitName,
                            explicitGetter,
                            names,
                            owner,
                        ),
                    )

                is KSFunctionDeclaration ->
                    add(
                        writableDerivation(
                            model = model,
                            function = member,
                            modelType = modelType,
                            explicitName = explicitName,
                            explicitGetter = explicitGetter,
                            names = names,
                            owner = owner,
                        ),
                    )
            }
        }
    }

    private fun readOnlyDerivation(
        property: KSPropertyDeclaration,
        constructorParameters: Set<String>,
        explicitName: String,
        explicitGetter: String,
        names: MutableSet<String>,
        owner: ClassName? = null,
    ): DerivedModel.ReadOnly {
        val propertyName = property.simpleName.asString()
        derivedPropertyError(
                propertyName,
                propertyName in constructorParameters,
                property.hasBackingField,
            )
            ?.let(::processorError)
        if (explicitGetter.isNotEmpty()) processorError(derivedGetterOnPropertyError(propertyName))
        val name = explicitName.ifEmpty { propertyName }
        checkName(name, names)
        return DerivedModel.ReadOnly(
            name,
            property.type.resolve().toTypeName(),
            propertyName,
            owner,
        )
    }

    private fun writableDerivation(
        model: KSClassDeclaration,
        function: KSFunctionDeclaration,
        modelType: ClassName,
        explicitName: String,
        explicitGetter: String,
        names: MutableSet<String>,
        owner: ClassName?,
    ): DerivedModel.Writable {
        val functionName = function.simpleName.asString()
        derivedFunctionError(
                name = functionName,
                parameterCount = function.parameters.size,
                isSuspend = Modifier.SUSPEND in function.modifiers,
                hasDefaultParameters = function.parameters.any { it.hasDefault },
                returnType = function.returnType?.resolve()?.toTypeName(),
                modelType = modelType,
            )
            ?.let(::processorError)

        val name = explicitName.ifEmpty {
            derivedDefaultName(functionName) ?: processorError(derivedNameError(functionName))
        }
        checkName(name, names)

        val valueType = function.parameters.first().type.resolve().toTypeName()
        val getterName = explicitGetter.ifEmpty { name }
        derivedGetterError(name, getterName, valueType, readableType(model, getterName))
            ?.let(::processorError)

        return DerivedModel.Writable(name, valueType, getterName, functionName, owner)
    }

    private fun readableType(model: KSClassDeclaration, name: String): TypeName? {
        model.primaryConstructor?.parameters.orEmpty().forEach { parameter ->
            if (parameter.name?.asString() == name) return parameter.type.resolve().toTypeName()
        }
        return model.declarations
            .filterIsInstance<KSPropertyDeclaration>()
            .firstOrNull { it.simpleName.asString() == name && Modifier.PRIVATE !in it.modifiers }
            ?.type
            ?.resolve()
            ?.toTypeName()
    }

    private fun checkName(name: String, names: MutableSet<String>) {
        if (!names.add(name)) processorError(duplicateNameError(name))
    }

    private fun KSClassDeclaration.derivationsProvider(): KSClassDeclaration? {
        val arguments = annotations.findAnnotation(Store::class)?.getAllArguments() ?: return null
        val type = arguments["derivations"] as? KSType ?: return null
        if (type.declaration.qualifiedName?.asString() == "kotlin.Unit") return null
        return type.declaration as? KSClassDeclaration
    }

    class Provider : SymbolProcessorProvider {
        private val processed = mutableSetOf<KSClassDeclaration>()
        private val processedDerived = mutableSetOf<KSAnnotated>()

        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
            StoreProcessor(processed, processedDerived, environment)
    }

    private companion object {
        const val SETTINGS_STORE = "top.ltfan.m3x.settingspage.annotation.SettingsStore"
    }
}
