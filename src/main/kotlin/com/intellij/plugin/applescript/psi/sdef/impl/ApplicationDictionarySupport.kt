package com.intellij.plugin.applescript.psi.sdef.impl

import com.intellij.lang.Language
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.plugin.applescript.AppleScriptIcons
import com.intellij.plugin.applescript.AppleScriptLanguage
import com.intellij.plugin.applescript.lang.dictionary.index.DictionaryIndexes
import com.intellij.plugin.applescript.lang.ide.AppleScriptDocHelper
import com.intellij.plugin.applescript.lang.sdef.AppleScriptClass
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand
import com.intellij.plugin.applescript.lang.sdef.AppleScriptPropertyDefinition
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.lang.sdef.CommandDirectParameter
import com.intellij.plugin.applescript.lang.sdef.DictionaryComponent
import com.intellij.plugin.applescript.lang.sdef.DictionaryEnumeration
import com.intellij.plugin.applescript.lang.sdef.DictionaryEnumerator
import com.intellij.plugin.applescript.lang.sdef.DictionaryRecord
import com.intellij.plugin.applescript.lang.sdef.Suite
import com.intellij.plugin.applescript.psi.AppleScriptIdentifier
import com.intellij.plugin.applescript.psi.impl.AppleScriptElementPresentation
import com.intellij.plugin.applescript.psi.sdef.DictionaryIdentifier
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.FakePsiElement
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlTag
import com.intellij.util.IncorrectOperationException
import java.io.File
import javax.swing.Icon

internal abstract class ApplicationDictionaryMapSupport :
    FakePsiElement(),
    ApplicationDictionary {
    protected abstract val indexes: DictionaryIndexes
    protected abstract val dictionaryVirtualFile: VirtualFile
    protected abstract val applicationBundleFile: File?

    abstract override fun getName(): String

    override val dictionaryFile: VirtualFile get() = dictionaryVirtualFile

    override val applicationBundle: File? get() = applicationBundleFile

    override val dictionaryEnumerationMap: Map<String, DictionaryEnumeration> get() = indexes.dictionaryEnumerationMap

    override val dictionaryEnumeratorMap: Map<String, DictionaryEnumerator> get() = indexes.dictionaryEnumeratorMap

    override val dictionaryRecordMap: Map<String, DictionaryRecord> get() = indexes.dictionaryRecordMap

    override val dictionaryCommandMap: Map<String, AppleScriptCommand> get() = indexes.dictionaryCommandMap

    override val dictionaryClassMap: Map<String, AppleScriptClass> get() = indexes.dictionaryClassMap

    override val dictionaryPropertyMap: Map<String, AppleScriptPropertyDefinition> get() = indexes.dictionaryPropertyMap

    override val allCommands: Collection<AppleScriptCommand> get() = indexes.dictionaryCommandMap.values
}

internal abstract class ApplicationDictionaryLookupSupport : ApplicationDictionaryMapSupport() {
    override fun findClass(name: String?): AppleScriptClass? = name?.let { indexes.dictionaryClassMap[it] }

    override fun getParameterNamesForCommand(name: String): List<String>? = indexes.parameterNames(name)

    override fun findDirectParameterForCommand(commandName: String): CommandDirectParameter? =
        indexes.dictionaryCommandMap[commandName]?.directParameter

    override fun findProperty(name: String): AppleScriptPropertyDefinition? = indexes.dictionaryPropertyMap[name]

    override fun findAllCommandsWithName(name: String): List<AppleScriptCommand> =
        indexes.dictionaryCommandListMap[name]?.toList() ?: emptyList()

    override fun findEnumerator(name: String): DictionaryEnumerator? = indexes.dictionaryEnumeratorMap[name]

    override fun findClassByPluralName(pluralForm: String): AppleScriptClass? {
        val classesByPluralName = indexes.dictionaryClassToPluralNameMap
        return classesByPluralName[pluralForm]
    }

    override fun findEnumeration(name: String): DictionaryEnumeration? = indexes.dictionaryEnumerationMap[name]

    override fun findCommand(name: String?): AppleScriptCommand? = name?.let { indexes.dictionaryCommandMap[it] }
}

internal abstract class ApplicationDictionaryMutationSupport : ApplicationDictionaryLookupSupport() {
    override fun addCommand(command: AppleScriptCommand): Boolean = indexes.addCommand(command)

    override fun addClass(appleScriptClass: AppleScriptClass): Boolean = indexes.addClass(appleScriptClass)

