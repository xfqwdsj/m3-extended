package top.ltfan.m3x.demo.macos

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import platform.AppKit.NSApplication
import platform.AppKit.NSApplicationDelegateProtocol
import platform.darwin.NSObject
import top.ltfan.m3x.compose.ui.layout.ProvideWindowCornerRadii
import top.ltfan.m3x.demo.DemoApp
import top.ltfan.m3x.demo.initializeDemoRuntime

private class DemoApplicationDelegate : NSObject(), NSApplicationDelegateProtocol {
    override fun applicationShouldTerminateAfterLastWindowClosed(sender: NSApplication): Boolean =
        true
}

fun main() {
    initializeDemoRuntime()
    val app = NSApplication.sharedApplication
    // AppKit retains delegates weakly, so keep the delegate alive for the application lifetime.
    val delegate = DemoApplicationDelegate()
    app.delegate = delegate
    Window(title = "M3X Demo · 示例", size = DpSize(1180.dp, 860.dp)) {
        ProvideWindowCornerRadii(window) { DemoApp() }
    }
    app.activateIgnoringOtherApps(true)
    app.run()
}
