package top.ltfan.m3x.compose.ui.privateapi

import kotlinx.cinterop.DoubleVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.Foundation.NSInvocation
import platform.Foundation.NSMethodSignature
import platform.Foundation.NSSelectorFromString
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
internal fun invokeDoubleGetter(target: NSObject, name: String): Double? = memScoped {
    val selector = NSSelectorFromString(name)
    if (!target.respondsToSelector(selector)) return@memScoped null
    @Suppress("CAST_NEVER_SUCCEEDS")
    val signature =
        target.methodSignatureForSelector(selector) as? NSMethodSignature ?: return@memScoped null
    if (
        signature.numberOfArguments != 2uL ||
            signature.methodReturnType?.toKString() != "d" ||
            signature.methodReturnLength != 8uL
    ) {
        return@memScoped null
    }
    val invocation = NSInvocation.invocationWithMethodSignature(signature)
    invocation.selector = selector
    invocation.target = target
    invocation.invoke()
    val value = alloc<DoubleVar>()
    invocation.getReturnValue(value.ptr)
    value.value.takeIf { it.isFinite() && it >= 0.0 }
}
