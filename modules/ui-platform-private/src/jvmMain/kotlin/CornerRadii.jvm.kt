package top.ltfan.m3x.compose.ui.privateapi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.LocalAwtWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sun.jna.Callback
import com.sun.jna.Memory
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import java.awt.EventQueue
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.beans.PropertyChangeListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import top.ltfan.m3x.compose.ui.layout.CornerRadii
import top.ltfan.m3x.compose.ui.layout.LocalWindowCornerRadiiHook

@Composable
public actual fun ProvidePrivateCornerRadii(content: @Composable () -> Unit) {
    if (System.getProperty("os.name").contains("mac", ignoreCase = true)) {
        CompositionLocalProvider(
            LocalWindowCornerRadiiHook provides { rememberPrivateWindowCornerRadii() },
            content = content,
        )
    } else {
        content()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun rememberPrivateWindowCornerRadii(): CornerRadii? {
    if (LocalInspectionMode.current) return null
    val window = LocalAwtWindow.current?.takeIf { it is ComposeWindow || it is ComposeDialog }
    val density = LocalDensity.current
    val state = remember(window) { mutableStateOf<CornerRadii?>(null) }
    DisposableEffect(window, density) {
        if (window == null) return@DisposableEffect onDispose {}
        var disposed = false
        var closed = false
        var generation = 0L
        fun scheduleRefresh() {
            if (!EventQueue.isDispatchThread()) {
                EventQueue.invokeLater { scheduleRefresh() }
                return
            }
            if (disposed || closed) return
            val currentGeneration = ++generation
            requestWindowCorners(window, density) { result ->
                if (!disposed && !closed && currentGeneration == generation) state.value = result
            }
        }
        val componentListener =
            object : ComponentAdapter() {
                override fun componentResized(event: ComponentEvent) = scheduleRefresh()

                override fun componentMoved(event: ComponentEvent) = scheduleRefresh()

                override fun componentShown(event: ComponentEvent) = scheduleRefresh()
            }
        val propertyListener = PropertyChangeListener { scheduleRefresh() }
        val windowListener =
            object : WindowAdapter() {
                override fun windowOpened(event: WindowEvent) = scheduleRefresh()

                override fun windowStateChanged(event: WindowEvent) = scheduleRefresh()

                override fun windowClosing(event: WindowEvent) {
                    closed = true
                    generation++
                    state.value = null
                }

                override fun windowClosed(event: WindowEvent) {
                    closed = true
                    generation++
                    state.value = null
                }
            }
        window.addComponentListener(componentListener)
        window.addPropertyChangeListener(propertyListener)
        window.addWindowListener(windowListener)
        window.addWindowStateListener(windowListener)
        scheduleRefresh()
        onDispose {
            disposed = true
            generation++
            window.removeComponentListener(componentListener)
            window.removePropertyChangeListener(propertyListener)
            window.removeWindowListener(windowListener)
            window.removeWindowStateListener(windowListener)
        }
    }
    return state.value
}

private fun requestWindowCorners(
    window: Window,
    density: Density,
    onResult: (CornerRadii?) -> Unit,
) {
    if (
        !System.getProperty("os.name").contains("mac", ignoreCase = true) || !window.isDisplayable
    ) {
        onResult(null)
        return
    }
    val handle =
        when (window) {
            is ComposeWindow -> window.windowHandle
            is ComposeDialog -> window.windowHandle
            else -> {
                onResult(null)
                return
            }
        }
    if (handle == 0L) {
        onResult(null)
        return
    }
    try {
        CocoaCornerReader.request(Pointer(handle), density, onResult)
    } catch (_: LinkageError) {
        onResult(null)
    } catch (_: UnsatisfiedLinkError) {
        onResult(null)
    }
}

private object CocoaCornerReader {
    private val system by lazy { NativeLibrary.getInstance("System") }
    private val objc by lazy { NativeLibrary.getInstance("objc") }
    private val msgSend by lazy { objc.getFunction("objc_msgSend") }
    private val selRegister by lazy { objc.getFunction("sel_registerName") }
    private val callbackIds = AtomicLong()
    private val callbacks = ConcurrentHashMap<Long, Callback>()
    private val dispatchAsync by lazy { system.getFunction("dispatch_async_f") }
    private val mainQueue by lazy { system.getGlobalVariableAddress("_dispatch_main_q") }

    private fun selector(name: String): Pointer =
        selRegister.invokePointer(arrayOf(name)) ?: throw IllegalStateException("Missing selector.")

    private fun responds(target: Pointer, name: String): Boolean =
        msgSend.invokeInt(arrayOf(target, selector("respondsToSelector:"), selector(name))) != 0

    fun request(target: Pointer, density: Density, onResult: (CornerRadii?) -> Unit) {
        val async = dispatchAsync
        val queue = mainQueue
        val id = callbackIds.incrementAndGet()
        val callback =
            object : DispatchWork {
                override fun invoke(context: Pointer?) {
                    val retained = context ?: return
                    try {
                        val result = readCorners(retained, density)
                        EventQueue.invokeLater { onResult(result) }
                    } catch (_: LinkageError) {
                        EventQueue.invokeLater { onResult(null) }
                    } finally {
                        objc.getFunction("objc_release").invokeVoid(arrayOf(retained))
                        callbacks.remove(id)
                    }
                }
            }
        callbacks[id] = callback
        val retained = objc.getFunction("objc_retain").invokePointer(arrayOf(target))
        if (retained == null) {
            callbacks.remove(id)
            onResult(null)
            return
        }
        try {
            async.invokeVoid(arrayOf(queue, retained, callback))
        } catch (error: Throwable) {
            objc.getFunction("objc_release").invokeVoid(arrayOf(retained))
            callbacks.remove(id)
            throw error
        }
    }

    private fun readCorners(target: Pointer, density: Density): CornerRadii? {
        val values =
            listOf("_topCornerRadius", "_bottomCornerRadius").map { invokeDoubleGetter(target, it) }
        val fallback = invokeDoubleGetter(target, "_cornerRadius")
        val top = values[0] ?: fallback
        val bottom = values[1] ?: fallback
        if (top == null && bottom == null) return null
        val backingScale = invokeDoubleGetter(target, "backingScaleFactor") ?: return null
        if (!backingScale.isFinite() || backingScale <= 0.0) return null
        if (!density.density.isFinite() || density.density <= 0f) return null
        fun radius(points: Double?): Dp? =
            points
                ?.takeIf { it.isFinite() && it >= 0.0 }
                ?.let { it * backingScale / density.density }
                ?.takeIf { it.isFinite() && it >= 0.0 }
                ?.toFloat()
                ?.takeIf { it.isFinite() }
                ?.dp
        val radii = CornerRadii(radius(top), radius(top), radius(bottom), radius(bottom))
        return radii.takeIf {
            it.topLeft != null ||
                it.topRight != null ||
                it.bottomRight != null ||
                it.bottomLeft != null
        }
    }

    private fun invokeDoubleGetter(target: Pointer, name: String): Double? {
        if (!responds(target, name)) return null
        val selected = selector(name)
        val signature =
            msgSend.invokePointer(
                arrayOf(target, selector("methodSignatureForSelector:"), selected),
            ) ?: return null
        val count = msgSend.invokeLong(arrayOf(signature, selector("numberOfArguments")))
        val type =
            msgSend.invokePointer(arrayOf(signature, selector("methodReturnType")))?.getString(0)
        val length = msgSend.invokeLong(arrayOf(signature, selector("methodReturnLength")))
        if (count != 2L || type != "d" || length != 8L) return null
        val invocationClass =
            objc.getFunction("objc_getClass").invokePointer(arrayOf("NSInvocation")) ?: return null
        val invocation =
            msgSend.invokePointer(
                arrayOf(invocationClass, selector("invocationWithMethodSignature:"), signature),
            ) ?: return null
        msgSend.invokeVoid(arrayOf(invocation, selector("setSelector:"), selected))
        msgSend.invokeVoid(arrayOf(invocation, selector("setTarget:"), target))
        msgSend.invokeVoid(arrayOf(invocation, selector("invoke")))
        val storage = Memory(8)
        return storage.use { storage ->
            msgSend.invokeVoid(arrayOf(invocation, selector("getReturnValue:"), storage))
            storage.getDouble(0).takeIf { it.isFinite() && it >= 0.0 }
        }
    }

    private fun interface DispatchWork : Callback {
        fun invoke(context: Pointer?)
    }
}
