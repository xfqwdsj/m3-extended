package top.ltfan.m3x.backdrop.effects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RenderEffect
import org.intellij.lang.annotations.Language
import top.ltfan.m3x.backdrop.BackdropEffectContext
import top.ltfan.m3x.backdrop.BackdropEffectScope
import top.ltfan.m3x.backdrop.BackdropEffectScopeImpl
import top.ltfan.m3x.backdrop.BackdropRenderDsl
import top.ltfan.m3x.backdrop.BackdropSampling
import top.ltfan.m3x.backdrop.BuiltinRuntimeShader
import top.ltfan.m3x.backdrop.RuntimeShader
import top.ltfan.m3x.backdrop.internal.createRuntimeShaderEffect
import top.ltfan.m3x.backdrop.isRenderEffectSupported
import top.ltfan.m3x.backdrop.isRuntimeShaderSupported

/** Adds a platform render effect with its declared input sampling behavior. */
public fun BackdropEffectScope.effect(effect: RenderEffect, sampling: BackdropSampling) {
    effect(sampling) { effect }
}

/**
 * Registers a stage with its complete input sampling contract. The factory runs after the graph's
 * final storage extension and this stage's input bounds are resolved. Geometry uses surface-local
 * pixels; translate [BackdropEffectContext.inputBounds] by the extension's left/top margins when
 * constructing an effect in storage coordinates. Unsupported platform render effects skip this
 * stage and its factory. The factory constructs one effect and cannot register stages implicitly.
 * It owns any native chain it returns. Register sampled shaders as separate stages so the executor
 * can preserve their intermediate input images. [BackdropSampling.Identity] permits same-coordinate
 * stages to share native filter composition; other custom sampling retains an input image when
 * required by the backend.
 */
public fun BackdropEffectScope.effect(
    sampling: BackdropSampling,
    factory: @BackdropRenderDsl BackdropEffectContext.() -> RenderEffect,
) {
    if (!isRenderEffectSupported()) return
    val context = (this as BackdropEffectScopeImpl).context
    addEffect(sampling, requiresInputImage = sampling !== BackdropSampling.Identity) {
        context.factory()
    }
}

/**
 * Adds one runtime-shader stage with its complete sampling footprint. The executor supplies
 * resolved input and output domains and retains intermediate GPU images where required by the
 * backend.
 */
public fun BackdropEffectScope.runtimeShaderEffect(
    key: String,
    @Language("AGSL") shaderString: String,
    uniformShaderName: String,
    sampling: BackdropSampling,
    block: RuntimeShader.() -> Unit,
): Unit = runtimeShaderEffect(key, shaderString, null, uniformShaderName, sampling, block)

internal fun BackdropEffectScope.runtimeShaderEffect(
    key: String,
    program: BuiltinRuntimeShader,
    uniformShaderName: String,
    sampling: BackdropSampling,
    block: RuntimeShader.() -> Unit,
) = runtimeShaderEffect(key, program.source, program, uniformShaderName, sampling, block)

private fun BackdropEffectScope.runtimeShaderEffect(
    key: String,
    source: String,
    program: BuiltinRuntimeShader?,
    uniformShaderName: String,
    sampling: BackdropSampling,
    block: RuntimeShader.() -> Unit,
) {
    if (!isRuntimeShaderSupported()) return
    val scope = this as BackdropEffectScopeImpl
    val stageKey = scope.nextRuntimeShaderEffectKey(key)
    effect(sampling) {
        val shader =
            if (program == null) obtainRuntimeShader(stageKey, source)
            else scope.obtainStageRuntimeShader(stageKey, program)
        createRuntimeShaderEffect(
            runtimeShader = shader.apply(block),
            uniformShaderName = uniformShaderName,
            inputBounds = inputBounds.translate(Offset(extension.left, extension.top)),
            outputBounds = scope.stageOutputBounds.translate(Offset(extension.left, extension.top)),
        )
    }
}
