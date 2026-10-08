package top.ltfan.m3x.demo

import androidx.compose.runtime.ComposeRuntimeFlags
import androidx.compose.runtime.ExperimentalComposeApi

@OptIn(ExperimentalComposeApi::class)
private val demoRuntimeInitializer = DemoRuntimeInitializer { enabled ->
    ComposeRuntimeFlags.isLinkBufferComposerEnabled = enabled
}

/** Applies persisted runtime options once, before a platform launcher creates any composition. */
internal fun initializeDemoRuntime(readPreferences: () -> String?) {
    demoRuntimeInitializer.initialize(readPreferences)
}

/** Keeps activity or window recreation from switching the process-wide composer implementation. */
internal class DemoRuntimeInitializer(private val applyLinkComposer: (Boolean) -> Unit) {
    private var initialized = false

    fun initialize(readPreferences: () -> String?) {
        if (initialized) return
        val preferences =
            readPreferences()?.let {
                DemoJsonCodec.create(DemoPreferences.serializer()).decode(it.encodeToByteArray())
            } ?: DemoPreferences()
        applyLinkComposer(preferences.runtime.linkComposer)
        initialized = true
    }
}
