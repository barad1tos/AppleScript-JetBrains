package com.intellij.plugin.applescript.lang.sdef

import com.intellij.openapi.util.text.StringUtil
import com.intellij.plugin.applescript.lang.ide.AppleScriptDocHelper
import com.intellij.psi.xml.XmlTag

/**
 * Hybrid PSI + immutable value type pattern (D-01 keystone of v1.1).
 *
 *  - The class extends `AbstractDictionaryComponent` → `DictionaryComponentBase` →
 *    `FakePsiElement` so it remains a real PSI node (PsiManager caches,
 *    `areElementsEquivalent`, Find Usages, rename refactoring all keep working).
 *  - The class holds `private var data: ClassDefinition` (immutable value type).
 *    Every public accessor reads from `data`. The field is `var` only because
 *    the existing `setProperties(...)` setter swaps in a `data.copy(properties =
 *    …)` after the parser's two-pass walk fills in `<property>` tags in a second
 *    pass.
 *  - The `elements` and `respondingCommands` lookups (D-04) are
 *    `by lazy(LazyThreadSafetyMode.SYNCHRONIZED)` — the old mutable
 *    `initialized: Boolean` flag and `MutableList` accumulators are gone.
 *    `SYNCHRONIZED` is the safe default; CD-03 allows `PUBLICATION` if profile
 *    shows monitor contention.
 *  - Why not `data class : FakePsiElement`? PITFALLS §1.1 BLOCKER: Kotlin
 *    synthesises `equals`/`hashCode` from primary-constructor properties,
 *    overriding the platform's PSI identity contract and breaking caches.
 *
 * Public constructor surface is preserved verbatim so SDEF_Parser.parseClassTag
 * (line 307) + parseClassExtensionTag (line 229) continue to compile unchanged.
 */
class DictionaryClass :
    AbstractDictionaryComponent<Suite>,
    AppleScriptClass {

    private var data: ClassDefinition

    constructor(
        suite: Suite,
        name: String,
        code: String,
        xmlTagClass: XmlTag,
        parentClassName: String?,
        elementNames: List<String>?,
        respondingCommandNames: List<String>?,
        pluralClassName: String?,
    ) : super(suite, name, code, xmlTagClass, null) {
        val plural = if (StringUtil.isEmpty(pluralClassName)) "${name}s" else pluralClassName!!
        this.data = ClassDefinition(
            name = name,
            code = code,
            description = null,
            parentClassName = parentClassName,
            pluralClassName = plural,
            elementNames = elementNames.orEmpty(),
            respondingCommandNames = respondingCommandNames.orEmpty(),
            properties = emptyList(),
        )
    }

    constructor(
        suite: Suite,
        name: String,
        code: String,
        properties: List<AppleScriptPropertyDefinition>,
        xmlTagClass: XmlTag,
        description: String?,
        parentClassName: String?,
    ) : super(suite, name, code, xmlTagClass, description) {
        this.data = ClassDefinition(
            name = name,
            code = code,
            description = description,
            parentClassName = parentClassName,
            pluralClassName = "${name}s",
            elementNames = emptyList(),
            respondingCommandNames = emptyList(),
            properties = properties,
        )
    }

    // D-04: resolve-once lookups via lazy(SYNCHRONIZED). The old mutable
    // `initialized: Boolean` flag + `MutableList` accumulators are gone.
    // The lazy block reads from `data.elementNames` / `data.respondingCommandNames`
    // captured at access time, so any future `setProperties`-style swap of `data`
    // would invalidate these caches — but the parser two-pass only mutates
    // `properties`, not the name lists, so the snapshot is correct in practice.
    private val elementsLazy: List<AppleScriptClass> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        data.elementNames.mapNotNull { getDictionary().findClass(it) }
    }
    private val respondingCommandsLazy: List<AppleScriptCommand> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        data.respondingCommandNames.mapNotNull { getDictionary().findCommand(it) }
    }

    override fun getDocFooter(): String = buildString {
        AppleScriptDocHelper.appendClassAttributes(this, this@DictionaryClass)
        val parentClass = getParentClass()
        if (parentClass != null) {
            val indent = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
            append("<p>").append(indent).append("INHERITED FROM ").append(parentClass.getName().uppercase()).append("</p>")
            AppleScriptDocHelper.appendClassAttributes(this, parentClass)
        }
        append("</HTML>")
    }

    @Suppress("UNCHECKED_CAST")
    override fun getContents(): List<AppleScriptClass> = emptyList()

    override fun getProperties(): List<AppleScriptPropertyDefinition> = data.properties

    override fun setProperties(properties: List<AppleScriptPropertyDefinition>) {
        data = data.copy(properties = properties)
    }

    override fun getSuite(): Suite = myParent

    override fun getParentClassName(): String? = data.parentClassName

    override fun getParentClass(): AppleScriptClass? = getDictionary().findClass(data.parentClassName)

    override fun getElementNames(): List<String> = data.elementNames

    override fun getElements(): List<AppleScriptClass> = elementsLazy

    override fun getRespondingCommands(): List<AppleScriptCommand> = respondingCommandsLazy

    override fun getPluralClassName(): String = data.pluralClassName

    override fun setPluralClassName(pluralClassName: String): DictionaryClass {
        if (!StringUtil.isEmpty(pluralClassName)) {
            data = data.copy(pluralClassName = pluralClassName)
        }
        return this
    }
}
