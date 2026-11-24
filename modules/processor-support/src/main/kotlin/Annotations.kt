package top.ltfan.m3x.processor

import com.google.devtools.ksp.symbol.KSAnnotation
import kotlin.reflect.KClass

public fun Sequence<KSAnnotation>.findAnnotation(annotation: KClass<*>): KSAnnotation? =
    firstOrNull {
        it.shortName.asString() == annotation.simpleName &&
            it.annotationType.resolve().declaration.qualifiedName?.asString() ==
                annotation.qualifiedName
    }

public fun KSAnnotation.getAllArguments(): Map<String, Any?> {
    val defaultArgs =
        defaultArguments
            .asSequence()
            .mapNotNull { arg -> arg.name?.asString()?.let { it to arg.value } }
            .toMap()
    val explicitArgs =
        arguments
            .asSequence()
            .mapNotNull { arg -> arg.name?.asString()?.let { it to arg.value } }
            .toMap()

    return defaultArgs + explicitArgs
}
