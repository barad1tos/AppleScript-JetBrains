package com.intellij.plugin.applescript.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.text.StringUtil
import com.intellij.plugin.applescript.lang.resolve.AppleScriptResolveUtil
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.psi.AppleScriptApplicationReference
import com.intellij.plugin.applescript.psi.AppleScriptArgumentSelector
import com.intellij.plugin.applescript.psi.AppleScriptAssignmentStatement
import com.intellij.plugin.applescript.psi.AppleScriptComponent
import com.intellij.plugin.applescript.psi.AppleScriptDirectParameterDeclaration
import com.intellij.plugin.applescript.psi.AppleScriptExpression
import com.intellij.plugin.applescript.psi.AppleScriptFormalParameterList
import com.intellij.plugin.applescript.psi.AppleScriptHandlerLabeledParametersDefinition
import com.intellij.plugin.applescript.psi.AppleScriptIdentifier
import com.intellij.plugin.applescript.psi.AppleScriptLabeledParameterDeclarationList
import com.intellij.plugin.applescript.psi.AppleScriptLabeledParameterDeclarationPart
import com.intellij.plugin.applescript.psi.AppleScriptListFormalParameter
import com.intellij.plugin.applescript.psi.AppleScriptObjectNamedPropertyDeclaration
import com.intellij.plugin.applescript.psi.AppleScriptObjectTargetPropertyDeclaration
import com.intellij.plugin.applescript.psi.AppleScriptPsiElement
import com.intellij.plugin.applescript.psi.AppleScriptRecordFormalParameter
import com.intellij.plugin.applescript.psi.AppleScriptSimpleFormalParameter
import com.intellij.plugin.applescript.psi.AppleScriptTargetListLiteral
import com.intellij.plugin.applescript.psi.AppleScriptTargetRecordLiteral
import com.intellij.plugin.applescript.psi.AppleScriptTargetVariable
import com.intellij.plugin.applescript.psi.AppleScriptTellCompoundStatement
import com.intellij.plugin.applescript.psi.AppleScriptTellSimpleStatement
import com.intellij.plugin.applescript.psi.AppleScriptTokenTypesSets
import com.intellij.plugin.applescript.psi.AppleScriptTypes
import com.intellij.plugin.applescript.psi.AppleScriptUseStatement
import com.intellij.plugin.applescript.psi.AppleScriptUsingTermsFromStatement
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.SmartList
import com.intellij.util.containers.SortedList

/**
 * Static helpers referenced by Grammar-Kit-generated PSI classes via the BNF `methods = [...]` mechanism.
 * Every public function needs `@JvmStatic` so the generated Java proxy methods can find them.
 */
object AppleScriptPsiImplUtil {

    @JvmStatic
    fun getTargets(targetList: AppleScriptTargetListLiteral): List<AppleScriptTargetVariable> =
        targetList.targetVariableList.toList()

    @JvmStatic
    fun getTargets(targetRecord: AppleScriptTargetRecordLiteral): List<AppleScriptTargetVariable> {
        val targetVariables = mutableListOf<AppleScriptTargetVariable>()
        addRecordTargetVariablesRecursive(targetRecord, targetVariables)
        return targetVariables
    }

    @JvmStatic
    fun getTargets(assignmentStatement: AppleScriptAssignmentStatement): List<AppleScriptTargetVariable> {
        assignmentStatement.targetVariable?.let { return listOf(it) }
        assignmentStatement.targetListLiteral?.let { return getTargets(it) }
        assignmentStatement.targetRecordLiteral?.let { return getTargets(it) }
        return emptyList()
    }

    @JvmStatic
    fun getParameters(listFormalParameter: AppleScriptListFormalParameter): List<AppleScriptSimpleFormalParameter> =
        listFormalParameter.simpleFormalParameterList.toList()

    @JvmStatic
    fun getParameters(recordParameter: AppleScriptRecordFormalParameter): List<AppleScriptSimpleFormalParameter> {
        val parameters = mutableListOf<AppleScriptSimpleFormalParameter>()
        addRecordFormalParameterRecursive(recordParameter, parameters)
        return parameters
    }

