package com.intellij.plugin.applescript.lang.ide.structure

import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.Sorter
import com.intellij.openapi.editor.Editor
import com.intellij.plugin.applescript.psi.AppleScriptHandlerLabeledParametersDefinition
import com.intellij.plugin.applescript.psi.AppleScriptIdentifier
import com.intellij.plugin.applescript.psi.AppleScriptScriptBody
import com.intellij.plugin.applescript.psi.AppleScriptScriptObject
import com.intellij.psi.PsiFile

class AppleScriptStructureViewModel internal constructor(psiFile: PsiFile, editor: Editor?) :
    StructureViewModelBase(psiFile, editor, AppleScriptStructureViewElement(psiFile)),
    StructureViewModel.ElementInfoProvider {

    init {
        withSorters(Sorter.ALPHA_SORTER)
        withSuitableClasses(AppleScriptScriptBody::class.java, AppleScriptScriptObject::class.java)
    }

    override fun shouldEnterElement(element: Any?): Boolean = element is AppleScriptHandlerLabeledParametersDefinition

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = isAlwaysLeaf(element)

    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean =
        element.value is AppleScriptIdentifier
}
