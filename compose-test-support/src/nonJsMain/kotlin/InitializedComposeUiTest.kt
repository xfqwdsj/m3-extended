package top.ltfan.m3x.compose.test

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlinx.coroutines.test.TestResult

public actual fun runInitializedComposeUiTest(block: suspend ComposeUiTest.() -> Unit): TestResult =
    runComposeUiTest(block = block)
