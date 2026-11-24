package top.ltfan.m3x.settingspage.processor.error

import com.google.devtools.ksp.symbol.KSNode

data class SettingsPageProcessorError(override val message: String?, val node: KSNode? = null) :
    Throwable(message)

fun processorError(message: String?, node: KSNode? = null): Nothing =
    throw SettingsPageProcessorError(message, node)
