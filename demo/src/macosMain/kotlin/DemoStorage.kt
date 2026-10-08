package top.ltfan.m3x.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults
import top.ltfan.m3x.datastore.StoreFile

@Composable
internal actual fun rememberDemoStoreFile(): StoreFile = remember {
    val preferences = NSUserDefaults(suiteName = "top.ltfan.m3x")
    val rootKey = "/top/ltfan/m3x/"
    val nodeKey = "demo/"
    DemoStoreFile(
        { key ->
            preferences.synchronize()
            val root = preferences.dictionaryForKey(rootKey)
            val node = root?.get(nodeKey) as? Map<*, *>
            node?.get(key) as? String
        },
        { key, value ->
            preferences.synchronize()
            val root = preferences.dictionaryForKey(rootKey).orEmpty().toMutableMap()
            val node = (root[nodeKey] as? Map<*, *>).orEmpty().toMutableMap()
            node[key] = value
            root[nodeKey] = node
            preferences.setObject(root, rootKey)
            check(preferences.synchronize()) { "Could not save demo settings" }
        },
    )
}

/** Initializes process-wide runtime options before creating a Compose window. */
fun initializeDemoRuntime() {
    initializeDemoRuntime {
        val preferences = NSUserDefaults(suiteName = "top.ltfan.m3x")
        preferences.synchronize()
        val root = preferences.dictionaryForKey("/top/ltfan/m3x/")
        val node = root?.get("demo/") as? Map<*, *>
        node?.get("appearance") as? String
    }
}
