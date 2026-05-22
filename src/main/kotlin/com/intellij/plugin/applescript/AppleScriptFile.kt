package com.intellij.plugin.applescript

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.fileTypes.FileType
import com.intellij.plugin.applescript.psi.AppleScriptPsiElement
import com.intellij.plugin.applescript.psi.impl.AppleScriptElementPresentation
import com.intellij.plugin.applescript.psi.impl.AppleScriptPsiElementImpl
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.ResolveState
import com.intellij.psi.scope.PsiScopeProcessor
import javax.swing.Icon

class AppleScriptFile(viewProvider: FileViewProvider) :
    PsiFileBase(viewProvider, AppleScriptLanguage),
    AppleScriptPsiElement {

    override fun processDeclarations(
        processor: PsiScopeProcessor,
        state: ResolveState,
        lastParent: PsiElement?,
        place: PsiElement,
    ): Boolean =
        AppleScriptPsiElementImpl.processDeclarationsImpl(this, processor, state, lastParent, place) &&
            super.processDeclarations(processor, state, lastParent, place)

    override fun getFileType(): FileType = AppleScriptFileType

    override fun toString(): String = "AppleScript File"

    override fun getIcon(flags: Int): Icon = AppleScriptIcons.FILE

    override fun getPresentation(): ItemPresentation = AppleScriptElementPresentation(this)
}
