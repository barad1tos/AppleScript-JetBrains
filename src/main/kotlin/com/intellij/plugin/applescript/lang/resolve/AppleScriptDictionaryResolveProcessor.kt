package com.intellij.plugin.applescript.lang.resolve

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.KeyWithDefaultValue
import com.intellij.plugin.applescript.lang.dictionary.project.AppleScriptProjectDictionaryService
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.lang.sdef.DictionaryComponent
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

    override fun doExecute(
        element: AppleScriptPsiElement,
        state: ResolveState,
    ): Boolean {
        if (element !is ApplicationDictionaryDeclarator) return true

        val collectAll = state.get(COLLECT_ALL_DECLARATIONS) == java.lang.Boolean.TRUE
        val targetElement = myElement

        return if (!collectAll && targetElement != null) {
            executeTargetedDeclaration(element, targetElement)
        } else { // collecting all declarations
            collectDeclaration(element)
            true
        }
    }

    private fun executeTargetedDeclaration(
        element: ApplicationDictionaryDeclarator,
        targetElement: DictionaryCompositeElement,
    ): Boolean {
        val dictionaryRegistry = element.project.getService(AppleScriptProjectDictionaryService::class.java)
        val appName = element.getApplicationName()
        if (dictionaryRegistry == null || appName == null) return true

        val importedDictionary = findOrCreateDictionary(dictionaryRegistry, appName)
        return if (element is AppleScriptUseStatement) {
            collectUseStatement(element, targetElement, dictionaryRegistry, importedDictionary)
            true
        } else if (foundInUseStatementFlag || importedDictionary == null) {
            true
        } else {
            collectedDictionaries.add(importedDictionary)
            !setResult(importedDictionary, targetElement)
        }
    }

    private fun collectUseStatement(
        useStatement: AppleScriptUseStatement,
        targetElement: DictionaryCompositeElement,
        dictionaryRegistry: AppleScriptProjectDictionaryService,
        importedDictionary: ApplicationDictionary?,
    ) {
        mySortedUseStatements.add(useStatement)
        if (!useStatement.withImporting()) return

        importedDictionary?.let(collectedDictionaries::add)
        for (sortedUseStatement in mySortedUseStatements) {
            val currentDictionary = sortedUseStatement.getImportingDictionary(dictionaryRegistry)
            if (currentDictionary != null && setResult(currentDictionary, targetElement)) {
                foundInUseStatementFlag = true
                return
            }
        }
    }

    private fun collectDeclaration(element: ApplicationDictionaryDeclarator) {
        val dictionaryRegistry = element.project.getService(AppleScriptProjectDictionaryService::class.java)
        val importedDictionary = dictionaryRegistry?.let(element::getImportedDictionary)

        if (element is AppleScriptUseStatement) {
            mySortedUseStatements.add(element)
            if (element.withImporting()) {
                importedDictionary?.let(collectedDictionaries::add)
            }
        } else {
            importedDictionary?.let(collectedDictionaries::add)
        }
    }

    private fun setResultFromTerminology(
        element: DictionaryCompositeElement,
        terminologyProvider: AppleScriptProjectDictionaryService.() -> ApplicationDictionary?,
    ): Boolean {
        val dictionaryRegistry =
            element.project.getService(AppleScriptProjectDictionaryService::class.java)
                ?: return false

        val terminology = dictionaryRegistry.terminologyProvider()
        return terminology != null && setResult(terminology, element)
    }

    private fun setResult(
        importedDictionary: ApplicationDictionary,
        element: DictionaryCompositeElement,
    ): Boolean {
        val res: DictionaryComponent? =
            when (element) {
                is AppleScriptCommandHandlerCall -> importedDictionary.findCommand(myElementName)
                is AppleScriptDictionaryClassName, is AppleScriptBuiltInClassIdentifier ->
                    importedDictionary.findClass(myElementName)
                is AppleScriptDictionaryPropertyName -> importedDictionary.findProperty(myElementName)
                is AbstractDictionaryConstantSpecifier -> importedDictionary.findEnumerator(myElementName)
                is AppleScriptDictionaryClassIdentifierPlural ->
                    importedDictionary.findClassByPluralName(myElementName)
                        ?: importedDictionary.findClass(myElementName)
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

    override fun handleEvent(
        event: PsiScopeProcessor.Event,
        associated: Any?,
    ) = Unit

    fun getMyResult(): DictionaryComponent? {
        val targetElement = myElement.takeIf { myResult == null }
        // No terms in <tell>/<using terms>/<use> statements — fall back to CocoaStandard (always
        // available for scriptable apps), and to StandardAdditions only when there were no <use> stmts.
        if (targetElement != null) {
            val resolvedFromCocoa =
                setResultFromTerminology(
                    targetElement,
                    AppleScriptProjectDictionaryService::getCocoaStandardTerminology,
                )
            if (!resolvedFromCocoa && mySortedUseStatements.isEmpty()) {
                setResultFromTerminology(
                    targetElement,
                    AppleScriptProjectDictionaryService::getScriptingAdditionsTerminology,
                )
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
                mySortedUseStatements.isNotEmpty(),
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
            val dictionaryRegistry =
                project.getService(AppleScriptProjectDictionaryService::class.java)
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
            if (importedDict == null) {
                return
            }

            if (withCocoaStdLibFiltering) {
                collectComponentsMissingFromCocoa(importedDict, dictionaryComponents)
            } else {
                collectAllComponents(importedDict, dictionaryComponents)
            }
        }

        private fun collectComponentsMissingFromCocoa(
            importedDict: ApplicationDictionary,
            dictionaryComponents: MutableCollection<DictionaryComponent>,
        ) {
            val cocoaStandard =
                importedDict.project
                    .getService(AppleScriptProjectDictionaryService::class.java)
                    ?.getCocoaStandardTerminology()

            if (cocoaStandard != null) {
                importedDict.dictionaryEnumeratorMap.values.filterTo(dictionaryComponents) {
                    cocoaStandard.findEnumerator(it.getName()) == null
                }
                importedDict.dictionaryClassMap.values.filterTo(dictionaryComponents) {
                    cocoaStandard.findClass(it.getName()) == null
                }
                importedDict.allCommands.filterTo(dictionaryComponents) {
                    cocoaStandard.findCommand(it.getName()) == null
                }
                importedDict.dictionaryPropertyMap.values.filterTo(dictionaryComponents) {
                    cocoaStandard.findCommand(it.getName()) == null
                }
            }
        }

        private fun collectAllComponents(
            importedDict: ApplicationDictionary,
            dictionaryComponents: MutableCollection<DictionaryComponent>,
        ) {
            dictionaryComponents.addAll(importedDict.dictionaryEnumeratorMap.values)
            dictionaryComponents.addAll(importedDict.dictionaryPropertyMap.values)
            dictionaryComponents.addAll(importedDict.dictionaryClassMap.values)
            dictionaryComponents.addAll(importedDict.allCommands)
        }
    }
}

private fun ApplicationDictionaryDeclarator.getImportedDictionary(
    dictionaryRegistry: AppleScriptProjectDictionaryService,
): ApplicationDictionary? {
    val appName = getApplicationName() ?: return null
    return findOrCreateDictionary(dictionaryRegistry, appName)
}

private fun AppleScriptUseStatement.getImportingDictionary(
    dictionaryRegistry: AppleScriptProjectDictionaryService,
): ApplicationDictionary? =
    takeIf { it.withImporting() }
        ?.getApplicationName()
        ?.takeUnless(String::isEmpty)
        ?.let(dictionaryRegistry::getDictionary)

private fun findOrCreateDictionary(
    dictionaryRegistry: AppleScriptProjectDictionaryService,
    appName: String,
): ApplicationDictionary? = dictionaryRegistry.getDictionary(appName) ?: dictionaryRegistry.createDictionary(appName)
