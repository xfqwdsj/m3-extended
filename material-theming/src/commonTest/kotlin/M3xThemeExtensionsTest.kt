package top.ltfan.m3x.material.theming

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousCapsule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.compose.ui.layout.CornerRadii
import top.ltfan.m3x.compose.ui.layout.WindowPresentation
import top.ltfan.m3x.material.backdrop.BackdropPresets
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropTheme
import top.ltfan.m3x.material.backdrop.theme.backdrop
import top.ltfan.m3x.material.bundle.M3xMaterialTheme
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.theme.CornerRadiiPolicy
import top.ltfan.m3x.material.theme.ExpressiveCornerRadii
import top.ltfan.m3x.material.theme.M3xCornerRadii
import top.ltfan.m3x.material.theme.M3xShapeExtensions
import top.ltfan.m3x.material.theme.M3xShapes
import top.ltfan.m3x.material.theme.RoundedM3xShapeFactory
import top.ltfan.m3x.material.theme.adaptiveCornerRadii
import top.ltfan.m3x.material.theme.extended
import top.ltfan.m3x.material.theme.toM3xShapes
import top.ltfan.m3x.material.theme.toMaterialShapes
import top.ltfan.m3x.material.theme.toShapeExtensions
import top.ltfan.m3x.material.theming.capsule.CapsuleM3xShapeFactory
import top.ltfan.m3x.material.theming.capsule.capsuleShapes

class M3xThemeExtensionsTest {
    @Test
    fun motionDefaultsInheritAndExplicitScopesRemainAuthoritative() = runInitializedComposeUiTest {
        val custom = object : MotionScheme by MotionScheme.standard() {}
        setContent {
            ProvideM3xMaterialRendering {
                M3xMaterialTheme {
                    assertSame(MotionScheme.expressive(), MaterialTheme.motionScheme)
                    M3xMaterialTheme {
                        assertSame(MotionScheme.expressive(), MaterialTheme.motionScheme)
                    }
                    M3xMaterialTheme(motionScheme = custom) {
                        assertSame(custom, MaterialTheme.motionScheme)
                        M3xMaterialTheme { assertSame(custom, MaterialTheme.motionScheme) }
                    }
                    MaterialTheme(motionScheme = custom) {
                        M3xMaterialTheme { assertSame(custom, MaterialTheme.motionScheme) }
                    }
                    assertSame(MotionScheme.expressive(), MaterialTheme.motionScheme)
                }
            }
        }
    }

    @Test
    fun entranceBlurRadiusInheritsAndCanBeScopedIndependently() = runInitializedComposeUiTest {
        setContent {
            ProvideM3xMaterialRendering {
                M3xMaterialTheme {
                    assertEquals(16.dp, MaterialTheme.extended.enterExitBlurRadius)
                    M3xMaterialTheme(enterExitBlurRadius = 20.dp) {
                        assertEquals(20.dp, MaterialTheme.extended.enterExitBlurRadius)
                        M3xMaterialTheme(backdrop = M3xBackdropTheme()) {
                            assertEquals(20.dp, MaterialTheme.extended.enterExitBlurRadius)
                        }
                        M3xMaterialTheme(enterExitBlurRadius = 0.dp) {
                            assertEquals(0.dp, MaterialTheme.extended.enterExitBlurRadius)
                        }
                        assertEquals(20.dp, MaterialTheme.extended.enterExitBlurRadius)
                    }
                    assertEquals(16.dp, MaterialTheme.extended.enterExitBlurRadius)
                }
            }
        }
    }

