package com.intellij.plugin.applescript.psi.sdef.impl

import com.github.markusbernhardt.proxy.util.PListParser
import com.intellij.lang.Language
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.plugin.applescript.AppleScriptIcons
import com.intellij.plugin.applescript.AppleScriptLanguage
import com.intellij.plugin.applescript.lang.ide.AppleScriptDocHelper
import com.intellij.plugin.applescript.lang.sdef.AppleScriptClass
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommandImpl
import com.intellij.plugin.applescript.lang.sdef.AppleScriptPropertyDefinition
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.lang.sdef.CommandDirectParameter
import com.intellij.plugin.applescript.lang.sdef.DictionaryComponent
import com.intellij.plugin.applescript.lang.sdef.DictionaryEnumeration
import com.intellij.plugin.applescript.lang.sdef.DictionaryEnumerator
import com.intellij.plugin.applescript.lang.sdef.DictionaryRecord
import com.intellij.plugin.applescript.lang.sdef.Suite
import com.intellij.plugin.applescript.lang.sdef.parser.SDEF_Parser
import com.intellij.plugin.applescript.psi.AppleScriptExpression
import com.intellij.plugin.applescript.psi.AppleScriptIdentifier
import com.intellij.plugin.applescript.psi.impl.AppleScriptElementPresentation
import com.intellij.plugin.applescript.psi.sdef.DictionaryIdentifier
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.FakePsiElement
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.util.IncorrectOperationException
import com.intellij.util.ui.JBImageIcon
import com.intellij.util.ui.JBUI
import org.apache.commons.imaging.ImageReadException
import org.apache.commons.imaging.formats.icns.IcnsImageParser
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.swing.Icon

/**
 * PSI representation of one SDEF dictionary. Owns the suite registry plus per-name lookup maps for
 * commands / classes / records / properties / enumerations, eagerly initialised at construction by
 * parsing the bundled XmlFile via [SDEF_Parser]. Bundle metadata (application name, icon) is read
 * from the .app's Info.plist + ICNS resource when available.
 */
