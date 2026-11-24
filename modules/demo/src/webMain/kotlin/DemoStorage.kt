package top.ltfan.m3x.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.window
import top.ltfan.m3x.datastore.StoreFile

@Composable
internal actual fun rememberDemoStoreFile(): StoreFile = remember {
    DemoStoreFile(
        { window.localStorage.getItem("m3x-demo-$it") },
        { key, value -> window.localStorage.setItem("m3x-demo-$key", value) },
    )
}

/** Initializes process-wide runtime options before creating the Compose viewport. */
fun initializeDemoRuntime() {
    initializeDemoRuntime { window.localStorage.getItem("m3x-demo-appearance") }
}
