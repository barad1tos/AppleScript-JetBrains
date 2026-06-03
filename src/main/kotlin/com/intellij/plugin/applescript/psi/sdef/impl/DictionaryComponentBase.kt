package com.intellij.plugin.applescript.psi.sdef.impl

import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.plugin.applescript.AppleScriptLanguage
import com.intellij.plugin.applescript.lang.sdef.DictionaryComponent
import com.intellij.plugin.applescript.psi.AppleScriptPsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.FakePsiElement
import com.intellij.psi.xml.XmlElement

/**
 * Thin PSI adapter that forwards platform text/range/navigation methods to the backing XML element.
 */
abstract class DictionaryComponentBase<P : DictionaryComponent, D : XmlElement> protected constructor(
    @JvmField protected val myXmlElement: D,
    @JvmField protected val myParent: P,
) : FakePsiElement(),
    AppleScriptPsiElement {
    override fun getLanguage(): Language = AppleScriptLanguage

    override fun getTextRange(): TextRange? = myXmlElement.textRange

    override fun getStartOffsetInParent(): Int = myXmlElement.startOffsetInParent

    override fun getTextLength(): Int = myXmlElement.textLength

    override fun getTextOffset(): Int = myXmlElement.textOffset

    override fun getProject(): Project = dictionaryParentComponent.project

    override fun getContainingFile(): PsiFile =
        when {
            myXmlElement.isValid -> myXmlElement.containingFile
            else -> parent.containingFile
        }

    override fun getText(): String? = myXmlElement.text

    override fun getNode(): ASTNode? = myXmlElement.node

    override fun getParent(): PsiElement = dictionaryParentComponent

    /** JVM-visible as `getDictionaryParentComponent()`; satisfies `DictionaryComponent.dictionaryParentComponent`. */
    val dictionaryParentComponent: P get() = myParent
}
