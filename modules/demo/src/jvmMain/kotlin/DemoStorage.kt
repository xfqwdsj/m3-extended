package top.ltfan.m3x.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.util.prefs.Preferences
import top.ltfan.m3x.datastore.StoreFile

@Composable
internal actual fun rememberDemoStoreFile(): StoreFile = remember {
    val preferences = Preferences.userRoot().node("top/ltfan/m3x/demo")
    DemoStoreFile(
        { preferences.get(it, null) },
        { key, value ->
            preferences.put(key, value)
            preferences.flush()
        },
    )
}

/** Initializes process-wide runtime options before application creates its composition. */
fun initializeDemoRuntime() {
    initializeDemoRuntime {
        Preferences.userRoot().node("top/ltfan/m3x/demo").get("appearance", null)
    }
}
