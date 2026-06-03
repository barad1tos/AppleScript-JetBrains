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
 *    synthesizes `equals`/`hashCode` from primary-constructor properties,
 *    overriding the platform's PSI identity contract and breaking caches.
 *
 * The SDEF parser constructs the immutable `ClassDefinition` value and wraps
 * it here as a PSI element.
 */
class DictionaryClass :
    AbstractDictionaryComponent<Suite>,
    AppleScriptClass {
    private var data: ClassDefinition

    constructor(
        suite: Suite,
        data: ClassDefinition,
        xmlTagClass: XmlTag,
    ) : super(suite, data.name, data.code, xmlTagClass, data.description) {
        this.data = data
    }

    // D-04: resolve-once lookups via lazy(SYNCHRONIZED). The old mutable
    // `initialized: Boolean` flag + `MutableList` accumulators are gone.
    // The lazy block reads from `data.elementNames` / `data.respondingCommandNames`
    // captured at access time, so any future `setProperties`-style swap of `data`
    // would invalidate these caches — but the parser two-pass only mutates
    // `properties`, not the name lists, so the snapshot is correct in practice.
    private val elementsLazy: List<AppleScriptClass> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        data.elementNames.mapNotNull { dictionary.findClass(it) }
    }
    private val respondingCommandsLazy: List<AppleScriptCommand> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        data.respondingCommandNames.mapNotNull { dictionary.findCommand(it) }
    }

    override fun getDocFooter(): String =
        buildString {
            AppleScriptDocHelper.appendClassAttributes(this, this@DictionaryClass)
            val parent = parentClass
            if (parent != null) {
                val indent = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
                append("<p>")
                    .append(indent)
                    .append("INHERITED FROM ")
                    .append(parent.getName().uppercase())
                    .append("</p>")
                AppleScriptDocHelper.appendClassAttributes(this, parent)
            }
            append("</HTML>")
        }

    override val contents: List<AppleScriptClass> get() = emptyList()

    override var properties: List<AppleScriptPropertyDefinition>
        get() = data.properties
        set(value) {
            data = data.copy(properties = value)
        }

    override val suite: Suite get() = myParent

    override val parentClassName: String? get() = data.parentClassName

    override val parentClass: AppleScriptClass? get() = dictionary.findClass(data.parentClassName)

    override val elementNames: List<String> get() = data.elementNames

    override val elements: List<AppleScriptClass> get() = elementsLazy

    override val respondingCommands: List<AppleScriptCommand> get() = respondingCommandsLazy

    override val pluralClassName: String get() = data.pluralClassName

    override fun setPluralClassName(pluralClassName: String): DictionaryClass {
        if (!StringUtil.isEmpty(pluralClassName)) {
            data = data.copy(pluralClassName = pluralClassName)
        }
        return this
    }
}