class ApplicationDictionaryImpl(
    private val project: Project,
    dictionaryXmlFile: XmlFile,
    private var applicationName: String,
    private val applicationBundleFile: File?,
) : FakePsiElement(),
    ApplicationDictionary {

    private val dictionaryFile: VirtualFile = dictionaryXmlFile.virtualFile
    private var applicationIcon: Icon? = null
    private val includedFiles: MutableList<PsiFile> = ArrayList()
    private var dictionaryName: String = ""
    private var documentation: String? = null

    private var myRootTag: XmlTag? = null
    private val mySuites: MutableList<Suite> = ArrayList()

    // Plan 02-05 / D-07: the 9-map index cluster moved into [DictionaryIndexes]
    // (CHM-backed). Closes the processInclude race latent since v1.0.0; Phase 1
    // explicitly deferred this fix here. v1.3 service split lifts `indexes`
    // wholesale into `SdefIndexService` without touching any consumer site.
    private val indexes: DictionaryIndexes = DictionaryIndexes()

    init {
        readDictionaryFromXmlFile(dictionaryXmlFile)
        applicationBundleFile?.let { setIconFromBundle(it) }
        if (StringUtil.isEmpty(dictionaryName)) {
            dictionaryName = applicationName
        }
        LOG.info(
            "Dictionary [$dictionaryName] for application [$applicationName] initialized " +
                "In project[${project.name}]  Commands: ${indexes.dictionaryCommandMap.size}. " +
                "Classes: ${indexes.dictionaryClassMap.size}",
        )
    }

    /** Resolve the dictionary icon from the application bundle's Info.plist + ICNS resource. */
    private fun setIconFromBundle(applicationBundleFile: File) {
        try {
            val appUrl = applicationBundleFile.path
            val infoPlist = File("$appUrl/Contents/Info.plist")
            if (!infoPlist.exists() || infoPlist.isDirectory) return
            var dict: PListParser.Dict? = null
            try {
                dict = PListParser.load(infoPlist)
            } catch (e: PListParser.XmlParseException) {
                LOG.warn("Can not parse Info.plist for $applicationName: ${e.message}")
            } catch (e: IOException) {
                LOG.warn("Can not parse Info.plist for $applicationName: ${e.message}")
            }
            var imgFilename: Any? = dict?.get("CFBundleIconFile")
            if (imgFilename == null) {
                imgFilename = applicationName // best-effort guess
            }
            var fileName = imgFilename.toString()
            fileName = if (fileName.endsWith(".icns")) fileName else "$fileName.icns"

            val icnsFile = File("$appUrl/Contents/Resources/$fileName")
            if (!icnsFile.exists() || icnsFile.isDirectory) return

            val parser = IcnsImageParser()
            // TODO: 25/12/15 verify memory management for the BufferedImage list.
            val list: List<BufferedImage>? = parser.getAllBufferedImages(icnsFile)
            if (list.isNullOrEmpty()) return

            val index = if (list.size > 1) list.size - 1 else 0
            val size = JBUI.scale(13)
            val img: Image = list[index].getScaledInstance(size, size, Image.SCALE_SMOOTH)
            applicationIcon = JBImageIcon(img)
        } catch (e: ImageReadException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun processInclude(includedFile: XmlFile): PsiFile? {
        if (includedFile.isValid) {
            val document = includedFile.document
            if (document != null) {
                val rootTag = document.rootTag
                if (rootTag != null) {
                    SDEF_Parser.parseRootTag(this, rootTag)
                }
            }
            includedFiles.add(includedFile)
            LOG.debug("Processed included file:: $includedFile")
        }
        return includedFile
    }

    override fun getProject(): Project = project

    override fun addSuite(suite: Suite): Boolean = mySuites.add(suite)

    override fun getDictionaryFile(): VirtualFile = dictionaryFile

    override fun getDictionaryEnumerationMap(): Map<String, DictionaryEnumeration> = indexes.dictionaryEnumerationMap

    override fun getDictionaryEnumeratorMap(): Map<String, DictionaryEnumerator> = indexes.dictionaryEnumeratorMap

    override fun getDictionaryRecordMap(): Map<String, DictionaryRecord> = indexes.dictionaryRecordMap

    // First-by-name contract preserved (D-03 interface freeze). Use
    // findAllCommandsWithName for 0..N entries on overloaded command names.
    override fun getDictionaryCommandMap(): Map<String, AppleScriptCommand> = indexes.dictionaryCommandMap

    override fun getDictionaryClassMap(): Map<String, AppleScriptClass> = indexes.dictionaryClassMap

    // Null-name guard: `ConcurrentHashMap` rejects null keys with NPE, whereas
    // the pre-fix `HashMap` returned null silently. The interface accepts
    // `String?` so legitimate null inputs flow in (e.g. unresolved handler-call
    // identifiers from the resolver path). Preserve original semantics.
    override fun findClass(name: String?): AppleScriptClass? =
        name?.let { indexes.dictionaryClassMap[it] }

    override fun getParameterNamesForCommand(name: String): List<String>? =
        indexes.dictionaryCommandMap[name]?.getParameterNames()

    override fun findDirectParameterForCommand(commandName: String): CommandDirectParameter? =
        indexes.dictionaryCommandMap[commandName]?.getDirectParameter()

    override fun findProperty(name: String): AppleScriptPropertyDefinition? = indexes.dictionaryPropertyMap[name]

    // D-02: returns 0..N entries for overloaded command names. Dedupe inside the
    // backing list is by CommandData structural equality (see addCommand).
    // .toList() defensive copy so callers cannot mutate the backing list.
    override fun findAllCommandsWithName(name: String): List<AppleScriptCommand> =
        indexes.dictionaryCommandListMap[name]?.toList() ?: emptyList()

    override fun findEnumerator(name: String): DictionaryEnumerator? = indexes.dictionaryEnumeratorMap[name]

    override fun findClassByPluralName(pluralForm: String): AppleScriptClass? =
        indexes.dictionaryClassToPluralNameMap[pluralForm]

    override fun findEnumeration(name: String): DictionaryEnumeration? = indexes.dictionaryEnumerationMap[name]

    /**
     * Convention (locked, matched by SuiteImpl.addCommand): returns true on
     * first insert of a (name, command) pair, false on duplicate name. D-02
     * also populates `dictionaryCommandListMap` so [findAllCommandsWithName]
     * can return all overloaded entries; the list dedupes by `CommandData`
     * structural equality so two impls with identical name + code + parameters
     * + result (e.g. re-ingest of the same SDEF after a cache miss) collapse
     * to one list entry, while genuinely overloaded commands (same name,
     * different parameter signatures) co-exist as N entries.
     *
     * Concurrency: the primary `dictionaryCommandMap.put` is atomic on
     * `ConcurrentHashMap`. For the secondary list-map we use
     * `computeIfAbsent` (CHM-atomic create-or-fetch) backed by a
     * [java.util.concurrent.CopyOnWriteArrayList] so the per-name list's
     * `any { … }` read concurrent with `.add(…)` from another thread is
     * CME-free. The dedupe-then-add sequence is *not* atomic across writers
     * for the same name; this is acceptable because (a) overload-by-name
     * adds are rare in practice (parser walks each suite once), and (b) the
     * worst case is a transient duplicate list entry on a structurally-equal
     * re-insert race — the dedupe check eventually wins on the next
     * `addCommand`-by-name. Strict atomicity would need a per-name mutex;
     * the cost outweighs the benefit for the observed workload.
     */
    override fun addCommand(command: AppleScriptCommand): Boolean {
        val name = command.getName()
        val wasNew = indexes.dictionaryCommandMap.put(name, command) == null
        val list = indexes.dictionaryCommandListMap.computeIfAbsent(name) {
            java.util.concurrent.CopyOnWriteArrayList<AppleScriptCommand>()
        }
        // Structural-equality dedupe via `CommandData` (D-02 closure). The
        // cast is safe because the parser only ever instantiates
        // [AppleScriptCommandImpl]; for unexpected impl types we fall back
        // to reference equality, which is strictly more permissive (no
        // overload-by-content collapse) but never corrupts the list.
        val alreadyPresent = if (command is AppleScriptCommandImpl) {
            list.any { existing -> existing is AppleScriptCommandImpl && existing.commandData == command.commandData }
        } else {
            list.any { it === command }
        }
        if (!alreadyPresent) list.add(command)
        return wasNew
    }

    /** Test-helper accessor for the secondary list-keyed index. */
    @org.jetbrains.annotations.TestOnly
    internal fun getDictionaryCommandListMap(): Map<String, List<AppleScriptCommand>> =
        indexes.dictionaryCommandListMap.mapValues { it.value.toList() }

    override fun addClass(appleScriptClass: AppleScriptClass): Boolean {
        val previous = indexes.dictionaryClassMap.put(appleScriptClass.getName(), appleScriptClass)
        appleScriptClass.getCode()?.let { indexes.dictionaryClassByCodeMap[it] = appleScriptClass }
        indexes.dictionaryClassToPluralNameMap[appleScriptClass.getPluralClassName()] = appleScriptClass
        for (property in appleScriptClass.getProperties()) {
            addProperty(property)
        }
        return previous == null
    }

    override fun getIcon(open: Boolean): Icon = applicationIcon ?: AppleScriptIcons.OPEN_DICTIONARY

    override fun getPresentation(): ItemPresentation = AppleScriptElementPresentation(this)

    override fun findClassByCode(code: String): AppleScriptClass? = indexes.dictionaryClassByCodeMap[code]

    override fun addProperty(property: AppleScriptPropertyDefinition): Boolean =
        indexes.dictionaryPropertyMap.put(property.getName(), property) == null

    override fun addEnumeration(enumeration: DictionaryEnumeration): Boolean {
        val previous = indexes.dictionaryEnumerationMap.put(enumeration.getName(), enumeration)
        for (enumerator in enumeration.getEnumerators().orEmpty()) {
            indexes.dictionaryEnumeratorMap[enumerator.getName()] = enumerator
        }
        return previous == null
    }

    override fun getDocumentation(): String = buildString {
        append(getType()).append(" <b>").append(getName()).append("</b>")
        append("<p>")
        for (suite in mySuites) {
            append("<br>    <b>")
            AppleScriptDocHelper.appendElementLink(this, suite, suite.getName())
            append("</b><br>")
        }
        append("</p>")
    }

    override fun getCode(): String? = null

    override fun getCocoaClassName(): String? = null

    override fun isScriptProperty(): Boolean = false

    override fun isHandler(): Boolean = false

    override fun getOriginalDeclaration(): PsiElement? = null

    override fun isObjectProperty(): Boolean = false

    override fun isVariable(): Boolean = false

    override fun findAssignedValue(): AppleScriptExpression? = null

    override fun getName(): String = dictionaryName

    override fun getApplicationName(): String = applicationName

    @Throws(IncorrectOperationException::class)
    override fun setName(name: String): PsiElement {
        dictionaryName = name
        return this
    }

    override fun getNameIdentifiers(): List<String> = applicationName.split("\\s+".toRegex())

    override fun getQualifiedPath(): String = "dictionary:${getName()}/${getQualifiedName()}"

    override fun getQualifiedName(): String = "${getType()}:${getCode()}"

    override fun getDescription(): String? = null

    override fun getLanguage(): Language = AppleScriptLanguage

    override fun getDictionaryPropertyMap(): Map<String, AppleScriptPropertyDefinition> = indexes.dictionaryPropertyMap

    override fun addRecord(record: DictionaryRecord) {
        indexes.dictionaryRecordMap[record.getName()] = record
        for (prop in record.getProperties()) {
            indexes.dictionaryPropertyMap[prop.getName()] = prop
        }
    }

    override fun getParent(): PsiElement? = PsiManager.getInstance(getProject()).findFile(getDictionaryFile())

    override fun getSuite(): Suite? = null

    override fun getDictionaryParentComponent(): DictionaryComponent? = null

    override fun getType(): String = "dictionary"

    override fun setDescription(description: String?) = Unit

    override fun setDictionaryDoc(documentation: String?) {
        this.documentation = documentation
    }

    override fun getDictionary(): ApplicationDictionary = this

    private fun readDictionaryFromXmlFile(xmlFile1: XmlFile) {
        if (xmlFile1.isValid) {
            xmlFile1.rootTag?.let { setRootTag(it) }
            SDEF_Parser.parse(xmlFile1, this)
            LOG.debug("Dictionary loaded. Virtual file: $xmlFile1")
        }
    }

    override fun getIdentifier(): AppleScriptIdentifier {
        // Mirrors the Java original — NPEs if myRootTag was never set, which would itself indicate a
        // construction/lifecycle bug (the constructor always assigns rootTag via readDictionaryFromXmlFile).
        val rootTag = myRootTag!!
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
        this.myRootTag = myRootTag
        return this
    }

    override fun getRootTag(): XmlTag? = myRootTag

    override fun findSuiteByName(suiteCode: String): Suite? {
        for (suite in mySuites) {
            if (suiteCode == suite.getName()) return suite
        }
        return null
    }

    override fun findSuiteByCode(suiteCode: String): Suite? {
        for (suite in mySuites) {
            if (suiteCode == suite.getCode()) return suite
        }
        return null
    }

    // Null-name guard: `ConcurrentHashMap` rejects null keys with NPE, whereas
    // the pre-fix `HashMap` returned null silently. Preserve original semantics
    // for callers like `AppleScriptDictionaryResolveProcessor` that pass
    // unresolved identifiers (which may be null).
    override fun findCommand(name: String?): AppleScriptCommand? =
        name?.let { indexes.dictionaryCommandMap[it] }

    override fun getAllCommands(): Collection<AppleScriptCommand> = indexes.dictionaryCommandMap.values

    override fun getApplicationBundle(): File? = applicationBundleFile

    companion object {
        @JvmField
        val LOG: Logger = Logger.getInstance("#${ApplicationDictionaryImpl::class.java.name}")

        @JvmStatic
        fun extensionSupported(extension: String?): Boolean =
            extension != null && ApplicationDictionary.SUPPORTED_DICTIONARY_EXTENSIONS.contains(extension.lowercase())
    }
}
