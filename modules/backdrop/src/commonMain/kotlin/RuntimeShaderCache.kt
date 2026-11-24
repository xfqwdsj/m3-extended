package top.ltfan.m3x.backdrop

import org.intellij.lang.annotations.Language

public sealed interface RuntimeShaderCache {

    /**
     * Reuses the shader for this key and source in the current cache. A source change replaces it.
     */
    public fun obtainRuntimeShader(key: String, @Language("AGSL") string: String): RuntimeShader
}

internal class RuntimeShaderCacheImpl : RuntimeShaderCache {

    private class Entry(val source: String, val shader: RuntimeShader)

    private val runtimeShaders = mutableMapOf<String, Entry>()

    override fun obtainRuntimeShader(key: String, string: String): RuntimeShader =
        obtain(key, string) { RuntimeShader(string) }

    fun obtainRuntimeShader(key: String, program: BuiltinRuntimeShader): RuntimeShader =
        obtain(key, program.source) { program.create() }

    private inline fun obtain(
        key: String,
        source: String,
        create: () -> RuntimeShader,
    ): RuntimeShader {
        val cached = runtimeShaders[key]
        if (cached != null && cached.source == source) return cached.shader
        return create().also { runtimeShaders[key] = Entry(source, it) }
    }

    fun clear() {
        runtimeShaders.clear()
    }
}

/** Keeps owner-local instances whose keys fully identify the generated source. */
internal class RuntimeShaderVariantCache<K : Any> {
    private val shaders = mutableMapOf<K, RuntimeShader>()

    inline fun obtain(key: K, source: () -> String): RuntimeShader =
        shaders.getOrPut(key) { RuntimeShader(source()) }

    fun clear() {
        shaders.clear()
    }
}
