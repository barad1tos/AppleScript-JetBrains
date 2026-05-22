package com.intellij.plugin.applescript.lang.sdef

import com.intellij.psi.xml.XmlTag

open class DictionaryPropertyImpl :
    AbstractDictionaryComponent<DictionaryComponent>,
    AppleScriptPropertyDefinition {

    private val typeSpecifier: String
    private var accessType: AccessType? = AccessType.RW

    constructor(
        classOrRecord: DictionaryComponent,
        name: String,
        code: String,
        typeSpecifier: String,
        description: String?,
        xmlTagProperty: XmlTag,
        accessType: AccessType?,
    ) : super(classOrRecord, name, code, xmlTagProperty, description) {
        this.typeSpecifier = typeSpecifier
        if (accessType != null) this.accessType = accessType
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
        this.typeSpecifier = typeSpecifier
        this.accessType = accessType
        check(myParent is AppleScriptClass || myParent is DictionaryRecord)
    }

    @Suppress("UNCHECKED_CAST")
    override fun getPsiType(): PsiType = PsiType.CLASS

    override fun isClassProperty(): Boolean = myParent is AppleScriptClass

    override fun isRecordProperty(): Boolean = myParent is DictionaryRecord

    override fun getMyClass(): AppleScriptClass? = myParent as? AppleScriptClass

    override fun getMyRecord(): DictionaryRecord? = myParent as? DictionaryRecord

    override fun setAccessType(accessType: AccessType?) {
        this.accessType = accessType
    }

    override fun isObjectProperty(): Boolean = true

    override fun getSuite(): Suite = getDictionaryParentComponent().getSuite()!!

    override fun getAccessType(): AccessType? = accessType

    override fun getTypeSpecifier(): String = typeSpecifier
}
