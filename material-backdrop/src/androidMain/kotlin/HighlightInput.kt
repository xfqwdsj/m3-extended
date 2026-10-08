package top.ltfan.m3x.material.backdrop.internal

import android.os.Build
import androidx.annotation.RequiresApi
import top.ltfan.backdrop.RuntimeShader
import top.ltfan.backdrop.asAndroidRuntimeShader

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal actual fun RuntimeShader.setHighlightInput(name: String, shader: RuntimeShader) {
    asAndroidRuntimeShader().setInputShader(name, shader.asAndroidRuntimeShader())
}
