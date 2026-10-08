package top.ltfan.m3x.settingspage.processor

import com.squareup.kotlinpoet.CodeBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import top.ltfan.m3x.settingspage.processor.util.ofList

class CodeBlockUtilTest {
    @Test
    fun emptyList() {
        val cb = CodeBlock.ofList()
        assertEquals("listOf()", cb.toString())
    }

    @Test
    fun singleElement() {
        val cb = CodeBlock.ofList(CodeBlock.of("1"))
        assertEquals("listOf(1)", cb.toString())
    }

    @Test
    fun multipleElements() {
        val cb =
            CodeBlock.ofList(
                CodeBlock.of("%S", "abc"),
                CodeBlock.of("'c'"),
                CodeBlock.of("123"),
                CodeBlock.of("true"),
            )
        assertEquals("listOf(\"abc\", 'c', 123, true)", cb.toString())
    }

    @Test
    fun customFunction() {
        val cb = CodeBlock.ofList(CodeBlock.of("1"), CodeBlock.of("2"), function = "mutableListOf")
        assertEquals("mutableListOf(1, 2)", cb.toString())
    }
}
