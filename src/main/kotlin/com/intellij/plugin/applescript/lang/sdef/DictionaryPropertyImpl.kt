package com.intellij.plugin.applescript.lang.sdef

import com.intellij.psi.xml.XmlTag

data class DictionaryPropertyData(
    val name: String,
    val code: String,
    val typeSpecifier: String,
    val description: String?,
    val accessType: AccessType = AccessType.RW,
)

open class DictionaryPropertyImpl(
    classOrRecord: DictionaryComponent,
    data: DictionaryPropertyData,
    xmlTagProperty: XmlTag,
) : AbstractDictionaryComponent<DictionaryComponent>(
        classOrRecord,
        data.name,
        data.code,
        xmlTagProperty,
        data.description,
    ),
    AppleScriptPropertyDefinition {
    private val backingTypeSpecifier: String = data.typeSpecifier
    private var backingAccessType: AccessType? = data.accessType

    init {
        check(myParent is AppleScriptClass || myParent is DictionaryRecord)
    }

    override val psiType: PsiType get() = PsiType.CLASS

    override val isClassProperty: Boolean get() = myParent is AppleScriptClass

    override val isRecordProperty: Boolean get() = myParent is DictionaryRecord

    override val myClass: AppleScriptClass? get() = myParent as? AppleScriptClass

    override val myRecord: DictionaryRecord? get() = myParent as? DictionaryRecord

    override var accessType: AccessType?
        get() = backingAccessType
        set(value) {
            backingAccessType = value
        }

    override fun isObjectProperty(): Boolean = true

    override val suite: Suite
        get() =
            when (val parent = myParent) {
                is AppleScriptClass -> parent.suite
                is DictionaryRecord -> parent.suite
                else -> error("property ${getName()} has a class or record parent")
            }

    override val typeSpecifier: String get() = backingTypeSpecifier
}
