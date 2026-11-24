package top.ltfan.m3x.datastore.processor

import com.squareup.kotlinpoet.ClassName
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertNotNull
import top.ltfan.m3x.datastore.processor.util.derivedFunctionError
import top.ltfan.m3x.datastore.processor.util.derivedGetterError
import top.ltfan.m3x.datastore.processor.util.derivedGetterOnPropertyError
import top.ltfan.m3x.datastore.processor.util.derivedNameError
import top.ltfan.m3x.datastore.processor.util.derivedPropertyError
import top.ltfan.m3x.datastore.processor.util.derivedWithoutStoreError
import top.ltfan.m3x.datastore.processor.util.duplicateNameError
import top.ltfan.m3x.datastore.processor.util.genericStoreError

class DerivationValidationTest {
    private val model = ClassName("com.example", "UiSettingsModel")
    private val boolean = ClassName("kotlin", "Boolean")

    @Test
    fun derivedOnConstructorParameter() {
        val error =
            assertNotNull(
                derivedPropertyError(
                    "blur",
                    isConstructorParameter = true,
                    hasBackingField = false,
                ),
            )
        assertContains(error, "constructor parameter")
    }

    @Test
    fun derivedPropertyWithBackingField() {
        val error =
            assertNotNull(
                derivedPropertyError(
                    "blur",
                    isConstructorParameter = false,
                    hasBackingField = true,
                ),
            )
        assertContains(error, "computed")
    }

    @Test
    fun derivedFunctionWithWrongParameterCount() {
        val error =
            assertNotNull(
                derivedFunctionError(
                    name = "withBlur",
                    parameterCount = 2,
                    isSuspend = false,
                    hasDefaultParameters = false,
                    returnType = model,
                    modelType = model,
                ),
            )
        assertContains(error, "exactly one parameter")
    }

    @Test
    fun derivedSuspendFunction() {
        val error =
            assertNotNull(
                derivedFunctionError(
                    name = "withBlur",
                    parameterCount = 1,
                    isSuspend = true,
                    hasDefaultParameters = false,
                    returnType = model,
                    modelType = model,
                ),
            )
        assertContains(error, "suspend")
    }

    @Test
    fun derivedFunctionWithDefaultParameters() {
        val error =
            assertNotNull(
                derivedFunctionError(
                    name = "withBlur",
                    parameterCount = 1,
                    isSuspend = false,
                    hasDefaultParameters = true,
                    returnType = model,
                    modelType = model,
                ),
            )
        assertContains(error, "default")
    }

    @Test
    fun derivedFunctionWithWrongReturnType() {
        val error =
            assertNotNull(
                derivedFunctionError(
                    name = "withBlur",
                    parameterCount = 1,
                    isSuspend = false,
                    hasDefaultParameters = false,
                    returnType = boolean,
                    modelType = model,
                ),
            )
        assertContains(error, "must return UiSettingsModel")
    }

    @Test
    fun duplicateGeneratedName() {
        assertContains(duplicateNameError("blur"), "collides")
    }

    @Test
    fun missingGetter() {
        val error = assertNotNull(derivedGetterError("blurEnabled", "blur", boolean, null))
        assertContains(error, "no readable property named 'blur'")
    }

    @Test
    fun getterTypeMismatch() {
        val error =
            assertNotNull(
                derivedGetterError("blurEnabled", "blur", boolean, ClassName("kotlin", "Int")),
            )
        assertContains(error, "has type")
    }

    @Test
    fun getterOnReadOnlyProperty() {
        assertContains(derivedGetterOnPropertyError("blur"), "only valid for functions")
    }

    @Test
    fun functionWithoutWithPrefix() {
        assertContains(derivedNameError("blur"), "withXxx")
    }

    @Test
    fun derivedWithoutStore() {
        assertContains(derivedWithoutStoreError(), "@Store class")
    }

    @Test
    fun genericStore() {
        assertContains(genericStoreError("Box"), "does not support generic classes: Box")
    }
}
