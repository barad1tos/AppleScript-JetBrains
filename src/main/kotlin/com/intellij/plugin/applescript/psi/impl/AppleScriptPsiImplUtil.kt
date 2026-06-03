@file:JvmName("AppleScriptPsiImplUtil")
@file:JvmMultifileClass

package com.intellij.plugin.applescript.psi.impl

import com.intellij.openapi.util.Pair
import com.intellij.plugin.applescript.psi.AppleScriptAssignmentStatement
import com.intellij.plugin.applescript.psi.AppleScriptExpression
import com.intellij.plugin.applescript.psi.AppleScriptObjectTargetPropertyDeclaration
import com.intellij.plugin.applescript.psi.AppleScriptPsiElement
import com.intellij.plugin.applescript.psi.AppleScriptTargetListLiteral
import com.intellij.plugin.applescript.psi.AppleScriptTargetRecordLiteral
import com.intellij.util.SmartList
import com.intellij.plugin.applescript.psi.AppleScriptTargetVariable as TargetVariable

/**
 * Static helpers referenced by Grammar-Kit-generated PSI classes through the BNF `methods = [...]`
 * mechanism. Public functions in this multifile facade are JVM-visible as `AppleScriptPsiImplUtil.*`.
 */
fun getTargets(targetList: AppleScriptTargetListLiteral): List<TargetVariable> = targetList.targetVariableList.toList()

fun getTargets(targetRecord: AppleScriptTargetRecordLiteral): List<TargetVariable> {
    val targetVariables = mutableListOf<TargetVariable>()
    addRecordTargetVariablesRecursive(targetRecord, targetVariables)
    return targetVariables
}

fun getTargets(assignmentStatement: AppleScriptAssignmentStatement): List<TargetVariable> =
    assignmentStatement.targetVariable?.let(::listOf)
        ?: assignmentStatement.targetListLiteral?.let(::getTargets)
        ?: assignmentStatement.targetRecordLiteral?.let(::getTargets)
        ?: emptyList()

fun getAssignmentTarget(assignmentStatement: AppleScriptAssignmentStatement): AppleScriptPsiElement? =
    assignmentStatement.targetVariable
        ?: assignmentStatement.targetListLiteral
        ?: assignmentStatement.targetRecordLiteral

fun getTargetsToValuesMapping(
    assignmentStatement: AppleScriptAssignmentStatement,
): List<Pair<AppleScriptPsiElement, AppleScriptExpression>> =
    when (getAssignmentTarget(assignmentStatement)) {
        null -> emptyList()
        else -> SmartList()
    }

private fun addRecordTargetVariablesRecursive(
    targetRecord: AppleScriptTargetRecordLiteral,
    targetVariables: MutableList<TargetVariable>,
) {
    for (property in targetRecord.objectTargetPropertyDeclarationList) {
        addTargetVariablesFromProperty(property, targetVariables)
    }
}

private fun addTargetVariablesFromProperty(
    property: AppleScriptObjectTargetPropertyDeclaration,
    targetVariables: MutableList<TargetVariable>,
) {
    property.targetVariable?.let(targetVariables::add)
    property.targetListLiteral?.let { targetVariables.addAll(it.targetVariableList) }
    property.targetRecordLiteral?.let { addRecordTargetVariablesRecursive(it, targetVariables) }
}
