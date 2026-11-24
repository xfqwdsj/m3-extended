package top.ltfan.m3x.backdrop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import top.ltfan.m3x.backdrop.backdrops.rememberCanvasBackdrop
import top.ltfan.m3x.backdrop.effects.runtimeShaderEffect

class RuntimeShaderCacheTest : PlatformGraphicsTest() {
    @Test
    fun processSlotRetainsShadersAcrossUpdatesAndReleasesThemWhenOmitted() = runRenderTest {
        val gain = mutableFloatStateOf(1f)
        val enabled = mutableStateOf(true)
        val observed = mutableListOf<RuntimeShader>()
        setContent {
            val source = rememberCanvasBackdrop { drawRect(Color.Blue) }
            Box(
                Modifier.size(40.dp).drawBackdrop(source, { RectangleShape }) {
                    if (enabled.value)
                        effects {
                            runtimeShaderEffect(
                                "gain",
                                "uniform shader content; uniform float gain; half4 main(float2 p) { return content.eval(p) * gain; }",
                                "content",
                                BackdropSampling.Identity,
                            ) {
                                observed += this
                                setFloatUniform("gain", gain.floatValue)
                            }
                        }
                },
            )
        }
        waitForIdle()
        val initial = observed.last()
        repeat(3) { i ->
            runOnIdle { gain.floatValue = .2f + i * .1f }
            waitForIdle()
            assertSame(initial, observed.last())
        }
        runOnIdle { enabled.value = false }
        waitForIdle()
        val disabledCount = observed.size
        runOnIdle { gain.floatValue = .9f }
        waitForIdle()
        assertEquals(disabledCount, observed.size)
        runOnIdle { enabled.value = true }
        waitForIdle()
        assertNotSame(initial, observed.last())
    }

    @Test
    fun uniformChangesReuseShaderButSourceChangesReplaceIt() {
        val cache = RuntimeShaderCacheImpl()
        val source = "uniform float gain; half4 main(float2 p) { return half4(gain); }"
        val first = cache.obtainRuntimeShader("effect", source)
        repeat(180) { frame ->
            val shader = cache.obtainRuntimeShader("effect", source)
            shader.setFloatUniform("gain", frame / 180f)
            assertSame(first, shader)
        }
        val replacement =
            cache.obtainRuntimeShader("effect", "half4 main(float2 p) { return half4(0.0); }")
        assertNotSame(first, replacement)
    }

    @Test
    fun lazySourcesAreBuiltOncePerKeyUntilCacheIsReleased() {
        val cache = RuntimeShaderVariantCache<String>()
        var builds = 0
        val source = {
            builds++
            "half4 main(float2 p) { return half4(1.0); }"
        }
        val first = cache.obtain("capacity4", source)
        repeat(180) { assertSame(first, cache.obtain("capacity4", source)) }
        assertEquals(1, builds)
        assertNotSame(first, cache.obtain("capacity8", source))
        assertEquals(2, builds)
        cache.clear()
        assertNotSame(first, cache.obtain("capacity4", source))
        assertEquals(3, builds)
    }

    @Test
    fun failedConstructionPreservesExistingShaderAndAllowsRetry() {
        val cache = RuntimeShaderCacheImpl()
        val source = "half4 main(float2 p) { return half4(1.0); }"
        val shader = cache.obtainRuntimeShader("effect", source)
        assertFails { cache.obtainRuntimeShader("effect", "invalid AGSL") }
        assertSame(shader, cache.obtainRuntimeShader("effect", source))

        val variants = RuntimeShaderVariantCache<Int>()
        assertFails { variants.obtain(4) { "invalid AGSL" } }
        val replacement = variants.obtain(4) { source }
        assertSame(replacement, variants.obtain(4) { error("Cache hit rebuilt source") })
    }

    @Test
    fun builtinsReuseSourcesWithinAnOwnerAndCreateSeparateShaderInstances() {
        val cache = RuntimeShaderCacheImpl()
        val source = "uniform float gain; half4 main(float2 p) { return half4(gain); }"
        val program = BuiltinRuntimeShader(source)
        val shader = cache.obtainRuntimeShader("first", program)
        assertSame(shader, cache.obtainRuntimeShader("first", source))
        assertSame(shader, cache.obtainRuntimeShader("first", program))
        assertNotSame(shader, cache.obtainRuntimeShader("second", program))
        assertNotSame(shader, RuntimeShaderCacheImpl().obtainRuntimeShader("first", program))
    }

    @Test
    fun repeatedRuntimeShaderKeysGetDistinctStageCacheEntries() {
        val scope =
            object : BackdropEffectScopeImpl() {
                    override val shape = RectangleShape
                }
                .apply { size = Size(10f, 10f) }
        val shaders = mutableListOf<RuntimeShader>()
        val source =
            "uniform shader content; uniform float value; half4 main(float2 p) { return content.eval(p) * value; }"

        scope.resolveEffects {
            repeat(2) {
                runtimeShaderEffect(
                    key = "same",
                    shaderString = source,
                    uniformShaderName = "content",
                    sampling = BackdropSampling.Identity,
                ) {
                    shaders += this
                    setFloatUniform("value", 1f)
                }
            }
        }

        assertEquals(2, shaders.size)
        assertNotSame(shaders[0], shaders[1])
    }

    @Test
    fun refreshRecreatesValuesAndKeepsIsolatedShaderNamespaces() {
        val resources = PrimitiveResources { error("No layers are requested") }
        var disposed = 0
        val text = "half4 main(float2 p) { return half4(1.0); }"
        val pool =
            resources.resourcePool<Int, RuntimeShader>(dispose = { disposed++ }) {
                obtainRuntimeShader("program", text)
            }
        val first = pool.getOrCreate(1)
        assertNotSame(first, pool.getOrCreate(2))
        resources.releaseLayers()
        assertEquals(2, disposed)
        assertEquals(2, pool.size)
        assertSame(first, pool.getOrCreate(1))
        pool.retainOnly(emptySet())
        assertNotSame(first, pool.getOrCreate(1))
        resources.close()
        assertEquals(4, disposed)
    }
}
