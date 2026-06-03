package com.intellij.plugin.applescript.lang.sdef

import com.intellij.psi.xml.XmlTag

/**
 * PSI representation of one SDEF command parameter — Hybrid pattern (D-01).
 *
 *  - The class extends `AbstractDictionaryComponent` → `DictionaryComponentBase` →
 *    `FakePsiElement` so it remains a real PSI node (PsiManager caches,
 *    `areElementsEquivalent`, Find Usages and rename refactoring all keep working).
 *  - The class holds `internal val data: CommandParameterData` (immutable value
 *    type). Every public accessor reads from `data`. The field is `internal val`
 *    rather than `private val` so `AppleScriptCommandImpl.setParameters` can
 *    re-build its frozen `CommandData` from the parameter list it just received
 *    without round-tripping through a builder for each parameter — see the
 *    `AppleScriptCommandImpl.setParameters` body for the routing.
 *  - Why not `data class : FakePsiElement`? PITFALLS §1.1 BLOCKER: Kotlin
 *    synthesizes `equals`/`hashCode` from primary-constructor properties,
 *    overriding the platform's PSI identity contract and breaking caches.
 *
 * The SDEF parser constructs the immutable `CommandParameterData` value and
 * wraps it here as a PSI element.
 */
class CommandParameterImpl :
    AbstractDictionaryComponent<AppleScriptCommand>,
    CommandParameter {
    internal val data: CommandParameterData

    /**
     * Hybrid back-link constructor — used by `AppleScriptCommandImpl.getParameters`
     * (and similar wrap-on-demand call sites) to surface a `CommandParameter`
     * PSI view of a `CommandParameterData` value that was reconstructed from
     * another impl's `data`. Same `data` content + same parent reference =
     * structurally equivalent for caller intent, while preserving the PSI
     * identity hierarchy.
     */
    constructor(
        myCommand: AppleScriptCommand,
        data: CommandParameterData,
        xmlTagParameter: XmlTag,
    ) : super(myCommand, data.name, data.code, xmlTagParameter, data.description) {
        this.data = data
    }

    override val typeSpecifier: String get() = data.type

    override val myCommand: AppleScriptCommand get() = myParent

    override val isOptional: Boolean get() = data.optional

    override val suite: Suite get() = myCommand.suite
}
