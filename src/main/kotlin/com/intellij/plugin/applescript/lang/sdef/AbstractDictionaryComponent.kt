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

    private val code: String
    private val name: String
    private var description: String? = null

    @Suppress("PropertyName")
    protected var docField: String? = null

    protected constructor(
        parent: P,
        name: String,
        code: String,
        myXmlTag: XmlTag,
        description: String?,
    ) : super(myXmlTag, parent) {
        this.code = code
        this.name = name
        this.description = description
    }

    protected constructor(
        parent: P,
        name: String,
        code: String,
        myXmlTag: XmlTag,
    ) : super(myXmlTag, parent) {
        this.code = code
        this.name = name
    }

    override fun isScriptProperty(): Boolean = false

    override fun isHandler(): Boolean = false

    override fun getOriginalDeclaration(): PsiElement = this

    override fun isObjectProperty(): Boolean = false

    override fun isVariable(): Boolean = false

    override fun findAssignedValue(): AppleScriptExpression? = null

    override fun getName(): String = name

    override fun getNameIdentifiers(): List<String> = name.split("\\s+".toRegex())

    override fun getQualifiedName(): String =
        "${getDictionaryParentComponent().getQualifiedName()}/$shortQname"

    private val shortQname: String
        get() = getType().substring(11) + ":" + getCode()

    override fun getQualifiedPath(): String =
        "${getDictionaryParentComponent().getQualifiedPath()}/$shortQname"

    override fun getType(): String {
        val componentType = AppleScriptComponentType.typeOf(this)
        return componentType?.toString()?.lowercase() ?: "dictionary reference"
    }

    override fun setDescription(description: String?) {
        this.description = description
    }

    override fun getDictionary(): ApplicationDictionary = getSuite().getDictionary()

    override fun getCocoaClassName(): String? = null

    protected open fun getDocHeader(): String = buildString {
        append("<b>")
        AppleScriptDocHelper.appendElementLink(this, getDictionary(), getDictionary().getName())
        append("</b> ").append(" Dictionary").append("<br>")
    }

    protected open fun getTypeDescription(): String = buildString {
        append("<p>")
        val type = StringUtil.capitalizeWords(getType(), true)
        val displayType = if (type.lowercase().contains("dictionary")) type.substring(10) else type
        append(displayType).append(" <b>").append(name).append("</b>")

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
        append(" : ").append(StringUtil.notNullize(getDescription()))
    }

    override fun getDocumentation(): String =
        getDocHeader() + getTypeDescription() + getDocFooter()

    protected open fun getDocFooter(): String = "</p>"

    override fun getCode(): String = code

    override fun getDescription(): String? = description

    abstract override fun getSuite(): Suite

    override fun getIcon(open: Boolean): Icon? = AppleScriptComponentType.typeOf(this)?.icon

    override fun getPresentation(): ItemPresentation = AppleScriptElementPresentation(this)

    override fun getLocationString(): String? = getQualifiedPath()

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
