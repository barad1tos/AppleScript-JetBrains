package com.intellij.plugin.applescript.lang.sdef

import com.intellij.psi.xml.XmlTag

class DictionaryEnumerationImpl :
    AbstractDictionaryComponent<Suite>,
    DictionaryEnumeration {
    private var enumerators: MutableList<DictionaryEnumerator> = mutableListOf()

    constructor(
        suite: Suite,
        name: String,
        code: String,
        description: String?,
        xmlTagEnumeration: XmlTag,
    ) : super(suite, name, code, xmlTagEnumeration, description)

    override fun getEnumerators(): List<DictionaryEnumerator> = enumerators

    override fun setEnumerators(enumerators: List<DictionaryEnumerator>?) {
        this.enumerators = enumerators?.toMutableList() ?: mutableListOf()
    }

    override val suite: Suite get() = myParent
}
