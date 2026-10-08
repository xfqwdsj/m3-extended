package top.ltfan.m3x.demo.web

import androidx.compose.ui.window.ComposeViewport
import top.ltfan.m3x.demo.DemoApp
import top.ltfan.m3x.demo.initializeDemoRuntime

fun main() {
    initializeDemoRuntime()
    ComposeViewport { DemoApp() }
}