    @JvmStatic
    fun getFormalParameters(parameterList: AppleScriptFormalParameterList): List<AppleScriptComponent> {
        val parameters = mutableListOf<AppleScriptComponent>()
        parameters.addAll(parameterList.simpleFormalParameterList)
        for (listOfParams in parameterList.listFormalParameterList) {
            parameters.addAll(getParameters(listOfParams))
        }
        for (recordOfParams in parameterList.recordFormalParameterList) {
            parameters.addAll(getParameters(recordOfParams))
        }
        return parameters
    }

    @JvmStatic
    fun getParameterComponentList(handler: AppleScriptHandlerLabeledParametersDefinition): List<AppleScriptComponent> {
        val parameterList = handler.labeledParameterDeclarationList
        val givenProperties: List<AppleScriptObjectTargetPropertyDeclaration> = handler.objectTargetPropertyDeclarationList
        return buildList {
            parameterList.directParameterDeclaration?.let(::add)
            parameterList.targetListLiteral?.let { addAll(getTargets(it)) }
            parameterList.targetVariable?.let(::add)
            parameterList.targetRecordLiteral?.let { addAll(getTargets(it)) }
            addAll(parameterList.labeledParameterDeclarationPartList)
            addAll(givenProperties)
        }
    }

    @JvmStatic
    fun getComponentList(parametersDeclaration: AppleScriptLabeledParameterDeclarationList): List<AppleScriptComponent> {
        val directParameter: AppleScriptDirectParameterDeclaration? = parametersDeclaration.directParameterDeclaration
        val labeledParameters: List<AppleScriptLabeledParameterDeclarationPart> = parametersDeclaration.labeledParameterDeclarationPartList
        return buildList {
            directParameter?.let(::add)
            parametersDeclaration.targetListLiteral?.let { addAll(getTargets(it)) }
            parametersDeclaration.targetVariable?.let(::add)
            parametersDeclaration.targetRecordLiteral?.let { addAll(getTargets(it)) }
            addAll(labeledParameters)
        }
    }

    @JvmStatic
    fun getAssignmentTarget(assignmentStatement: AppleScriptAssignmentStatement): AppleScriptPsiElement? =
        assignmentStatement.targetVariable
            ?: assignmentStatement.targetListLiteral
            ?: assignmentStatement.targetRecordLiteral

    @JvmStatic
    fun getTargetsToValuesMapping(
        assignmentStatement: AppleScriptAssignmentStatement,
    ): List<Pair<AppleScriptPsiElement, AppleScriptExpression>> = SmartList()

    @JvmStatic
    fun getSelectorIdentifier(argumentSelector: AppleScriptArgumentSelector): AppleScriptIdentifier? =
        PsiTreeUtil.findChildOfType(argumentSelector, AppleScriptIdentifier::class.java)

    @JvmStatic
    fun getSelectorName(argumentSelector: AppleScriptArgumentSelector): String {
        val result = StringBuilder()
        var child: ASTNode? = argumentSelector.node.firstChildNode
        while (child != null) {
            val tt = child.elementType
            if (tt == AppleScriptTypes.IDENTIFIER || tt == AppleScriptTypes.COLON) {
                result.append(child.text)
            }
            child = child.treeNext
        }
        return result.toString()
    }

    @JvmStatic
    fun isWhiteSpaceOrNls(node: ASTNode?): Boolean =
        node != null && AppleScriptTokenTypesSets.WHITE_SPACES_SET.contains(node.elementType)

    @JvmStatic
    fun isBefore(e1: PsiElement, e2: PsiElement, strict: Boolean): Boolean =
        if (strict) e1.textOffset < e2.textOffset else e1.textOffset <= e2.textOffset

    @JvmStatic
    fun getApplicationNameForElementInsideTellStatement(element: PsiElement): List<String> {
        val resolveScope: SortedList<PsiElement> = AppleScriptResolveUtil.getTellStatementResolveScope(element)
        val result = mutableListOf<String>()
        for (tellStatement in resolveScope) {
            val appRef = findApplicationNameFromTellStatement(tellStatement)
            if (!StringUtil.isEmpty(appRef)) {
                result.add(appRef!!)
            }
        }
        return result
    }

    @JvmStatic
    fun findApplicationNameFromTellStatement(tellStatement: PsiElement): String? {
        val appRef: AppleScriptApplicationReference? = PsiTreeUtil.findChildOfType(
            tellStatement, AppleScriptApplicationReference::class.java,
        )
        return getNameFromApplicationReference(appRef)
    }

