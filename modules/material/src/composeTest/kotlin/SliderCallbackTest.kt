package top.ltfan.m3x.material

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSliderState
import androidx.compose.material3.SliderState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering

class SliderCallbackTest {
    @Test fun horizontalStateReportsProposalsToItsOwner() = controlled(false)

    @Test fun verticalStateReportsProposalsToItsOwner() = controlled(true)

    private fun controlled(vertical: Boolean) = runComposeUiTest {
        val state = SliderState(0.25f)
        var proposal = -1f
        var changes = 0
        var finishes = 0
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    if (vertical) {
                        VerticalSlider(
                            state,
                            modifier = Modifier.testTag("slider"),
                            onValueChange = {
                                proposal = it
                                changes++
                            },
                            onValueChangeFinished = { finishes++ },
                            topToBottom = false,
                        )
                    } else {
                        Slider(
                            state,
                            modifier = Modifier.testTag("slider"),
                            onValueChange = {
                                proposal = it
                                changes++
                            },
                            onValueChangeFinished = { finishes++ },
                        )
                    }
                }
            }
        }
        onNodeWithTag("slider").performSemanticsAction(SemanticsActions.SetProgress) {
            assertTrue(it(0.75f))
        }
        runOnIdle {
            assertEquals(0.75f, proposal)
            assertEquals(0.25f, state.value)
            assertEquals(1, changes)
            assertEquals(1, finishes)
        }
    }

    @Test
    fun ownerCallbackUpdatesTheState() = runComposeUiTest {
        val state = SliderState(0.25f)
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    Slider(
                        state,
                        onValueChange = { state.value = it },
                        modifier = Modifier.testTag("slider"),
                    )
                }
            }
        }
        onNodeWithTag("slider").performSemanticsAction(SemanticsActions.SetProgress) { it(0.75f) }
        runOnIdle { assertEquals(0.75f, state.value) }
    }

    @Test
    fun rangeStateReportsProposalsWithoutMutatingOwnedValues() = runComposeUiTest {
        val state = RangeSliderState(0.2f, 0.8f)
        var proposal: ClosedFloatingPointRange<Float>? = null
        var changes = 0
        var finishes = 0
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    RangeSlider(
                        state,
                        onValueChange = {
                            proposal = it
                            changes++
                        },
                        onValueChangeFinished = { finishes++ },
                    )
                }
            }
        }
        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))[0]
            .performSemanticsAction(SemanticsActions.SetProgress) { assertTrue(it(0.4f)) }
        runOnIdle {
            val proposed = proposal
            assertNotNull(proposed)
            assertEquals(0.4f, proposed.start, 1e-6f)
            assertEquals(0.8f, proposed.endInclusive, 1e-6f)
            assertEquals(0.2f, state.startValue)
            assertEquals(0.8f, state.endValue)
            assertEquals(1, changes)
            assertEquals(1, finishes)
        }
    }
}
