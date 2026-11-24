package top.ltfan.m3x.settingspage.processor

import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Visibility
import top.ltfan.m3x.settingspage.processor.error.processorError

/** Every segment of an editable path must support both a read and a data-class copy. */
internal fun requireEditableSettingsProperty(
    owner: KSClassDeclaration,
    property: KSPropertyDeclaration,
    path: List<String>,
) {
    val name = property.simpleName.asString()
    val setting = path.joinToString(".")
    val constructor = owner.primaryConstructor
    if (
        constructor == null ||
            constructor.parameters.none {
                it.name?.asString() == name && (it.isVal || it.isVar)
            }
    ) {
        processorError(
            "Setting '$setting' must be a primary constructor property of ${owner.simpleName.asString()} " +
                "so it can be updated with copy()",
            property,
        )
    }
    if (!property.isAccessibleFromGeneratedFile()) {
        processorError(
            "Setting '$setting' must be accessible from the generated describer",
            property,
        )
    }

    // Source-level copy access follows the primary constructor, including with
    // @ExposedCopyVisibility.
    // That annotation only preserves the public binary signature, which KSP may still report as
    // public.
    if (!constructor.isAccessibleFromGeneratedFile()) {
        processorError(
            "Setting '$setting' requires an accessible copy() on ${owner.simpleName.asString()}",
            property,
        )
    }
}

private fun KSDeclaration.isAccessibleFromGeneratedFile(): Boolean =
    generateSequence(this) { it.parentDeclaration }
        .all {
            when (it.getVisibility()) {
                Visibility.PUBLIC -> true
                Visibility.INTERNAL -> it.containingFile != null
                else -> false
            }
        }
