package top.ltfan.m3x.settingspage.processor.util

import com.squareup.kotlinpoet.CodeBlock

fun CodeBlock.Companion.ofList(vararg elements: CodeBlock, function: String = "listOf"): CodeBlock {
    if (elements.isEmpty()) return CodeBlock.of("%L()", function)
    val block = CodeBlock.builder().add("%L(", function)
    elements.forEachIndexed { index, element ->
        if (index > 0) block.add(", ")
        block.add("%L", element)
    }
    block.add(")")
    return block.build()
}
