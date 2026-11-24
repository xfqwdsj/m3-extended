package top.ltfan.m3x.backdrop.benchmark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import top.ltfan.m3x.backdrop.benchmark.scenarios.ScenarioController
import top.ltfan.m3x.backdrop.benchmark.scenarios.ScenarioId
import top.ltfan.m3x.backdrop.benchmark.scenarios.ScenarioScene

/** Profileable Android host for the same scenarios used by the JVM runner. */
class BenchmarkActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scenario = ScenarioId.valueOf(intent.getStringExtra("scenario") ?: "SharedBounds")
        setContent { ScenarioScene(remember { ScenarioController(scenario) }) }
    }
}
