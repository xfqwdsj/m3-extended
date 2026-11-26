package top.ltfan.m3x.backdrop.internal

import kotlinx.coroutines.CoroutineScope

/** Renderer-scoped services available to primitives that prepare work across frames. */
internal interface BackdropPrimitiveRuntime {
    val scope: () -> CoroutineScope
}
