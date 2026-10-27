package top.ltfan.m3x.material.theming

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import top.ltfan.m3x.compose.ui.visual.DefaultEnterExitBlurRadius
import top.ltfan.m3x.material.theme.M3xShapeFactory
import top.ltfan.m3x.material.theme.RoundedM3xShapeFactory
import top.ltfan.m3x.material.theme.extended

class BaseThemeTest {
    @Test
    fun independentRootUsesRoundedShapesAndNestedThemesInheritItsFactory() = runComposeUiTest {
        val alternative = object : M3xShapeFactory by RoundedM3xShapeFactory {}
        setContent {
            M3xBaseTheme {
                assertSame(RoundedM3xShapeFactory, MaterialTheme.extended.shapes.factory)
                assertEquals(
                    DefaultEnterExitBlurRadius,
                    MaterialTheme.extended.enterExitBlurRadius,
                )
                M3xBaseTheme(rootShapeFactory = alternative) {
                    assertSame(RoundedM3xShapeFactory, MaterialTheme.extended.shapes.factory)
                }
                M3xBaseTheme(shapeFactory = alternative) {
                    assertSame(alternative, MaterialTheme.extended.shapes.factory)
                    M3xBaseTheme {
                        assertSame(alternative, MaterialTheme.extended.shapes.factory)
                    }
                }
            }
        }
    }

    @Test
    fun explicitPresetAndFactoryTakePrecedenceOverTheRootDefault() = runComposeUiTest {
        val alternative = object : M3xShapeFactory by RoundedM3xShapeFactory {}
        val preset = roundedShapes()
        setContent {
            M3xBaseTheme(shapeFactory = alternative) {
                assertSame(alternative, MaterialTheme.extended.shapes.factory)
            }
            M3xBaseTheme(
                shapes = preset,
                shapeFactory = alternative,
                rootShapeFactory = alternative,
            ) {
                assertSame(RoundedM3xShapeFactory, MaterialTheme.extended.shapes.factory)
                assertSame(preset.medium, MaterialTheme.shapes.medium)
            }
        }
    }

    @Test
    fun changingTheRootDefaultUpdatesThePresetAtTheSameThemePosition() = runComposeUiTest {
        val replacement = object : M3xShapeFactory by RoundedM3xShapeFactory {}
        var factory by mutableStateOf<M3xShapeFactory>(RoundedM3xShapeFactory)
        var observed: M3xShapeFactory? = null
        setContent {
            M3xBaseTheme(rootShapeFactory = factory) {
                observed = MaterialTheme.extended.shapes.factory
            }
        }
        runOnIdle {
            assertSame(RoundedM3xShapeFactory, observed)
            factory = replacement
        }
        waitForIdle()
        runOnIdle { assertSame(replacement, observed) }
    }
}
