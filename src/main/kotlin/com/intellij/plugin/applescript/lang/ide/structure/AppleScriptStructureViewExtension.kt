package com.intellij.plugin.applescript.lang.ide.structure

import com.intellij.ide.structureView.StructureViewExtension
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.openapi.editor.Editor
import com.intellij.plugin.applescript.psi.AppleScriptPsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlTag

class AppleScriptStructureViewExtension : StructureViewExtension {

    override fun getType(): Class<out PsiElement> = XmlTag::class.java

    override fun getChildren(parent: PsiElement?): Array<StructureViewTreeElement> {
        if (parent is AppleScriptPsiElement) {
            AppleScriptStructureViewElement(parent).children
        }
        return emptyArray()
    }

    override fun getCurrentEditorElement(editor: Editor?, parent: PsiElement?): Any? {
        if (parent is AppleScriptPsiElement) {
            return AppleScriptStructureViewModel(parent.containingFile, editor).currentEditorElement
        }
        return null
    }
}