    @JvmStatic
    fun getNameFromApplicationReference(appRef: AppleScriptApplicationReference?): String? {
        if (appRef == null) return null
        val text = appRef.text
        val from = text.indexOf('"') + 1
        val to = text.indexOf('"', from)
        return if (from in 0..text.length && to in 0..text.length && from <= to) {
            text.substring(from, to)
        } else {
            null
        }
    }

    @JvmStatic
    fun getApplicationName(useStatement: AppleScriptUseStatement): String? = when {
        useStatement.text.contains("application") -> {
            val appRef = PsiTreeUtil.findChildOfType(useStatement, AppleScriptApplicationReference::class.java)
            appRef?.node?.findChildByType(AppleScriptTypes.STRING_LITERAL)?.text?.replace("\"", "")
        }

        useStatement.node.findChildByType(AppleScriptTypes.SCRIPTING_ADDITIONS) != null ->
            ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY

        else -> null
    }

    @JvmStatic
    fun useStandardAdditions(useStatement: AppleScriptUseStatement): Boolean =
        useStatement.node.findChildByType(AppleScriptTypes.SCRIPTING_ADDITIONS) != null

    @JvmStatic
    fun withImporting(useStatement: AppleScriptUseStatement): Boolean {
        val text = useStatement.node.text
        return !text.contains("without") && !text.contains("false")
    }

    @JvmStatic
    fun getApplicationName(usingTermsStatement: AppleScriptUsingTermsFromStatement): String? {
        val appRef = usingTermsStatement.applicationReference
        return appRef?.applicationName
            ?: if (withImportingStdLibrary(usingTermsStatement)) {
                ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY
            } else {
                null
            }
    }

    @JvmStatic
    fun withImportingStdLibrary(usingTermsStatement: AppleScriptUsingTermsFromStatement): Boolean =
        usingTermsStatement.node.findChildByType(AppleScriptTypes.SCRIPTING_ADDITIONS) != null

    @JvmStatic
    fun getApplicationName(applicationReference: AppleScriptApplicationReference): String? =
        getNameFromApplicationReference(applicationReference)

    @JvmStatic
    fun isInsideTellStatement(element: PsiElement): Boolean {
        var parent: PsiElement? = element
        while (parent != null) {
            parent = parent.parent
            if (parent is AppleScriptTellSimpleStatement || parent is AppleScriptTellCompoundStatement) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun getApplicationReference(tell: AppleScriptTellCompoundStatement): AppleScriptApplicationReference? =
        PsiTreeUtil.getChildOfType(tell, AppleScriptApplicationReference::class.java)

    @JvmStatic
    fun getApplicationName(tellCompound: AppleScriptTellCompoundStatement): String? =
        tellCompound.applicationReference?.applicationName

    @JvmStatic
    fun getApplicationReference(tell: AppleScriptTellSimpleStatement): AppleScriptApplicationReference? =
        PsiTreeUtil.getChildOfType(tell, AppleScriptApplicationReference::class.java)

    @JvmStatic
    fun getApplicationName(tellSimple: AppleScriptTellSimpleStatement): String? =
        PsiTreeUtil.findChildOfType(tellSimple, AppleScriptApplicationReference::class.java)?.applicationName

    private fun addRecordTargetVariablesRecursive(
        targetRecord: AppleScriptTargetRecordLiteral,
        targetVariables: MutableList<AppleScriptTargetVariable>,
    ) {
        for (property in targetRecord.objectTargetPropertyDeclarationList) {
            property.targetVariable?.let { targetVariables.add(it) }
            property.targetListLiteral?.let { targetVariables.addAll(it.targetVariableList) }
            property.targetRecordLiteral?.let { addRecordTargetVariablesRecursive(it, targetVariables) }
        }
    }

    private fun addRecordFormalParameterRecursive(
        recordParameter: AppleScriptRecordFormalParameter,
        parameters: MutableList<AppleScriptSimpleFormalParameter>,
    ) {
        for (property: AppleScriptObjectNamedPropertyDeclaration in recordParameter.objectNamedPropertyDeclarationList) {
            property.simpleFormalParameter?.let { parameters.add(it) }
            property.listFormalParameter?.let { parameters.addAll(it.simpleFormalParameterList) }
            property.recordFormalParameter?.let { addRecordFormalParameterRecursive(it, parameters) }
        }
    }
}
