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
import com.intellij.plugin.applescript.lang.sdef.parser.SdefParser
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
import java.util.concurrent.CopyOnWriteArrayList
import javax.swing.Icon

@Suppress("SpellCheckingInspection")
private const val ICON_FILE_EXTENSION = "icns"

private const val DICTIONARY_ICON_SIZE = 13
private const val INFO_PLIST_PATH = "Contents/Info.plist"
private const val ICON_RESOURCES_PATH = "Contents/Resources"

/**
 * PSI representation of one SDEF dictionary. Owns the suite registry plus per-name lookup maps for
 * commands / classes / records / properties / enumerations, eagerly initialised at construction by
 * parsing the bundled XmlFile via [SdefParser]. Bundle metadata (application name, icon) is read
 * from the .app's Info.plist + ICNS resource when available.
 *
 * The method count is intentionally high because this class is the concrete PSI implementation of
 * the broad [ApplicationDictionary] surface. Splitting those overrides into delegate shells would
 * add indirection without reducing domain complexity.
 */
@Suppress("TooManyFunctions")
class ApplicationDictionaryImpl(
    private val project: Project,
    dictionaryXmlFile: XmlFile,
    override var applicationName: String,
    private val applicationBundleFile: File?,
) : FakePsiElement(),
    ApplicationDictionary {
    override val dictionaryFile: VirtualFile = dictionaryXmlFile.virtualFile
    private var applicationIcon: Icon? = null
    private val includedFiles: MutableList<PsiFile> = ArrayList()
    private var dictionaryName: String = ""
    private var dictionaryDocumentation: String? = null

    private var myRootTag: XmlTag? = null
    private val mySuites: MutableList<Suite> = ArrayList()

    // Plan 02-05 / D-07: the 9-map index cluster moved into [DictionaryIndexes]
    // (CHM-backed). Closes the processInclude race latent since v1.0.0; Phase 1
    // explicitly deferred this fix here. v1.3 service split lifts `indexes`
    // wholesale into `SdefIndexService` without touching any consumer site.
    private val indexes: DictionaryIndexes = DictionaryIndexes()

    init {
        readDictionaryFromXmlFile(dictionaryXmlFile)
        applicationIcon = applicationBundleFile?.let { loadIconFromBundle(it, applicationName) }
        if (StringUtil.isEmpty(dictionaryName)) {
            dictionaryName = applicationName
        }
        LOG.info(
            "Dictionary [$dictionaryName] for application [$applicationName] initialized " +
                "In project[${project.name}]  Commands: ${indexes.dictionaryCommandMap.size}. " +
                "Classes: ${indexes.dictionaryClassMap.size}",
        )
    }

    override fun processInclude(includedFile: XmlFile): PsiFile {
        if (!includedFile.isValid) {
            return includedFile
        }
        includedFile.document?.rootTag?.let { SdefParser.parseRootTag(this, it) }
        includedFiles.add(includedFile)
        LOG.debug("Processed included file: $includedFile")
        return includedFile
    }

    override fun getProject(): Project = project

    override fun addSuite(suite: Suite): Boolean = mySuites.add(suite)

    override val dictionaryEnumerationMap: Map<String, DictionaryEnumeration> get() = indexes.dictionaryEnumerationMap

    override val dictionaryEnumeratorMap: Map<String, DictionaryEnumerator> get() = indexes.dictionaryEnumeratorMap

    override val dictionaryRecordMap: Map<String, DictionaryRecord> get() = indexes.dictionaryRecordMap

    // First-by-name contract preserved (D-03 interface freeze). Use
    // findAllCommandsWithName for 0..N entries on overloaded command names.
    override val dictionaryCommandMap: Map<String, AppleScriptCommand> get() = indexes.dictionaryCommandMap

    override val dictionaryClassMap: Map<String, AppleScriptClass> get() = indexes.dictionaryClassMap

    // Null-name guard: `ConcurrentHashMap` rejects null keys with NPE, whereas
    // the pre-fix `HashMap` returned null silently. The interface accepts
    // `String?` so legitimate null inputs flow in (e.g. unresolved handler-call
    // identifiers from the resolver path). Preserve original semantics.
    override fun findClass(name: String?): AppleScriptClass? = name?.let { indexes.dictionaryClassMap[it] }

    override fun getParameterNamesForCommand(name: String): List<String>? = indexes.parameterNames(name)

    override fun findDirectParameterForCommand(commandName: String): CommandDirectParameter? =
        indexes.dictionaryCommandMap[commandName]?.directParameter

    override fun findProperty(name: String): AppleScriptPropertyDefinition? = indexes.dictionaryPropertyMap[name]

    // D-02: returns 0..N entries for overloaded command names. Dedupe inside the
    // backing list is by CommandData structural equality (see addCommand).
    // .toList() defensive copy so callers cannot mutate the backing list.
    override fun findAllCommandsWithName(name: String): List<AppleScriptCommand> =
        indexes.dictionaryCommandListMap[name]?.toList() ?: emptyList()

    override fun findEnumerator(name: String): DictionaryEnumerator? = indexes.dictionaryEnumeratorMap[name]

    override fun findClassByPluralName(pluralForm: String): AppleScriptClass? = indexes.dictionaryClassToPluralNameMap[pluralForm]

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
        val list =
            indexes.dictionaryCommandListMap.computeIfAbsent(name) {
                CopyOnWriteArrayList()
            }
        // Structural-equality dedupe via `CommandData` (D-02 closure). The
        // cast is safe because the parser only ever instantiates
        // [AppleScriptCommandImpl]; for unexpected impl types we fall back
        // to reference equality, which is strictly more permissive (no
        // overload-by-content collapse) but never corrupts the list.
        val alreadyPresent =
            if (command is AppleScriptCommandImpl) {
                list.any { existing ->
                    existing is AppleScriptCommandImpl &&
                        existing.commandData == command.commandData
                }
            } else {
                list.any { it === command }
            }
        if (!alreadyPresent) list.add(command)
        return wasNew
    }

    override fun addClass(appleScriptClass: AppleScriptClass): Boolean {
        val previous = indexes.dictionaryClassMap.put(appleScriptClass.getName(), appleScriptClass)
        indexes.dictionaryClassByCodeMap[appleScriptClass.code] = appleScriptClass
        indexes.dictionaryClassToPluralNameMap[appleScriptClass.pluralClassName] = appleScriptClass
        for (property in appleScriptClass.properties) {
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

    override val documentation: String
        get() =
            buildString {
                append(type).append(" <b>").append(getName()).append("</b>")
                append("<p>")
                for (suite in mySuites) {
                    append("<br>    <b>")
                    AppleScriptDocHelper.appendElementLink(this, suite, suite.getName())
                    append("</b><br>")
                }
                append("</p>")
            }

    override val code: String? get() = null

    override val cocoaClassName: String? get() = null

    override fun getOriginalDeclaration(): PsiElement? = null

    override fun getName(): String = dictionaryName

    @Throws(IncorrectOperationException::class)
    override fun setName(name: String): PsiElement {
        dictionaryName = name
        return this
    }

    override val nameIdentifiers: List<String> get() = applicationName.split("\\s+".toRegex())

    override val qualifiedPath: String get() = "dictionary:${getName()}/$qualifiedName"

    override val qualifiedName: String get() = "$type:$code"

    override var description: String?
        get() = null
        set(_) = Unit

    override fun getLanguage(): Language = AppleScriptLanguage

    override val dictionaryPropertyMap: Map<String, AppleScriptPropertyDefinition> get() = indexes.dictionaryPropertyMap

    override fun addRecord(record: DictionaryRecord) {
        indexes.dictionaryRecordMap[record.getName()] = record
        for (prop in record.getProperties()) {
            indexes.dictionaryPropertyMap[prop.getName()] = prop
        }
    }

    override fun getParent(): PsiElement? = PsiManager.getInstance(getProject()).findFile(dictionaryFile)

    override val suite: Suite? get() = null

    override val dictionaryParentComponent: DictionaryComponent? get() = null

    override val type: String get() = "dictionary"

    override fun setDictionaryDoc(documentation: String?) {
        this.dictionaryDocumentation = documentation
    }

    override val dictionary: ApplicationDictionary get() = this

    private fun readDictionaryFromXmlFile(xmlFile1: XmlFile) {
        if (xmlFile1.isValid) {
            xmlFile1.rootTag?.let { setRootTag(it) }
            SdefParser.parse(xmlFile1, this)
            LOG.debug("Dictionary loaded. Virtual file: $xmlFile1")
        }
    }

    override fun getIdentifier(): AppleScriptIdentifier {
        // D-06 audit: KEPT as `!!` (not requireNotNull). Null is unreachable — the constructor always
        // assigns rootTag via readDictionaryFromXmlFile, so a null here would itself be a
        // construction/lifecycle bug. The `!!` is deliberate: it mirrors the Java original's NPE-on-bug
        // semantics, and requireNotNull would swap that for IllegalArgumentException. Since null never
        // occurs at runtime, the throw-type is unobservable, but we keep the NPE form to preserve intent.
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

    override val rootTag: XmlTag? get() = myRootTag

    override fun findSuiteByName(suiteCode: String): Suite? {
        for (suite in mySuites) {
            if (suiteCode == suite.getName()) return suite
        }
        return null
    }

    override fun findSuiteByCode(suiteCode: String): Suite? {
        for (suite in mySuites) {
            if (suiteCode == suite.code) return suite
        }
        return null
    }

    // Null-name guard: `ConcurrentHashMap` rejects null keys with NPE, whereas
    // the pre-fix `HashMap` returned null silently. Preserve original semantics
    // for callers like `AppleScriptDictionaryResolveProcessor` that pass
    // unresolved identifiers (which may be null).
    override fun findCommand(name: String?): AppleScriptCommand? = name?.let { indexes.dictionaryCommandMap[it] }

    override val allCommands: Collection<AppleScriptCommand> get() = indexes.dictionaryCommandMap.values

    override val applicationBundle: File? get() = applicationBundleFile

    companion object {
        @JvmField
        val LOG: Logger = Logger.getInstance("#${ApplicationDictionaryImpl::class.java.name}")
    }
}

private fun loadIconFromBundle(
    applicationBundleFile: File,
    applicationName: String,
): Icon? {
    val infoPlist = File(applicationBundleFile, INFO_PLIST_PATH)
    val iconFileName = loadIconFileName(infoPlist, applicationName) ?: applicationName
    val iconFile = File(applicationBundleFile, "$ICON_RESOURCES_PATH/${iconFileName.withIconExtension()}")
    return if (iconFile.exists() && !iconFile.isDirectory) {
        loadIconFromFile(iconFile, applicationName)
    } else {
        null
    }
}

private fun loadIconFileName(
    infoPlist: File,
    applicationName: String,
): String? {
    if (!infoPlist.exists() || infoPlist.isDirectory) {
        return null
    }
    val dictionary =
        try {
            PListParser.load(infoPlist)
        } catch (e: PListParser.XmlParseException) {
            logInfoPlistParseFailure(applicationName, e)
            null
        } catch (e: IOException) {
            logInfoPlistParseFailure(applicationName, e)
            null
        }
    return dictionary?.get("CFBundleIconFile")?.toString()
}

private fun DictionaryIndexes.parameterNames(name: String): List<String>? = dictionaryCommandMap[name]?.parameterNames

private fun String.withIconExtension(): String = if (hasIconExtension()) this else "$this.$ICON_FILE_EXTENSION"

private fun String.hasIconExtension(): Boolean = endsWith(".$ICON_FILE_EXTENSION")

private fun loadIconFromFile(
    iconFile: File,
    applicationName: String,
): Icon? =
    try {
        createScaledIcon(IcnsImageParser().getAllBufferedImages(iconFile))
    } catch (e: ImageReadException) {
        logIconLoadFailure(applicationName, iconFile, e)
        null
    } catch (e: IOException) {
        logIconLoadFailure(applicationName, iconFile, e)
        null
    }

private fun createScaledIcon(images: List<BufferedImage>?): Icon? {
    if (images.isNullOrEmpty()) {
        return null
    }
    val iconSize = JBUI.scale(DICTIONARY_ICON_SIZE)
    val image: Image = images.last().getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH)
    return JBImageIcon(image)
}

private fun logInfoPlistParseFailure(
    applicationName: String,
    throwable: Throwable,
) {
    ApplicationDictionaryImpl.LOG.warn("Cannot parse Info.plist for $applicationName", throwable)
}

private fun logIconLoadFailure(
    applicationName: String,
    iconFile: File,
    throwable: Throwable,
) {
    ApplicationDictionaryImpl.LOG.warn(
        "Cannot load dictionary icon for $applicationName from ${iconFile.path}",
        throwable,
    )
}
