package top.ltfan.m3x.demo

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import top.ltfan.m3x.datastore.StoreFile

@Composable
internal actual fun rememberDemoStoreFile(): StoreFile {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        val preferences = context.getSharedPreferences("m3x-demo", 0)
        DemoStoreFile(
            { preferences.getString(it, null) },
            { key, value ->
                check(preferences.edit().putString(key, value).commit()) {
                    "Could not save demo settings"
                }
            },
        )
    }
}

/** Initializes process-wide runtime options before the first activity composition. */
fun initializeDemoRuntime(context: Context) {
    initializeDemoRuntime {
        context.applicationContext.getSharedPreferences("m3x-demo", 0).getString("appearance", null)
    }
}
