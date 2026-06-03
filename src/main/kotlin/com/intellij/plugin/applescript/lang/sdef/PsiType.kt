package com.intellij.plugin.applescript.lang.sdef

abstract class PsiType(
    val typeName: String,
) {
    private class AppleScriptBuiltInType(
        className: String,
    ) : PsiType(className)

    companion object {
        @JvmField
        val BOOLEAN: PsiType = AppleScriptBuiltInType("boolean")

        @JvmField
        val CLASS: PsiType = AppleScriptBuiltInType("class")
    }
}