    @Test
    fun cornerPolicyInheritsAndCanBeOverriddenByNestedTheme() = runInitializedComposeUiTest {
        var root: M3xCornerRadii? = null
        var inherited: M3xCornerRadii? = null
        var overridden: M3xCornerRadii? = null
        setContent {
            ProvideM3xMaterialRendering {
                M3xMaterialTheme(cornerRadiiPolicy = CornerRadiiPolicy.Display) {
                    root = MaterialTheme.extended.shapes.cornerRadii
                    M3xMaterialTheme { inherited = MaterialTheme.extended.shapes.cornerRadii }
                    M3xMaterialTheme(cornerRadiiPolicy = CornerRadiiPolicy.Window) {
                        overridden = MaterialTheme.extended.shapes.cornerRadii
                    }
                }
            }
        }
        waitForIdle()
        assertEquals(CornerRadiiPolicy.Display, root?.policy)
        assertEquals(CornerRadiiPolicy.Display, inherited?.policy)
        assertEquals(CornerRadiiPolicy.Window, overridden?.policy)
    }

    @Test
    fun nestedThemesInheritTheWholePresetAndExplicitPresetOverridesIt() =
        runInitializedComposeUiTest {
            val fixed = capsuleShapes(ExpressiveCornerRadii.copy(containerRadius = 41.dp))
            val override = roundedShapes(ExpressiveCornerRadii.copy(containerRadius = 19.dp))
            var root: M3xShapeExtensions? = null
            var inherited: M3xShapeExtensions? = null
            var overridden: M3xShapeExtensions? = null

            setContent {
                ProvideM3xMaterialRendering {
                    M3xMaterialTheme(shapes = fixed) {
                        root = MaterialTheme.extended.shapes
                        M3xMaterialTheme { inherited = MaterialTheme.extended.shapes }
                        M3xMaterialTheme(shapes = override) {
                            overridden = MaterialTheme.extended.shapes
                        }
                    }
                }
            }
            waitForIdle()
            assertEquals(fixed.toShapeExtensions(), root)
            assertEquals(fixed.toShapeExtensions(), inherited)
            assertEquals(override.toShapeExtensions(), overridden)
            assertEquals(41.dp, root?.cornerRadii?.containerRadius)
        }

    @Test
    fun nestedFactoryUsesInheritedRadiiAndExplicitPresetTakesPrecedence() =
        runInitializedComposeUiTest {
            val fixed = capsuleShapes(ExpressiveCornerRadii.copy(containerRadius = 41.dp))
            val explicit = roundedShapes(ExpressiveCornerRadii.copy(containerRadius = 19.dp))
            var configured: M3xShapeExtensions? = null
            var presetWins: M3xShapeExtensions? = null

            setContent {
                ProvideM3xMaterialRendering {
                    M3xMaterialTheme(shapes = fixed) {
                        M3xMaterialTheme(shapeFactory = RoundedM3xShapeFactory) {
                            configured = MaterialTheme.extended.shapes
                        }
                        M3xMaterialTheme(shapes = explicit, shapeFactory = CapsuleM3xShapeFactory) {
                            presetWins = MaterialTheme.extended.shapes
                        }
                    }
                }
            }
            waitForIdle()

            assertSame(RoundedM3xShapeFactory, configured?.factory)
            assertEquals(fixed.cornerRadii, configured?.cornerRadii)
            assertEquals(explicit.toShapeExtensions(), presetWins)
        }

    @Test
    fun backdropOverrideKeepsInheritedShapePreset() = runInitializedComposeUiTest {
        val fixed = capsuleShapes(ExpressiveCornerRadii.copy(containerRadius = 41.dp))
        var observedShapes: M3xShapeExtensions? = null
        var observedBackdrop: M3xBackdropTheme? = null
        setContent {
            ProvideM3xMaterialRendering {
                M3xMaterialTheme(shapes = fixed) {
                    M3xMaterialTheme(backdrop = BackdropPresets.MaterialBlur) {
                        observedShapes = MaterialTheme.extended.shapes
                        observedBackdrop = MaterialTheme.extended.backdrop
                    }
                }
            }
        }
        waitForIdle()
        assertEquals(fixed.toShapeExtensions(), observedShapes)
        assertEquals(BackdropPresets.MaterialBlur, observedBackdrop)
    }

