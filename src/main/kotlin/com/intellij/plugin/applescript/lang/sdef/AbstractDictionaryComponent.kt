package com.intellij.plugin.applescript.lang.sdef

import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.util.text.StringUtil
import com.intellij.plugin.applescript.lang.AppleScriptComponentType
import com.intellij.plugin.applescript.lang.ide.AppleScriptDocHelper
import com.intellij.plugin.applescript.psi.AppleScriptExpression
import com.intellij.plugin.applescript.psi.impl.AppleScriptElementPresentation
import com.intellij.plugin.applescript.psi.sdef.DictionaryIdentifier
import com.intellij.plugin.applescript.psi.sdef.impl.DictionaryComponentBase
import com.intellij.plugin.applescript.psi.sdef.impl.DictionaryIdentifierImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlTag
import javax.swing.Icon

abstract class AbstractDictionaryComponent<P : DictionaryComponent> :
    DictionaryComponentBase<P, XmlTag>,
    DictionaryComponent {

    private val backingCode: String
    private val backingName: String
    private var backingDescription: String? = null

    @Suppress("PropertyName")
    protected var docField: String? = null

    protected constructor(
        parent: P,
        name: String,
        code: String,
        myXmlTag: XmlTag,
        description: String?,
    ) : super(myXmlTag, parent) {
        this.backingCode = code
        this.backingName = name
        this.backingDescription = description
    }

    protected constructor(
        parent: P,
        name: String,
        code: String,
        myXmlTag: XmlTag,
    ) : super(myXmlTag, parent) {
        this.backingCode = code
        this.backingName = name
    }

    override fun isScriptProperty(): Boolean = false

    override fun isHandler(): Boolean = false

    override fun getOriginalDeclaration(): PsiElement = this

    override fun isObjectProperty(): Boolean = false

    override fun isVariable(): Boolean = false

    override fun findAssignedValue(): AppleScriptExpression? = null

    override fun getName(): String = backingName

    override val nameIdentifiers: List<String>
        get() = backingName.split("\\s+".toRegex())

    override val qualifiedName: String
        // Structural invariant: a concrete dictionary component (class/command/property/etc.) always
        // has a parent component — only the root ApplicationDictionaryImpl returns a null parent, and
        // qualifiedName is never resolved on the root through this path.
        get() = "${requireNotNull(dictionaryParentComponent) { "$backingName has a parent dictionary component" }.qualifiedName}/$shortQname"

    private val shortQname: String
        get() = type.substring(11) + ":" + code

    override val qualifiedPath: String
        // Same structural invariant as qualifiedName: a concrete component always has a parent.
        get() = "${requireNotNull(dictionaryParentComponent) { "$backingName has a parent dictionary component" }.qualifiedPath}/$shortQname"

    override val type: String
        get() {
            val componentType = AppleScriptComponentType.typeOf(this)
            return componentType?.toString()?.lowercase() ?: "dictionary reference"
        }

    override var description: String?
        get() = backingDescription
        set(value) {
            backingDescription = value
        }

    override val dictionary: ApplicationDictionary
        get() = suite.dictionary

    override val cocoaClassName: String?
        get() = null

    protected open fun getDocHeader(): String = buildString {
        append("<b>")
        AppleScriptDocHelper.appendElementLink(this, dictionary, dictionary.getName())
        append("</b> ").append(" Dictionary").append("<br>")
    }

    protected open fun getTypeDescription(): String = buildString {
        append("<p>")
        val capitalizedType = StringUtil.capitalizeWords(type, true)
        val displayType = if (capitalizedType.lowercase().contains("dictionary")) capitalizedType.substring(10) else capitalizedType
        append(displayType).append(" <b>").append(backingName).append("</b>")

        when (val thisRef = this@AbstractDictionaryComponent) {
            is AppleScriptClass -> {
                var parent: AppleScriptClass? = thisRef.parentClass
                if (parent != null) {
                    append(" [inh. ")
                    var ext = ""
                    var guard = 15
                    while (parent != null && guard > 0) {
                        guard--
                        append(ext)
                        AppleScriptDocHelper.appendElementLink(this, parent, parent.getName())
                        parent = parent.parentClass
                        ext = " > "
                    }
                    append(" ]")
                }
            }

            is CommandParameter -> {
                val pType = StringUtil.notNullize(thisRef.typeSpecifier)
                append(" [").append(pType).append("]")
            }

            else -> Unit
        }
        append(" : ").append(StringUtil.notNullize(description))
    }

    override val documentation: String
        get() = getDocHeader() + getTypeDescription() + getDocFooter()

    protected open fun getDocFooter(): String = "</p>"

    override val code: String
        get() = backingCode

    abstract override val suite: Suite

    override fun getIcon(open: Boolean): Icon? = AppleScriptComponentType.typeOf(this)?.icon

    override fun getPresentation(): ItemPresentation = AppleScriptElementPresentation(this)

    override fun getLocationString(): String? = qualifiedPath

    override fun getIdentifier(): DictionaryIdentifier {
        var myIdentifier: DictionaryIdentifier? = null
        val nameAttr = myXmlElement.getAttribute("name")
        if (nameAttr != null) {
            nameAttr.valueElement?.let {
                myIdentifier = DictionaryIdentifierImpl(this, getName(), it)
            }
        } else {
            for (anyAttr in myXmlElement.attributes) {
                myIdentifier = DictionaryIdentifierImpl(this, getName(), anyAttr)
            }
        }
        return myIdentifier ?: DictionaryIdentifierImpl(this, getName(), myXmlElement.attributes[0])
    }

    override fun getNameIdentifier(): PsiElement? = getIdentifier()

    override fun setDictionaryDoc(documentation: String?) {
        this.docField = documentation
    }
}
