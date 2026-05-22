package com.intellij.plugin.applescript.lang.formatter

import com.intellij.formatting.Alignment
import com.intellij.formatting.Block
import com.intellij.formatting.Indent
import com.intellij.formatting.Spacing
import com.intellij.formatting.Wrap
import com.intellij.formatting.WrapType
import com.intellij.formatting.templateLanguages.BlockWithParent
import com.intellij.lang.ASTNode
import com.intellij.plugin.applescript.AppleScriptLanguage
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.formatter.common.AbstractBlock

class AppleScriptBlock(
    node: ASTNode,
    wrap: Wrap?,
    alignment: Alignment?,
    private val mySettings: CodeStyleSettings,
) : AbstractBlock(node, wrap, alignment),
    BlockWithParent {

    private val myIndentProcessor: AppleScriptIndentProcessor =
        AppleScriptIndentProcessor(mySettings.getCommonSettings(AppleScriptLanguage))
    private val myIndent: Indent = myIndentProcessor.getChildIndent(myNode)
    private val mySpacingProcessor: AppleScriptSpacingProcessor =
        AppleScriptSpacingProcessor(node, mySettings.getCommonSettings(AppleScriptLanguage))
    private val myWrappingProcessor: AppleScriptWrappingProcessor =
        AppleScriptWrappingProcessor(node, mySettings.getCommonSettings(AppleScriptLanguage))

    private var myParent: BlockWithParent? = null
    private val myChildWrap: Wrap? = null

    override fun buildChildren(): List<Block> {
        if (isLeaf) return EMPTY
        val tlChildren = ArrayList<Block>()
        var childNode = node.firstChildNode
        while (childNode != null) {
            if (childNode.text.trim().isNotEmpty()) {
                val childBlock = AppleScriptBlock(childNode, createChildWrap(childNode), Alignment.createAlignment(), mySettings)
                childBlock.setParent(this)
                tlChildren.add(childBlock)
            }
            childNode = childNode.treeNext
        }
        return tlChildren
    }

    private fun createChildWrap(childNode: ASTNode): Wrap =
        myWrappingProcessor.createChildWrap(childNode, Wrap.createWrap(WrapType.NONE, false), myChildWrap)

    override fun getSpacing(child1: Block?, child2: Block): Spacing? =
        mySpacingProcessor.getSpacing(child1 as? AppleScriptBlock, child2 as AppleScriptBlock)

    override fun isLeaf(): Boolean = false

    override fun getIndent(): Indent = myIndent

    override fun getParent(): BlockWithParent? = myParent

    override fun setParent(parent: BlockWithParent?) {
        myParent = parent
    }
}
