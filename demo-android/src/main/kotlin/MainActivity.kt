package top.ltfan.m3x.demo.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import top.ltfan.m3x.demo.DemoApp
import top.ltfan.m3x.demo.initializeDemoRuntime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        initializeDemoRuntime(this)
        setContent { DemoApp() }
    }
}
