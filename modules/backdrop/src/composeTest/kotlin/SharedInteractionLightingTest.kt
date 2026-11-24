package top.ltfan.m3x.backdrop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import top.ltfan.m3x.backdrop.interaction.InteractionLight
import top.ltfan.m3x.backdrop.interaction.InteractionRegistry
import top.ltfan.m3x.backdrop.interaction.LocalInteractionRegistry
import top.ltfan.m3x.backdrop.interaction.ProvideInteractionLighting
import top.ltfan.m3x.backdrop.interaction.interactionLightSource

class SharedInteractionLightingTest {
    @Test
    fun nestedSourcesShareOnePhysicalPointerContribution() = runRenderTest {
        lateinit var registry: InteractionRegistry
        setContent {
            ProvideInteractionLighting {
                registry = LocalInteractionRegistry.current!!
                Box(Modifier.size(120.dp).testTag("nested-pointer-root")) {
                    Box(
                        Modifier.fillMaxSize().interactionLightSource {
                            InteractionLight.Config(color = Color.Red, radius = 100.dp)
                        },
                    ) {
                        Box(
                            Modifier.offset(20.dp, 20.dp).size(80.dp).interactionLightSource {
                                InteractionLight.Config(color = Color.Blue, radius = 100.dp)
                            },
                        )
                    }
                }
            }
        }
        waitForIdle()

        onNodeWithTag("nested-pointer-root").performTouchInput {
            down(position = Offset(60f, 60f))
        }
        waitForIdle()
        assertEquals(
            1,
            registry.lights.size,
            "nested sources contribute one light for the same physical pointer",
        )
    }

    @Test
    fun distinctPointersKeepDistinctNestedContributions() = runRenderTest {
        lateinit var registry: InteractionRegistry
        setContent {
            ProvideInteractionLighting {
                registry = LocalInteractionRegistry.current!!
                Box(Modifier.size(120.dp).testTag("multi-pointer-root")) {
                    Box(
                        Modifier.fillMaxSize().interactionLightSource {
                            InteractionLight.Config(color = Color.Red, radius = 100.dp)
                        },
                    ) {
                        Box(
                            Modifier.offset(20.dp, 20.dp).size(80.dp).interactionLightSource {
                                InteractionLight.Config(color = Color.Blue, radius = 100.dp)
                            },
                        )
                    }
                }
            }
        }
        waitForIdle()

        onNodeWithTag("multi-pointer-root").performTouchInput {
            down(pointerId = 1, position = Offset(40f, 40f))
            down(pointerId = 2, position = Offset(80f, 80f))
        }
        waitForIdle()
        assertEquals(2, registry.lights.size, "different pointer ids retain separate contributions")
    }

    @Test
    fun movingPointerAfterNestedChildDetachesRestoresParentContribution() = runRenderTest {
        lateinit var registry: InteractionRegistry
        val showChild = mutableStateOf(true)
        setContent {
            ProvideInteractionLighting {
                registry = LocalInteractionRegistry.current!!
                Box(Modifier.size(120.dp).testTag("child-exit-root")) {
                    Box(
                        Modifier.fillMaxSize().interactionLightSource {
                            InteractionLight.Config(color = Color.Red, radius = 100.dp)
                        },
                    ) {
                        if (showChild.value) {
                            Box(
                                Modifier.offset(20.dp, 20.dp).size(80.dp).interactionLightSource {
                                    InteractionLight.Config(color = Color.Blue, radius = 100.dp)
                                },
                            )
                        }
                    }
                }
            }
        }
        waitForIdle()
        onNodeWithTag("child-exit-root").performTouchInput {
            down(position = Offset(60f, 60f))
        }
        waitForIdle()
        assertEquals(1, registry.lights.size, "the child owns the shared contribution")

        runOnIdle { showChild.value = false }
        waitForIdle()
        assertEquals(0, registry.lights.size, "detaching the winning child removes its entry")
        onNodeWithTag("child-exit-root").performTouchInput { moveTo(Offset(70f, 65f)) }
        waitForIdle()
        assertEquals(
            1,
            registry.lights.size,
            "the parent restores its entry on the next pointer move",
        )
    }

    @Test
    fun movingPointerAfterNestedChildBecomesNoneRestoresParentContribution() = runRenderTest {
        lateinit var registry: InteractionRegistry
        val childLight =
            mutableStateOf<InteractionLight>(
                InteractionLight.Config(color = Color.Blue, radius = 100.dp),
            )
        setContent {
            ProvideInteractionLighting {
                registry = LocalInteractionRegistry.current!!
                Box(Modifier.size(120.dp).testTag("child-none-root")) {
                    Box(
                        Modifier.fillMaxSize().interactionLightSource {
                            InteractionLight.Config(color = Color.Red, radius = 100.dp)
                        },
                    ) {
                        Box(
                            Modifier.offset(20.dp, 20.dp).size(80.dp).interactionLightSource {
                                childLight.value
                            },
                        )
                    }
                }
            }
        }
        waitForIdle()
        onNodeWithTag("child-none-root").performTouchInput {
            down(position = Offset(60f, 60f))
        }
        waitForIdle()
        assertEquals(1, registry.lights.size, "nested sources share one active pointer light")

        runOnIdle { childLight.value = InteractionLight.None }
        waitForIdle()
        assertEquals(0, registry.lights.size, "None clears the child's winning entry")
        onNodeWithTag("child-none-root").performTouchInput { moveTo(Offset(70f, 65f)) }
        waitForIdle()
        assertEquals(1, registry.lights.size, "the parent restores its entry on the next move")
        assertEquals(Color.Red, registry.lights.single().color)
    }

    @Test
    fun clearingParentSourceDoesNotRemoveNestedChildWinner() = runRenderTest {
        lateinit var registry: InteractionRegistry
        val parentLight =
            mutableStateOf<InteractionLight>(
                InteractionLight.Config(color = Color.Red, radius = 100.dp),
            )
        setContent {
            ProvideInteractionLighting {
                registry = LocalInteractionRegistry.current!!
                Box(Modifier.size(120.dp).testTag("parent-exit-root")) {
                    Box(Modifier.fillMaxSize().interactionLightSource { parentLight.value }) {
                        Box(
                            Modifier.offset(20.dp, 20.dp).size(80.dp).interactionLightSource {
                                InteractionLight.Config(color = Color.Blue, radius = 100.dp)
                            },
                        )
                    }
                }
            }
        }
        waitForIdle()
        onNodeWithTag("parent-exit-root").performTouchInput {
            down(position = Offset(60f, 60f))
        }
        waitForIdle()
        val childEntry = registry.lights.single()

        runOnIdle { parentLight.value = InteractionLight.None }
        waitForIdle()
        assertEquals(1, registry.lights.size, "parent cleanup leaves the child's contribution")
        assertSame(childEntry, registry.lights.single(), "the child winner remains registered")
    }
}
