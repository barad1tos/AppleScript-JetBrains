package com.intellij.plugin.applescript.lang.sdef

import com.intellij.psi.xml.XmlTag

class DictionaryEnumeratorImpl :
    AbstractDictionaryComponent<DictionaryEnumeration>,
    DictionaryEnumerator {

    constructor(
        myEnumeration: DictionaryEnumeration,
        name: String,
        code: String,
        description: String?,
        xmlTagEnumerator: XmlTag,
    ) : super(myEnumeration, name, code, xmlTagEnumerator, description)

    constructor(
        myEnumeration: DictionaryEnumeration,
        name: String,
        code: String,
        xmlTagEnumerator: XmlTag,
    ) : super(myEnumeration, name, code, xmlTagEnumerator)

    override val suite: Suite get() = getMyEnumeration().suite

    override fun getMyEnumeration(): DictionaryEnumeration = myParent
}
