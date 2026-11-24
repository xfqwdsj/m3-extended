package top.ltfan.m3x.demo.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import top.ltfan.m3x.demo.DemoApp
import top.ltfan.m3x.demo.initializeDemoRuntime

fun main() {
    initializeDemoRuntime()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "M3X Demo · 示例",
            state = WindowState(size = DpSize(1180.dp, 860.dp)),
        ) {
            DemoApp()
        }
    }
}
