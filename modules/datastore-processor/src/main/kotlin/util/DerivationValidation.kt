package top.ltfan.m3x.datastore.processor.util

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName

internal fun derivedPropertyError(
    name: String,
    isConstructorParameter: Boolean,
    hasBackingField: Boolean,
): String? =
    when {
        isConstructorParameter ->
            "@Derived '$name': constructor parameters already get a generated delegate, move the derived value to the class body"

        hasBackingField ->
            "@Derived '$name': only computed properties with an explicit getter are supported"

        else -> null
    }

internal fun derivedFunctionError(
    name: String,
    parameterCount: Int,
    isSuspend: Boolean,
    hasDefaultParameters: Boolean,
    returnType: TypeName?,
    modelType: ClassName,
): String? =
    when {
        parameterCount != 1 ->
            "@Derived function '$name' must take exactly one parameter: fun withX(value: V)"

        isSuspend -> "@Derived function '$name' must not be suspend"

        hasDefaultParameters -> "@Derived function '$name' must not declare default values"

        returnType != modelType -> "@Derived function '$name' must return ${modelType.simpleName}"

        else -> null
    }

internal fun derivedGetterError(
    name: String,
    getter: String,
    type: TypeName,
    getterType: TypeName?,
): String? =
    when {
        getterType == null ->
            "@Derived('$name'): no readable property named '$getter' exists on the model"

        getterType != type ->
            "@Derived('$name'): '$getter' has type $getterType, but the derived value is $type"

        else -> null
    }

internal fun derivedGetterOnPropertyError(name: String): String =
    "@Derived('$name'): the getter parameter is only valid for functions"

internal fun derivedNameError(functionName: String): String =
    "@Derived function '$functionName' must be named 'withXxx' or declare @Derived(name = \"...\")"

internal fun duplicateNameError(name: String): String =
    "@Derived('$name') collides with another generated member, pass an explicit name to @Derived"

internal fun derivedWithoutStoreError(): String =
    "@Derived can only be used on members of a @Store class or of its derivations provider"

internal fun genericStoreError(className: String): String =
    "@Store does not support generic classes: $className"
