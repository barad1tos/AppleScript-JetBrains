package com.intellij.plugin.applescript.lang.ide

import com.intellij.lang.Commenter

class AppleScriptCommenter : Commenter {
    override fun getLineCommentPrefix(): String = "--"

    override fun getBlockCommentPrefix(): String = "(*"

    override fun getBlockCommentSuffix(): String = "*)"

    override fun getCommentedBlockCommentPrefix(): String? = null

    override fun getCommentedBlockCommentSuffix(): String? = null
}
