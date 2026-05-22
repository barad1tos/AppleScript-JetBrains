package com.intellij.plugin.applescript.lang.resolve

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.KeyWithDefaultValue
import com.intellij.openapi.util.text.StringUtil
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptProjectDictionaryService
import com.intellij.plugin.applescript.lang.sdef.AppleScriptClass
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand
import com.intellij.plugin.applescript.lang.sdef.AppleScriptPropertyDefinition
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.lang.sdef.DictionaryComponent
import com.intellij.plugin.applescript.lang.sdef.DictionaryEnumerator
import com.intellij.plugin.applescript.psi.AppleScriptBuiltInClassIdentifier
import com.intellij.plugin.applescript.psi.AppleScriptDictionaryClassIdentifierPlural
import com.intellij.plugin.applescript.psi.AppleScriptDictionaryClassName
import com.intellij.plugin.applescript.psi.AppleScriptDictionaryPropertyName
import com.intellij.plugin.applescript.psi.AppleScriptPsiElement
import com.intellij.plugin.applescript.psi.AppleScriptUseStatement
import com.intellij.plugin.applescript.psi.sdef.AppleScriptCommandHandlerCall
import com.intellij.plugin.applescript.psi.sdef.ApplicationDictionaryDeclarator
import com.intellij.plugin.applescript.psi.sdef.DictionaryCompositeElement
import com.intellij.plugin.applescript.psi.sdef.impl.AbstractDictionaryConstantSpecifier
import com.intellij.psi.ResolveState
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.util.containers.SortedList

class AppleScriptDictionaryResolveProcessor : AppleScriptPsiScopeProcessor {

    private val myProject: Project
    private val myElement: DictionaryCompositeElement?
    private val myElementName: String

    private val mySortedUseStatements: SortedList<AppleScriptUseStatement> =
        SortedList { o1, o2 -> -(o1.textOffset - o2.textOffset) }
    private val collectedDictionaries: MutableList<ApplicationDictionary> = ArrayList()

    private var myResult: DictionaryComponent? = null
    private var foundInUseStatementFlag = false

    constructor(dictionaryElement: DictionaryCompositeElement, elementName: String) {
        myElement = dictionaryElement
        myElementName = elementName
        myProject = myElement.project
    }

    constructor(project: Project, elementName: String) {
        myProject = project
        myElementName = elementName
        myElement = null
    }

    override fun doExecute(element: AppleScriptPsiElement, state: ResolveState): Boolean {
        if (element !is ApplicationDictionaryDeclarator) return true
        val collectAll = state.get(COLLECT_ALL_DECLARATIONS) == java.lang.Boolean.TRUE

        if (!collectAll && myElement != null) {
            val dictionaryRegistry = element.project.getService(AppleScriptProjectDictionaryService::class.java)
            val appName = element.getApplicationName()
            if (appName == null || dictionaryRegistry == null) return true

            val importedDictionary = dictionaryRegistry.getDictionary(appName)
                ?: dictionaryRegistry.createDictionary(appName)

            if (element is AppleScriptUseStatement) {
                mySortedUseStatements.add(element)
                if (element.withImporting()) {
                    importedDictionary?.let { collectedDictionaries.add(it) }

                    // Result := the closest application dictionary across all use statements.
                    for (sortedUseStatement in mySortedUseStatements) {
                        if (!sortedUseStatement.withImporting()) continue
                        val currAppName = sortedUseStatement.getApplicationName()
                        if (!StringUtil.isEmpty(currAppName)) {
                            val currentDictionary = dictionaryRegistry.getDictionary(currAppName!!)
                            if (currentDictionary != null) {
                                // The sorted list ordering means the first match is the closest use statement.
                                if (setResult(currentDictionary, myElement)) {
                                    foundInUseStatementFlag = true
                                    break
                                }
                            }
                        }
                    }
                }
            } else if (!foundInUseStatementFlag) {
                if (importedDictionary != null) {
                    collectedDictionaries.add(importedDictionary)
                    return !setResult(importedDictionary, myElement)
                }
            }
            return true
        } else { // collecting all declarations
            val dictionaryRegistry = element.project.getService(AppleScriptProjectDictionaryService::class.java)
            val appName = element.getApplicationName()
            var importedDictionary: ApplicationDictionary? = null
            if (appName != null && dictionaryRegistry != null) {
                importedDictionary = dictionaryRegistry.getDictionary(appName)
                    ?: dictionaryRegistry.createDictionary(appName)
            }
            if (element is AppleScriptUseStatement) {
                mySortedUseStatements.add(element)
                if (element.withImporting() && importedDictionary != null) {
                    collectedDictionaries.add(importedDictionary)
                }
            } else if (importedDictionary != null) {
                collectedDictionaries.add(importedDictionary)
            }
        }
        return true
    }

    private fun setResultFromCocoaStandardLibrary(element: DictionaryCompositeElement): Boolean {
        val dictionaryRegistry = element.project.getService(AppleScriptProjectDictionaryService::class.java)
            ?: return false
        val cocoaStandard = dictionaryRegistry.getCocoaStandardTerminology()
        return cocoaStandard != null && setResult(cocoaStandard, element)
    }

    private fun setResultFromStandardAdditionsLibrary(element: DictionaryCompositeElement): Boolean {
        val dictionaryRegistry = element.project.getService(AppleScriptProjectDictionaryService::class.java)
            ?: return false
        val scriptingAdditions = dictionaryRegistry.getScriptingAdditionsTerminology()
        return scriptingAdditions != null && setResult(scriptingAdditions, element)
    }

