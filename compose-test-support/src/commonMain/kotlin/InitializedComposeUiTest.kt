package top.ltfan.m3x.compose.test

import androidx.compose.ui.test.ComposeUiTest
import kotlinx.coroutines.test.TestResult

/** Starts the test only after the platform graphics runtime is available. */
public expect fun runInitializedComposeUiTest(block: suspend ComposeUiTest.() -> Unit): TestResult
