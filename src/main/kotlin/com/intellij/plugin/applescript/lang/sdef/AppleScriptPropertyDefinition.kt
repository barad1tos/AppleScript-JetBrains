package com.intellij.plugin.applescript.lang.sdef

/**
 * GROUP A (0 gen-implementer) SDEF interface — the Phase 5 property-conversion pilot (PSI-02/03).
 *
 * Each getter was converted from `fun getX()` to a Kotlin property. The Java-visible accessor names
 * are preserved by **property-name choice** (e.g. `psiType` synthesizes `getPsiType()`, `accessType`
 * synthesizes `getAccessType()`/`setAccessType()`), NOT by `@get:JvmName`: Kotlin 2.3.21 rejects
 * `@JvmName` on an abstract interface property accessor ("not applicable to this declaration"), so the
 * D-03 blanket-`@get:JvmName` policy is NOT applicable on interface declarations. The Java-name
 * contract is instead locked by the reflective `PsiGetterJvmSignatureTest`, which asserts the
 * synthesized JVM names over the compiled bytecode — a stronger guard than a source annotation.
 *
 * The `is`-prefix on [isClassProperty]/[isRecordProperty] is PRESERVED in the property name so the
 * synthesized accessor stays `isClassProperty()` (NOT `getClassProperty()`); never let an IDE
 * "convert getter to property" drop the prefix.
 */
interface AppleScriptPropertyDefinition : DictionaryComponent {

    /** JVM-visible as `getPsiType()`. */
    val psiType: PsiType

    /** JVM-visible as `isClassProperty()` — `is`-prefix preserved. */
    val isClassProperty: Boolean

    /** JVM-visible as `isRecordProperty()` — `is`-prefix preserved. */
    val isRecordProperty: Boolean

    /** JVM-visible as `getMyClass()`. */
    val myClass: AppleScriptClass?

    /** JVM-visible as `getMyRecord()`. */
    val myRecord: DictionaryRecord?

    /** JVM-visible as `getAccessType()` / `setAccessType(AccessType)`. */
    var accessType: AccessType?

    /** JVM-visible as `getTypeSpecifier()`. */
    val typeSpecifier: String
}