    private fun setResult(
        importedDictionary: ApplicationDictionary,
        element: DictionaryCompositeElement,
    ): Boolean {
        val res: DictionaryComponent? = when (element) {
            is AppleScriptCommandHandlerCall -> importedDictionary.findCommand(myElementName)
            is AppleScriptDictionaryClassName, is AppleScriptBuiltInClassIdentifier ->
                importedDictionary.findClass(myElementName)
            is AppleScriptDictionaryPropertyName -> importedDictionary.findProperty(myElementName)
            is AbstractDictionaryConstantSpecifier -> importedDictionary.findEnumerator(myElementName)
            is AppleScriptDictionaryClassIdentifierPlural ->
                importedDictionary.findClassByPluralName(myElementName)
            else -> {
                LOG.warn("WARNING: unknown dictionary reference element: ${element.javaClass.name}")
                null
            }
        }
        if (res != null) {
            myResult = res
            return true
        }
        return false
    }

    override fun <T> getHint(hintKey: Key<T>): T? = null

    override fun handleEvent(event: PsiScopeProcessor.Event, associated: Any?) = Unit

    fun getMyResult(): DictionaryComponent? {
        if (myResult == null && myElement != null) {
            // No terms in <tell>/<using terms>/<use> statements — fall back to CocoaStandard (always
            // available for scriptable apps), and to StandardAdditions only when there were no <use> stmts.
            if (!setResultFromCocoaStandardLibrary(myElement) && mySortedUseStatements.size == 0) {
                setResultFromStandardAdditionsLibrary(myElement)
            }
        }
        return myResult
    }

    fun getFilteredResult(): List<DictionaryComponent> {
        val result = ArrayList<DictionaryComponent>()
        var filterStdCocoaTerminologyFlag = false // true once we've collected from a non-additions dictionary
        val dictionaryRegistry = myProject.getService(AppleScriptProjectDictionaryService::class.java)
        for (collectedDictionary in collectedDictionaries) {
            collectAllComponentsFromDictionary(collectedDictionary, result, filterStdCocoaTerminologyFlag)
            filterStdCocoaTerminologyFlag = filterStdCocoaTerminologyFlag ||
                collectedDictionary.getName() != ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY
        }
        if (dictionaryRegistry != null) {
            appendResultsIfNeeded(
                result,
                myProject,
                mySortedUseStatements.size > 0,
                collectedDictionaries.contains(dictionaryRegistry.getScriptingAdditionsTerminology()),
                filterStdCocoaTerminologyFlag,
            )
        }
        return result
    }

    companion object {
        @JvmField
        val COLLECT_ALL_DECLARATIONS: Key<Boolean> =
            object : KeyWithDefaultValue<Boolean>("apple.script.resolve.state.collect.all") {
                override fun getDefaultValue(): Boolean = false
            }

        private val LOG: Logger =
            Logger.getInstance("#${AppleScriptDictionaryResolveProcessor::class.java.name}")

        private fun appendResultsIfNeeded(
            result: MutableCollection<DictionaryComponent>,
            project: Project,
            areThereUseStatements: Boolean,
            filterStandardAdditions: Boolean,
            filterCocoaStandard: Boolean,
        ) {
            val dictionaryRegistry = project.getService(AppleScriptProjectDictionaryService::class.java)
                ?: return
            if (!filterStandardAdditions && !areThereUseStatements) {
                val scriptAdditions = dictionaryRegistry.getScriptingAdditionsTerminology()
                collectAllComponentsFromDictionary(scriptAdditions, result, false)
            }
            if (!filterCocoaStandard) {
                val cocoaStandardTerminology = dictionaryRegistry.getCocoaStandardTerminology()
                collectAllComponentsFromDictionary(cocoaStandardTerminology, result, false)
            }
        }

        /**
         * Gather all term declarations from [importedDict].
         *
         * @param withCocoaStdLibFiltering if true, omit terms already present in the Standard Terminology
         */
        private fun collectAllComponentsFromDictionary(
            importedDict: ApplicationDictionary?,
            dictionaryComponents: MutableCollection<DictionaryComponent>,
            withCocoaStdLibFiltering: Boolean,
        ) {
            if (importedDict == null) return
            if (withCocoaStdLibFiltering) {
                val dictionaryRegistry =
                    importedDict.getProject().getService(AppleScriptProjectDictionaryService::class.java)
                val cocoaStandard: ApplicationDictionary? = dictionaryRegistry?.getCocoaStandardTerminology()
                if (cocoaStandard != null) {
                    for (dicConst: DictionaryEnumerator in importedDict.getDictionaryEnumeratorMap().values) {
                        if (cocoaStandard.findEnumerator(dicConst.getName()) == null) {
                            dictionaryComponents.add(dicConst)
                        }
                    }
                    for (clz: AppleScriptClass in importedDict.getDictionaryClassMap().values) {
                        if (cocoaStandard.findClass(clz.getName()) == null) {
                            dictionaryComponents.add(clz)
                        }
                    }
                    for (cmd: AppleScriptCommand in importedDict.getAllCommands()) {
                        if (cocoaStandard.findCommand(cmd.getName()) == null) {
                            dictionaryComponents.add(cmd)
                        }
                    }
                    for (prop: AppleScriptPropertyDefinition in importedDict.getDictionaryPropertyMap().values) {
                        if (cocoaStandard.findCommand(prop.getName()) == null) {
                            dictionaryComponents.add(prop)
                        }
                    }
                }
            } else {
                dictionaryComponents.addAll(importedDict.getDictionaryEnumeratorMap().values)
                dictionaryComponents.addAll(importedDict.getDictionaryPropertyMap().values)
                dictionaryComponents.addAll(importedDict.getDictionaryClassMap().values)
                dictionaryComponents.addAll(importedDict.getAllCommands())
            }
        }
    }
}
