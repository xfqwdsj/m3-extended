package top.ltfan.m3x.demo.ios

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import top.ltfan.m3x.demo.DemoApp
import top.ltfan.m3x.demo.initializeDemoRuntime

/** Creates the shared M3X demo inside an iOS view controller. */
@Suppress("unused", "FunctionName")
fun MainViewController(): UIViewController {
    initializeDemoRuntime()
    return ComposeUIViewController { DemoApp() }
}
