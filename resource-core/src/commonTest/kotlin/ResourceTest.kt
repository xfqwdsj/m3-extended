package top.ltfan.m3x.resource

import kotlin.test.Test
import kotlin.test.assertEquals

class ResourceTest {
    @Test
    fun globalResourcesCanBeReadWithoutCompose() {
        val static: Resource.Global<String> = Resource.Global.Static("ready")
        var calls = 0
        val dynamic: Resource.Global<Int> = Resource.Global.Dynamic { ++calls }
        val lazy: Resource.Global<Int> = Resource.Global.Lazy { ++calls }
        assertEquals("ready", static.value)
        assertEquals(1, dynamic.value)
        assertEquals(2, dynamic.value)
        assertEquals(3, lazy.value)
        assertEquals(3, lazy.value)
    }
}
