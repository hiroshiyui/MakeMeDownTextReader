package org.ghostsinthelab.app.makedown.reader.markdown

import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

object MarkdownAst {
    private val flavour = GFMFlavourDescriptor()
    private val parser = MarkdownParser(flavour)

    fun parse(source: String): ASTNode = parser.buildMarkdownTreeFromString(source)
}
