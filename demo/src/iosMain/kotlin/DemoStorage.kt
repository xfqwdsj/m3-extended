package top.ltfan.m3x.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults
import top.ltfan.m3x.datastore.StoreFile

@Composable
internal actual fun rememberDemoStoreFile(): StoreFile = remember {
    val preferences = NSUserDefaults.standardUserDefaults
    DemoStoreFile(
        { preferences.stringForKey("m3x-demo-$it") },
        { key, value -> preferences.setObject(value, "m3x-demo-$key") },
    )
}

/** Initializes process-wide runtime options before creating a Compose view controller. */
fun initializeDemoRuntime() {
    initializeDemoRuntime {
        NSUserDefaults.standardUserDefaults.stringForKey("m3x-demo-appearance")
    }
}
