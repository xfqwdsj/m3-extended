package top.ltfan.m3x.demo

import kotlin.test.Test
import kotlin.test.assertEquals

class DemoRuntimeTest {
    @Test
    fun olderPreferencesKeepTheDefaultComposer() {
        val applied = mutableListOf<Boolean>()
        DemoRuntimeInitializer(applied::add).initialize { "{}" }
        assertEquals(listOf(false), applied)
    }

    @Test
    fun savedComposerIsAppliedOnlyOncePerProcess() {
        val applied = mutableListOf<Boolean>()
        val initializer = DemoRuntimeInitializer(applied::add)
        initializer.initialize { """{"runtime":{"linkComposer":true}}""" }
        initializer.initialize { error("Recreating a window must not reload runtime options") }
        assertEquals(listOf(true), applied)
    }

    @Test
    fun aNewProcessAppliesTheChangedPreference() {
        val applied = mutableListOf<Boolean>()
        DemoRuntimeInitializer(applied::add).initialize {
            """{"runtime":{"linkComposer":true}}"""
        }
        DemoRuntimeInitializer(applied::add).initialize {
            """{"runtime":{"linkComposer":false}}"""
        }
        assertEquals(listOf(true, false), applied)
    }
}
