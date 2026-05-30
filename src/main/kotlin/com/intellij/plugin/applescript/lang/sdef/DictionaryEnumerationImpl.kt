package com.intellij.plugin.applescript.lang.sdef

import com.intellij.psi.xml.XmlTag

class DictionaryEnumerationImpl :
    AbstractDictionaryComponent<Suite>,
    DictionaryEnumeration {

    private var enumerators: MutableList<DictionaryEnumerator> = mutableListOf()

    private constructor(
        suite: Suite,
        name: String,
        code: String,
        enumerators: List<DictionaryEnumerator>?,
        description: String?,
        xmlTagEnumeration: XmlTag,
    ) : super(suite, name, code, xmlTagEnumeration, description) {
        if (enumerators != null) this.enumerators = enumerators.toMutableList()
    }

    constructor(
        suite: Suite,
        name: String,
        code: String,
        description: String?,
        xmlTagEnumeration: XmlTag,
    ) : super(suite, name, code, xmlTagEnumeration, description)

    constructor(
        suite: Suite,
        name: String,
        code: String,
        enumerators: List<DictionaryEnumerator>?,
        xmlTagEnumeration: XmlTag,
    ) : this(suite, name, code, enumerators, null, xmlTagEnumeration)

    override fun getEnumerators(): List<DictionaryEnumerator> = enumerators

    override fun setEnumerators(enumerators: List<DictionaryEnumerator>?) {
        this.enumerators = enumerators?.toMutableList() ?: mutableListOf()
    }

    override val suite: Suite get() = myParent
}
