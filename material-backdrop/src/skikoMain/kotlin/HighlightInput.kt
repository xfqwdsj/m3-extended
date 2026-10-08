package top.ltfan.m3x.material.backdrop.internal

import top.ltfan.backdrop.RuntimeShader
import top.ltfan.backdrop.asSkikoRuntimeShader

internal actual fun RuntimeShader.setHighlightInput(name: String, shader: RuntimeShader) {
    val child = shader.asSkikoRuntimeShader().makeShader()
    try {
        asSkikoRuntimeShader().child(name, child)
    } finally {
        child.close()
    }
}
