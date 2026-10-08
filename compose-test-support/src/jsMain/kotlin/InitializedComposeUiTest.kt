package top.ltfan.m3x.compose.test

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlinx.coroutines.test.TestResult
import org.jetbrains.skiko.InternalSkikoApi
import org.jetbrains.skiko.wasm.awaitSkiko

@OptIn(InternalSkikoApi::class)
public actual fun runInitializedComposeUiTest(block: suspend ComposeUiTest.() -> Unit): TestResult =
    // JavaScript promises adopt the returned test promise, including its failures.
    awaitSkiko.then { runComposeUiTest(block = block) }.unsafeCast<TestResult>()
