package com.intellij.plugin.applescript.lang.sdef

import com.intellij.psi.xml.XmlTag

open class DictionaryPropertyImpl :
    AbstractDictionaryComponent<DictionaryComponent>,
    AppleScriptPropertyDefinition {

    private val backingTypeSpecifier: String
    private var backingAccessType: AccessType? = AccessType.RW

    constructor(
        classOrRecord: DictionaryComponent,
        name: String,
        code: String,
        typeSpecifier: String,
        description: String?,
        xmlTagProperty: XmlTag,
        accessType: AccessType?,
    ) : super(classOrRecord, name, code, xmlTagProperty, description) {
        this.backingTypeSpecifier = typeSpecifier
        if (accessType != null) this.backingAccessType = accessType
        check(myParent is AppleScriptClass || myParent is DictionaryRecord)
    }

    protected constructor(
        classOrRecord: DictionaryComponent,
        name: String,
        code: String,
        typeSpecifier: String,
        accessType: AccessType?,
        xmlTagProperty: XmlTag,
    ) : super(classOrRecord, name, code, xmlTagProperty) {
        this.backingTypeSpecifier = typeSpecifier
        this.backingAccessType = accessType
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

    override val suite: Suite get() = dictionaryParentComponent!!.suite!!

    override val typeSpecifier: String get() = backingTypeSpecifier
}
