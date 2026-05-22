package com.intellij.plugin.applescript.lang.ide.sdef

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.psi.sdef.impl.ApplicationDictionaryImpl
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import java.io.File

/**
 * Manages dictionaries for the project. Dictionaries created here are cached for the session.
 */
@Service(Service.Level.PROJECT)
class AppleScriptProjectDictionaryService(private val project: Project) {

    private val dictionaryRegistryService: AppleScriptSystemDictionaryRegistryService =
        ApplicationManager.getApplication()
            .getService(AppleScriptSystemDictionaryRegistryService::class.java)

    private val dictionaryMap: MutableMap<String, ApplicationDictionary> = HashMap()

    /** Returns the terminology available by default in every script (Scripting Additions). */
    fun getScriptingAdditionsTerminology(): ApplicationDictionary? {
        val name = ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY
        return getDictionary(name) ?: createDictionary(name)
    }

    /** Returns the terminology available by default in any Cocoa scripting application (Cocoa Standard). */
    fun getCocoaStandardTerminology(): ApplicationDictionary? {
        val name = ApplicationDictionary.COCOA_STANDARD_LIBRARY
        return getDictionary(name) ?: createDictionary(name)
    }

    /**
     * Creates the dictionary PSI class for the named application. Cached dictionary files and standard
     * application paths are consulted; null if creation failed.
     */
    @Synchronized
    fun createDictionary(applicationName: String): ApplicationDictionary? {
        if (isInIgnoreList(applicationName, null)) return null
        getDictionary(applicationName)?.let { return it }
        val info = dictionaryRegistryService.getInitializedInfo(applicationName)
        if (info != null) {
            return createDictionaryFromInfo(info)
        }
        LOG.warn("Failed to get initialized dictionary info for $applicationName")
        return null
    }

    private fun createDictionaryFromInfo(info: DictionaryInfo): ApplicationDictionary? {
        if (!info.isInitialized()) {
            LOG.error(
                "Attempt to create dictionary for not initialized Dictionary Info for application" +
                    info.getApplicationName(),
            )
            return null
        }
        val applicationName = info.getApplicationName()
        val vFile = LocalFileSystem.getInstance().findFileByIoFile(info.getDictionaryFile())
        if (vFile != null && vFile.isValid) {
            val xmlFile = PsiManager.getInstance(project).findFile(vFile) as? XmlFile
            if (xmlFile != null) {
                val dictionary = ApplicationDictionaryImpl(project, xmlFile, applicationName, info.getApplicationFile())
                dictionaryMap[applicationName] = dictionary
                return dictionary
            }
        }
        LOG.warn("Failed to create dictionary from info for application: $applicationName. Reason: file is null")
        return null
    }

    /**
     * Generates the dictionary file for the application, initialises its terms for the parser, and creates
     * the [ApplicationDictionary] PSI class for the project.
     */
    @Synchronized
    fun createDictionaryFromFile(applicationName: String, applicationFile: VirtualFile): ApplicationDictionary? {
        val appIoFile = File(applicationFile.path)
        val info = dictionaryRegistryService.createAndInitializeInfo(appIoFile, applicationName)
        if (info != null) {
            return createDictionaryFromInfo(info)
        }
        LOG.warn("Failed to get initialized dictionary info for $applicationName from $applicationFile")
        return null
    }

    /**
     * @return true if the application is either not scriptable or wasn't found in the system, false otherwise.
     */
    private fun isInIgnoreList(applicationName: String, applicationFile: VirtualFile?): Boolean {
        if (dictionaryRegistryService.isNotScriptable(applicationName)) {
            LOG.debug("Application $applicationName is not scriptable. Can not create dictionary for it.")
            return true
        }
        if (applicationFile == null && dictionaryRegistryService.isInUnknownList(applicationName)) {
            LOG.debug(
                "WARNING: Application $applicationName was added to unknown list. " +
                    "Can not create dictionary for it.",
            )
            return true
        }
        return false
    }

    fun getDictionary(applicationName: String): ApplicationDictionary? = dictionaryMap[applicationName]

    fun getDictionaries(): Collection<ApplicationDictionary> = dictionaryMap.values

    companion object {
        private val LOG: Logger = Logger.getInstance("#${AppleScriptProjectDictionaryService::class.java.name}")
    }
}
