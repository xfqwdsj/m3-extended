package top.ltfan.m3x.compose.ui.privateapi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.DoubleVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.alloc
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.AppKit.NSWindow
import platform.AppKit.NSWindowDidBecomeKeyNotification
import platform.AppKit.NSWindowDidChangeBackingPropertiesNotification
import platform.AppKit.NSWindowDidChangeOcclusionStateNotification
import platform.AppKit.NSWindowDidChangeScreenNotification
import platform.AppKit.NSWindowDidEnterFullScreenNotification
import platform.AppKit.NSWindowDidExitFullScreenNotification
import platform.AppKit.NSWindowDidMoveNotification
import platform.AppKit.NSWindowDidResizeNotification
import platform.AppKit.NSWindowWillCloseNotification
import platform.Foundation.NSInvocation
import platform.Foundation.NSKeyValueObservingOptionNew
import platform.Foundation.NSMethodSignature
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNotificationName
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSSelectorFromString
import platform.Foundation.addObserver
import platform.Foundation.removeObserver
import platform.darwin.NSObject
import platform.objc.class_addMethod
import platform.objc.object_getClass
import top.ltfan.m3x.compose.ui.layout.CornerRadii
import top.ltfan.m3x.compose.ui.layout.LocalWindowCornerRadiiHook
import top.ltfan.m3x.compose.ui.layout.currentCornerRadiiWindow

@Composable
public actual fun ProvidePrivateCornerRadii(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalWindowCornerRadiiHook provides { rememberPrivateWindowCornerRadii() },
        content = content,
    )
}

@Composable
private fun rememberPrivateWindowCornerRadii(): CornerRadii? {
    if (LocalInspectionMode.current) return null
    val window = currentCornerRadiiWindow() ?: return null
    val density = LocalDensity.current
    val state = remember(window, density) { mutableStateOf<CornerRadii?>(null) }
    DisposableEffect(window, density) {
        var closed = false
        val center = NSNotificationCenter.defaultCenter
        val observerNames: List<NSNotificationName> =
            listOf(
                NSWindowDidBecomeKeyNotification,
                NSWindowDidChangeOcclusionStateNotification,
                NSWindowDidResizeNotification,
                NSWindowDidMoveNotification,
                NSWindowDidChangeBackingPropertiesNotification,
                NSWindowDidChangeScreenNotification,
                NSWindowDidEnterFullScreenNotification,
                NSWindowDidExitFullScreenNotification,
                NSWindowWillCloseNotification,
            )
        val observers = observerNames.map { name ->
            center.addObserverForName(
                name = name,
                `object` = window,
                queue = NSOperationQueue.mainQueue,
            ) { notification ->
                if (notification?.name == NSWindowWillCloseNotification) {
                    closed = true
                    state.value = null
                } else if (!closed) {
                    state.value = readWindowCornerRadii(window, density)
                }
            }
        }
        val styleObserver =
            WindowStyleObserver(window) {
                if (!closed) state.value = readWindowCornerRadii(window, density)
            }
        state.value = readWindowCornerRadii(window, density)
        onDispose {
            closed = true
            observers.forEach(center::removeObserver)
            styleObserver.dispose()
        }
    }
    return state.value
}

private fun readWindowCornerRadii(window: NSWindow, density: Density): CornerRadii? {
    if (!window.isVisible()) return null
    val top = invokeDoubleGetter(window, "_topCornerRadius")
    val bottom = invokeDoubleGetter(window, "_bottomCornerRadius")
    val fallback = invokeDoubleGetter(window, "_cornerRadius")
    val scale = invokeDoubleGetter(window, "backingScaleFactor") ?: return null
    if (!scale.isFinite() || scale <= 0.0 || !density.density.isFinite() || density.density <= 0f)
        return null
    fun radius(value: Double?): Dp? =
        value
            ?.let { it * scale / density.density }
            ?.takeIf { it.isFinite() && it >= 0.0 }
            ?.toFloat()
            ?.takeIf { it.isFinite() }
            ?.dp
    val topRadius = radius(top ?: fallback)
    val bottomRadius = radius(bottom ?: fallback)
    return CornerRadii(topRadius, topRadius, bottomRadius, bottomRadius).takeIf {
        it.topLeft != null || it.topRight != null || it.bottomLeft != null || it.bottomRight != null
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class WindowStyleObserver(
    private val window: NSWindow,
    onChange: () -> Unit,
) : NSObject() {
    private val callback = StableRef.create(onChange)

    init {
        class_addMethod(
            object_getClass(this),
            NSSelectorFromString("observeValueForKeyPath:ofObject:change:context:"),
            staticCFunction(::styleChanged).reinterpret(),
            "v@:@@@^v",
        )
        window.addObserver(
            observer = this,
            forKeyPath = "styleMask",
            options = NSKeyValueObservingOptionNew,
            context = callback.asCPointer(),
        )
    }

    fun dispose() {
        window.removeObserver(
            observer = this,
            forKeyPath = "styleMask",
            context = callback.asCPointer(),
        )
        callback.dispose()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun styleChanged(
    self: COpaquePointer?,
    selector: COpaquePointer?,
    key: COpaquePointer?,
    obj: COpaquePointer?,
    change: COpaquePointer?,
    context: COpaquePointer?,
) {
    context?.asStableRef<() -> Unit>()?.get()?.invoke()
}

@OptIn(ExperimentalForeignApi::class)
private fun invokeDoubleGetter(target: NSObject, name: String): Double? = memScoped {
    val selector = NSSelectorFromString(name)
    if (!target.respondsToSelector(selector)) return@memScoped null
    // Darwin exposes a forward declaration; Foundation provides the complete Objective-C class.
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
