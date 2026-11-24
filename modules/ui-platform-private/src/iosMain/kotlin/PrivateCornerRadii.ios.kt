package top.ltfan.m3x.compose.ui.privateapi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.uikit.LocalUIView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.DoubleVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSInvocation
import platform.Foundation.NSMethodSignature
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import top.ltfan.m3x.compose.ui.layout.CornerRadii
import top.ltfan.m3x.compose.ui.layout.LocalDisplayCornerRadiiHook

@Composable
public actual fun ProvidePrivateCornerRadii(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalDisplayCornerRadiiHook provides { rememberPrivateDisplayCornerRadii() },
        content = content,
    )
}

@OptIn(ExperimentalForeignApi::class)
@Composable
private fun rememberPrivateDisplayCornerRadii(): CornerRadii? {
    if (LocalInspectionMode.current) return null
    val host = currentUIViewOrNull() ?: return null
    val density = LocalDensity.current
    val state = remember(host, density) { mutableStateOf(readDisplayCorners(host.window, density)) }
    DisposableEffect(host, density) {
        val observer = HostLayoutObserver { state.value = readDisplayCorners(host.window, density) }
        host.addSubview(observer)
        observer.setFrame(host.bounds)
        state.value = readDisplayCorners(host.window, density)
        onDispose { observer.removeFromSuperview() }
    }
    return state.value
}

@OptIn(InternalComposeApi::class)
@Composable
private fun currentUIViewOrNull(): UIView? {
    val composer = currentComposer
    return runCatching { composer.consume(LocalUIView) }.getOrNull()
}

private fun readDisplayCorners(window: UIWindow?, density: Density): CornerRadii? {
    val screen = window?.windowScene?.screen ?: return null
    val points = invokeDoubleGetter(screen, "_displayCornerRadius") ?: return null
    if (!screen.scale.isFinite() || screen.scale <= 0.0) return null
    if (!density.density.isFinite() || density.density <= 0f) return null
    val radius =
        (points * screen.scale / density.density)
            .takeIf { it.isFinite() && it >= 0.0 }
            ?.toFloat()
            ?.takeIf { it.isFinite() }
            ?.dp ?: return null
    return CornerRadii(radius, radius, radius, radius)
}

@OptIn(ExperimentalForeignApi::class)
private fun invokeDoubleGetter(target: NSObject, name: String): Double? = memScoped {
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

@OptIn(ExperimentalForeignApi::class)
private class HostLayoutObserver(private val onLayout: () -> Unit) :
    UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    private var layoutUpdateScheduled = false

    init {
        userInteractionEnabled = false
        opaque = false
        backgroundColor = UIColor.clearColor
        autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        scheduleLayoutUpdate()
    }

    override fun didMoveToWindow() {
        super.didMoveToWindow()
        scheduleLayoutUpdate()
    }

    private fun scheduleLayoutUpdate() {
        if (layoutUpdateScheduled) return
        layoutUpdateScheduled = true
        dispatch_async(dispatch_get_main_queue()) {
            layoutUpdateScheduled = false
            onLayout()
        }
    }
}