    @Test
    fun rootThemeSelectsAdaptiveCapsule() = runInitializedComposeUiTest {
        var observed: M3xShapeExtensions? = null
        setContent {
            ProvideM3xMaterialRendering {
                M3xMaterialTheme { observed = MaterialTheme.extended.shapes }
            }
        }
        waitForIdle()
        assertNotNull(observed?.cornerRadii)
        assertSame(ContinuousCapsule, observed.full)
    }

    @Test
    fun changingRadiusRecomposes() = runInitializedComposeUiTest {
        var radius by mutableStateOf(24.dp)
        var observed: M3xCornerRadii? = null
        setContent {
            ProvideM3xMaterialRendering {
                M3xMaterialTheme(
                    shapes =
                        capsuleShapes(
                            adaptiveCornerRadii(
                                CornerRadii(topLeft = radius),
                                null,
                                WindowPresentation.Fullscreen,
                                policy = CornerRadiiPolicy.Display,
                            ),
                        ),
                ) {
                    observed = MaterialTheme.extended.shapes.cornerRadii
                }
            }
        }
        waitForIdle()
        assertEquals(24.dp, observed?.containerRadius)
        assertEquals(21.dp, observed?.extraLarge)
        runOnIdle { radius = 48.dp }
        waitForIdle()
        assertEquals(48.dp, observed?.containerRadius)
        assertEquals(42.dp, observed?.extraLarge)
    }

    @Test
    fun adapterMapsAllEightRolesAndCustomShapesDoNotInventMetadata() {
        val source =
            M3xShapes(
                extraSmall = RoundedCornerShape(1.dp),
                small = RoundedCornerShape(2.dp),
                medium = RoundedCornerShape(3.dp),
                large = RoundedCornerShape(4.dp),
                extraLarge = RoundedCornerShape(5.dp),
                largeIncreased = RoundedCornerShape(6.dp),
                extraLargeIncreased = RoundedCornerShape(7.dp),
                extraExtraLarge = RoundedCornerShape(8.dp),
            )
        val mapped = source.toMaterialShapes()
        assertSame(source.extraSmall, mapped.extraSmall)
        assertSame(source.small, mapped.small)
        assertSame(source.medium, mapped.medium)
        assertSame(source.large, mapped.large)
        assertSame(source.extraLarge, mapped.extraLarge)
        assertSame(source.largeIncreased, mapped.largeIncreased)
        assertSame(source.extraLargeIncreased, mapped.extraLargeIncreased)
        assertSame(source.extraExtraLarge, mapped.extraExtraLarge)
        assertNull(source.cornerRadii)
    }

    @Test
    fun customPresetWithoutRadiusMetadataKeepsMetadataNullInTheme() = runInitializedComposeUiTest {
        val custom = M3xShapes(full = RoundedCornerShape(17.dp))
        var observed: M3xShapeExtensions? = null
        setContent {
            ProvideM3xMaterialRendering {
                M3xMaterialTheme(shapes = custom) { observed = MaterialTheme.extended.shapes }
            }
        }
        waitForIdle()
        assertSame(custom.full, observed?.full)
        assertNull(observed?.cornerRadii)
    }

    @Test
    fun defaultM3xShapesMirrorUpstreamFiniteRolesWithoutRadiusMetadata() =
        runInitializedComposeUiTest {
            val upstream = Shapes()
            var extensions: M3xShapeExtensions? = null
            var observed: M3xShapes? = null
            setContent {
                ProvideM3xMaterialRendering {
                    MaterialTheme(shapes = upstream) {
                        extensions = MaterialTheme.extended.shapes
                        observed = extensions.toM3xShapes(MaterialTheme.shapes)
                    }
                }
            }
            waitForIdle()
            assertNotNull(observed)
            assertSame(upstream.extraSmall, observed.extraSmall)
            assertSame(upstream.small, observed.small)
            assertSame(upstream.medium, observed.medium)
            assertSame(upstream.large, observed.large)
            assertSame(upstream.extraLarge, observed.extraLarge)
            assertSame(upstream.largeIncreased, observed.largeIncreased)
            assertSame(upstream.extraLargeIncreased, observed.extraLargeIncreased)
            assertSame(upstream.extraExtraLarge, observed.extraExtraLarge)
            assertNull(observed.cornerRadii)
            assertNull(extensions?.cornerRadii)
        }