    override fun findClassByCode(code: String): AppleScriptClass? = indexes.dictionaryClassByCodeMap[code]

    override fun addProperty(property: AppleScriptPropertyDefinition): Boolean = indexes.addProperty(property)

    override fun addEnumeration(enumeration: DictionaryEnumeration): Boolean = indexes.addEnumeration(enumeration)

    override fun addRecord(record: DictionaryRecord) = indexes.addRecord(record)
}

internal abstract class ApplicationDictionarySuiteSupport : ApplicationDictionaryMutationSupport() {
    protected abstract val suites: MutableList<Suite>

    override fun addSuite(suite: Suite): Boolean = suites.add(suite)

    override val documentation: String
        get() =
            buildString {
                append(type).append(" <b>").append(getName()).append("</b>")
                append("<p>")
                for (suite in suites) {
                    append("<br>    <b>")
                    AppleScriptDocHelper.appendElementLink(this, suite, suite.getName())
                    append("</b><br>")
                }
                append("</p>")
            }

    override fun findSuiteByName(suiteCode: String): Suite? {
        val matchingSuite = suites.firstOrNull { suite -> suiteCode == suite.getName() }
        return matchingSuite
    }

    override fun findSuiteByCode(suiteCode: String): Suite? = suites.firstOrNull { suite -> suiteCode == suite.code }
}

internal abstract class ApplicationDictionaryComponentSupport : ApplicationDictionarySuiteSupport() {
    override val code: String? get() = null

    override val cocoaClassName: String? get() = null

    override val nameIdentifiers: List<String> get() = applicationName.split("\\s+".toRegex())

    override val qualifiedPath: String get() = "dictionary:${getName()}/$qualifiedName"

    override val qualifiedName: String get() = "$type:$code"

    override var description: String?
        get() = null
        set(_) = Unit

    override val type: String get() = "dictionary"
}

internal abstract class ApplicationDictionaryRelationSupport : ApplicationDictionaryComponentSupport() {
    override fun getOriginalDeclaration(): PsiElement? = null

    override val suite: Suite? get() = null

    override val dictionaryParentComponent: DictionaryComponent? get() = null

    override val dictionary: ApplicationDictionary get() = this
}

internal abstract class ApplicationDictionaryPsiSupport : ApplicationDictionaryRelationSupport() {
    protected abstract val dictionaryProject: Project
    protected abstract val dictionaryIcon: Icon?
    protected abstract var dictionaryDocumentationText: String?

    override fun getProject(): Project = dictionaryProject

    override fun getIcon(open: Boolean): Icon = dictionaryIcon ?: AppleScriptIcons.OPEN_DICTIONARY

    override fun getPresentation(): ItemPresentation = AppleScriptElementPresentation(this)

    override fun getLanguage(): Language = AppleScriptLanguage

    override fun getParent(): PsiElement? = PsiManager.getInstance(getProject()).findFile(dictionaryFile)

    override fun setDictionaryDoc(documentation: String?) {
        dictionaryDocumentationText = documentation
    }
}

internal abstract class ApplicationDictionaryNameSupport : ApplicationDictionaryPsiSupport() {
    protected abstract var dictionaryName: String
    protected abstract var dictionaryRootTag: XmlTag?

    override fun getName(): String = dictionaryName

    @Throws(IncorrectOperationException::class)
    override fun setName(name: String): PsiElement {
        dictionaryName = name
        return this
    }

    override fun getIdentifier(): AppleScriptIdentifier {
        // Null is a construction/lifecycle bug: the constructor assigns the root tag
        // before the dictionary is exposed to callers.
        val rootTag = dictionaryRootTag!!
        var myIdentifier: DictionaryIdentifier? = null
        val titleAttr: XmlAttribute? = rootTag.getAttribute("title")
        if (titleAttr != null) {
            val attrValue: XmlAttributeValue? = titleAttr.valueElement
            if (attrValue != null) {
                myIdentifier = DictionaryIdentifierImpl(this, getName(), attrValue)
            }
        }
        return myIdentifier ?: DictionaryIdentifierImpl(this, getName(), rootTag)
    }

    override fun getNameIdentifier(): PsiElement = getIdentifier()

    override fun setRootTag(myRootTag: XmlTag): ApplicationDictionary {
        dictionaryRootTag = myRootTag
        return this
    }

    override val rootTag: XmlTag? get() = dictionaryRootTag
}
