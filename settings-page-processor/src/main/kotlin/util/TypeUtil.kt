package top.ltfan.m3x.settingspage.processor.util

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSType
import top.ltfan.m3x.settingspage.processor.error.processorError

class TypeMatcher(private val predicate: KSType.() -> Boolean) {
    operator fun contains(other: Any?): Boolean {
        if (other !is KSType) return false
        return predicate(other)
    }
}

fun number(resolver: Resolver) = TypeMatcher {
    val numberType =
        resolver.getClassDeclarationByName("kotlin.Number")?.asStarProjectedType()
            ?: processorError("Cannot find kotlin.Number type")
    numberType.isAssignableFrom(this)
}

fun pairOfNumbers(resolver: Resolver) = TypeMatcher {
    if (declaration.qualifiedName?.asString() != "kotlin.Pair") return@TypeMatcher false
    val typeArguments = arguments
    if (typeArguments.size != 2) return@TypeMatcher false
    val firstType = typeArguments[0].type?.resolve() ?: return@TypeMatcher false
    val secondType = typeArguments[1].type?.resolve() ?: return@TypeMatcher false
    (firstType in number(resolver)) && (secondType in number(resolver))
}