    @Test
    fun nativeThemeOverrideKeepsExtensionShapesAndPresetMetadata() = runInitializedComposeUiTest {
        val custom =
            M3xShapes(
                extraSmall = RoundedCornerShape(11.dp),
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(13.dp),
                large = RoundedCornerShape(14.dp),
                extraLarge = RoundedCornerShape(15.dp),
                largeIncreased = RoundedCornerShape(16.dp),
                extraLargeIncreased = RoundedCornerShape(17.dp),
                extraExtraLarge = RoundedCornerShape(18.dp),
                full = RoundedCornerShape(39.dp),
                cornerRadii = ExpressiveCornerRadii.copy(containerRadius = 41.dp),
            )
        var materialShapes: Shapes? = null
        var extendedShapes: M3xShapeExtensions? = null
        var reassembled: M3xShapes? = null
        var radii: M3xCornerRadii? = ExpressiveCornerRadii
        val nativeOverride =
            Shapes(
                extraSmall = RoundedCornerShape(21.dp),
                small = RoundedCornerShape(22.dp),
                medium = RoundedCornerShape(23.dp),
                large = RoundedCornerShape(24.dp),
                extraLarge = RoundedCornerShape(25.dp),
                largeIncreased = RoundedCornerShape(26.dp),
                extraLargeIncreased = RoundedCornerShape(27.dp),
                extraExtraLarge = RoundedCornerShape(28.dp),
            )
        setContent {
            ProvideM3xMaterialRendering {
                M3xMaterialTheme(shapes = custom) {
                    MaterialTheme(shapes = nativeOverride) {
                        M3xMaterialTheme(backdrop = BackdropPresets.MaterialBlur) {
                            materialShapes = MaterialTheme.shapes
                            extendedShapes = MaterialTheme.extended.shapes
                            reassembled = extendedShapes.toM3xShapes(MaterialTheme.shapes)
                            radii = MaterialTheme.extended.shapes.cornerRadii
                        }
                    }
                }
            }
        }
        waitForIdle()
        assertSame(nativeOverride.extraSmall, materialShapes?.extraSmall)
        assertSame(nativeOverride.small, materialShapes?.small)
        assertSame(nativeOverride.medium, materialShapes?.medium)
        assertSame(nativeOverride.large, materialShapes?.large)
        assertSame(nativeOverride.extraLarge, materialShapes?.extraLarge)
        assertSame(nativeOverride.largeIncreased, materialShapes?.largeIncreased)
        assertSame(nativeOverride.extraLargeIncreased, materialShapes?.extraLargeIncreased)
        assertSame(nativeOverride.extraExtraLarge, materialShapes?.extraExtraLarge)
        assertSame(nativeOverride.extraSmall, reassembled?.extraSmall)
        assertSame(nativeOverride.small, reassembled?.small)
        assertSame(nativeOverride.medium, reassembled?.medium)
        assertSame(nativeOverride.large, reassembled?.large)
        assertSame(nativeOverride.extraLarge, reassembled?.extraLarge)
        assertSame(nativeOverride.largeIncreased, reassembled?.largeIncreased)
        assertSame(nativeOverride.extraLargeIncreased, reassembled?.extraLargeIncreased)
        assertSame(nativeOverride.extraExtraLarge, reassembled?.extraExtraLarge)
        assertSame(custom.full, extendedShapes?.full)
        assertSame(custom.none, extendedShapes?.none)
        assertSame(custom.container, extendedShapes?.container)
        assertEquals(custom.cornerRadii, radii)
    }
}
