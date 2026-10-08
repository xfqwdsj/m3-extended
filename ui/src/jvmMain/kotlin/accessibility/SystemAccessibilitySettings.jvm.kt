package top.ltfan.m3x.compose.ui.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalInspectionMode
import com.sun.jna.Callback
import com.sun.jna.CallbackReference
import com.sun.jna.Function
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import java.awt.EventQueue
import java.awt.GraphicsEnvironment
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

@Composable
internal actual fun rememberSystemAccessibilitySettingsPlatform(): SystemAccessibilitySettings {
    if (LocalInspectionMode.current || GraphicsEnvironment.isHeadless())
        return SystemAccessibilitySettings()
    val mac = isMacOS()
    var settings by remember {
        mutableStateOf(if (mac) SystemAccessibilitySettings() else readOtherPlatform())
    }
    val disposed = remember { AtomicBoolean(false) }
    DisposableEffect(Unit) {
        disposed.set(false)
        onDispose { disposed.set(true) }
    }
    LaunchedEffect(mac, disposed) {
        while (true) {
            if (mac) requestMacSettings(disposed) { settings = it }
            else settings = readOtherPlatform()
            delay(1000.milliseconds)
        }
    }
    return settings
}

private fun isMacOS(): Boolean = System.getProperty("os.name").startsWith("Mac", ignoreCase = true)

private fun readOtherPlatform(): SystemAccessibilitySettings =
    if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        readWindowsSettings()
    else SystemAccessibilitySettings()

private data class MacReadRequest(
    val disposed: AtomicBoolean,
    val onResult: (SystemAccessibilitySettings) -> Unit,
)

private val nextRequestId = AtomicLong()
private val macRequests = ConcurrentHashMap<Long, MacReadRequest>()
private val systemLibrary by lazy { NativeLibrary.getInstance("System") }
private val dispatchFunction by lazy { systemLibrary.getFunction("dispatch_async_f") }
private val mainQueue by lazy { systemLibrary.getGlobalVariableAddress("_dispatch_main_q") }

private fun interface DispatchCallback : Callback {
    fun invoke(context: Pointer?)
}

private val nativeCallback =
    object : DispatchCallback {
        override fun invoke(context: Pointer?) {
            val id = context?.let(Pointer::nativeValue) ?: return
            val request = macRequests.remove(id) ?: return
            val result = readMacSettings()
            EventQueue.invokeLater { if (!request.disposed.get()) request.onResult(result) }
        }
    }
private val nativeCallbackPointer by lazy { CallbackReference.getFunctionPointer(nativeCallback) }

private fun requestMacSettings(
    disposed: AtomicBoolean,
    onResult: (SystemAccessibilitySettings) -> Unit,
) {
    val id = nextRequestId.incrementAndGet()
    macRequests[id] = MacReadRequest(disposed, onResult)
    runCatching {
        dispatchFunction.invokeVoid(
            arrayOf(mainQueue, Pointer.createConstant(id), nativeCallbackPointer),
        )
    }
        .onFailure {
            macRequests.remove(id)
            EventQueue.invokeLater { if (!disposed.get()) onResult(SystemAccessibilitySettings()) }
        }
}

private val macFunctions by lazy {
    NativeLibrary.getInstance("/System/Library/Frameworks/AppKit.framework/AppKit")
    val objc = NativeLibrary.getInstance("/usr/lib/libobjc.A.dylib")
    val getClass = objc.getFunction("objc_getClass")
    val send = objc.getFunction("objc_msgSend")
    val registerSelector = objc.getFunction("sel_registerName")
    val workspaceClass = getClass.invokePointer(arrayOf("NSWorkspace"))
    val sharedSelector = registerSelector.invokePointer(arrayOf("sharedWorkspace"))
    val workspace = send.invokePointer(arrayOf(workspaceClass, sharedSelector))
    Triple(objc, send, Pair(registerSelector, workspace))
}

private fun readMacSettings(): SystemAccessibilitySettings = runCatching {
    val (_, send, selectorWorkspace) = macFunctions
    val (registerSelector, workspace) = selectorWorkspace
    fun read(selector: String): Boolean? = runCatching {
        val selectorPointer = registerSelector.invokePointer(arrayOf(selector))
        send.invokeInt(arrayOf(workspace, selectorPointer)) and 0xff != 0
    }
        .getOrNull()
    SystemAccessibilitySettings(
        reduceTransparency = read("accessibilityDisplayShouldReduceTransparency"),
        increaseContrast = read("accessibilityDisplayShouldIncreaseContrast"),
        reduceMotion = read("accessibilityDisplayShouldReduceMotion"),
    )
}
    .getOrElse { SystemAccessibilitySettings() }

private val windowsFunctions by lazy {
    val convention =
        if (Native.POINTER_SIZE == 4) Function.ALT_CONVENTION else Function.C_CONVENTION
    val options = mapOf(Library.OPTION_CALLING_CONVENTION to convention)
    NativeLibrary.getInstance("user32", options) to NativeLibrary.getInstance("advapi32", options)
}

private fun readWindowsSettings(): SystemAccessibilitySettings {
    val (user32, _) =
        runCatching { windowsFunctions }
            .getOrElse {
                return SystemAccessibilitySettings()
            }
    return SystemAccessibilitySettings(
        reduceTransparency = runCatching {
                readRegistryDword("EnableTransparency")?.let {
                    if (it == 0) true else if (it == 1) false else null
                }
            }
                .getOrNull(),
        increaseContrast = runCatching { readHighContrast(user32) }.getOrNull(),
        reduceMotion = runCatching { readClientAreaAnimation(user32)?.not() }.getOrNull(),
    )
}

private fun readHighContrast(user32: NativeLibrary): Boolean? {
    val size = 8 + Native.POINTER_SIZE
    Memory(size.toLong()).use { structure ->
        structure.clear()
        structure.setInt(0, size)
        val success =
            user32
                .getFunction("SystemParametersInfoW")
                .invokeInt(arrayOf(0x0042, size, structure, 0))
        return if (success == 0) null else structure.getInt(4) and 1 != 0
    }
}

private fun readClientAreaAnimation(user32: NativeLibrary): Boolean? =
    Memory(4).use { value ->
        value.clear()
        val success =
            user32.getFunction("SystemParametersInfoW").invokeInt(arrayOf(0x1042, 0, value, 0))
        if (success == 0) null else value.getInt(0) != 0
    }

private fun readRegistryDword(name: String): Int? {
    val (_, advapi32) = windowsFunctions
    val key = "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize"
    val hkeyCurrentUser = Pointer.createConstant(0x80000001L.toInt().toLong())
    val flags = if (Native.POINTER_SIZE == 4) Function.ALT_CONVENTION else Function.C_CONVENTION
    val function = advapi32.getFunction("RegGetValueW", flags)
    return Memory((key.length + 1L) * 2L).use { keyMemory ->
        Memory((name.length + 1L) * 2L).use { nameMemory ->
            Memory(4).use { type ->
                Memory(4).use { data ->
                    Memory(4).use { size ->
                        keyMemory.setWideString(0, key)
                        nameMemory.setWideString(0, name)
                        size.setInt(0, 4)
                        val status =
                            function.invokeInt(
                                arrayOf(
                                    hkeyCurrentUser,
                                    keyMemory,
                                    nameMemory,
                                    0x10,
                                    type,
                                    data,
                                    size,
                                ),
                            )
                        if (status == 0 && type.getInt(0) == 4 && size.getInt(0) == 4)
                            data.getInt(0)
                        else null
                    }
                }
            }
        }
    }
}
