@file:JvmName("AppleScriptPsiImplUtil")
@file:JvmMultifileClass

package com.intellij.plugin.applescript.psi.impl

import com.intellij.plugin.applescript.psi.AppleScriptComponent
import com.intellij.plugin.applescript.psi.AppleScriptDirectParameterDeclaration
import com.intellij.plugin.applescript.psi.AppleScriptFormalParameterList
import com.intellij.plugin.applescript.psi.AppleScriptHandlerLabeledParametersDefinition
import com.intellij.plugin.applescript.psi.AppleScriptLabeledParameterDeclarationList
import com.intellij.plugin.applescript.psi.AppleScriptLabeledParameterDeclarationPart
import com.intellij.plugin.applescript.psi.AppleScriptListFormalParameter
import com.intellij.plugin.applescript.psi.AppleScriptObjectNamedPropertyDeclaration
import com.intellij.plugin.applescript.psi.AppleScriptObjectTargetPropertyDeclaration
import com.intellij.plugin.applescript.psi.AppleScriptRecordFormalParameter
import com.intellij.plugin.applescript.psi.AppleScriptSimpleFormalParameter

fun getParameters(listFormalParameter: AppleScriptListFormalParameter): List<AppleScriptSimpleFormalParameter> =
    listFormalParameter.simpleFormalParameterList.toList()

fun getParameters(recordParameter: AppleScriptRecordFormalParameter): List<AppleScriptSimpleFormalParameter> {
    val parameters = mutableListOf<AppleScriptSimpleFormalParameter>()
    addRecordFormalParameterRecursive(recordParameter, parameters)
    return parameters
}

fun getFormalParameters(parameterList: AppleScriptFormalParameterList): List<AppleScriptComponent> {
    val parameters = mutableListOf<AppleScriptComponent>()
    parameters.addAll(parameterList.simpleFormalParameterList)
    for (listParameter in parameterList.listFormalParameterList) {
        parameters.addAll(getParameters(listParameter))
    }
    for (recordParameter in parameterList.recordFormalParameterList) {
        parameters.addAll(getParameters(recordParameter))
    }
    return parameters
}

fun getParameterComponentList(handler: AppleScriptHandlerLabeledParametersDefinition): List<AppleScriptComponent> {
    val parameterList = handler.labeledParameterDeclarationList
    val givenProperties: List<AppleScriptObjectTargetPropertyDeclaration> =
        handler.objectTargetPropertyDeclarationList
    return buildList {
        parameterList.directParameterDeclaration?.let(::add)
        parameterList.targetListLiteral?.let { addAll(getTargets(it)) }
        parameterList.targetVariable?.let(::add)
        parameterList.targetRecordLiteral?.let { addAll(getTargets(it)) }
        addAll(parameterList.labeledParameterDeclarationPartList)
        addAll(givenProperties)
    }
}

fun getComponentList(parametersDeclaration: AppleScriptLabeledParameterDeclarationList): List<AppleScriptComponent> {
    val directParameter: AppleScriptDirectParameterDeclaration? =
        parametersDeclaration.directParameterDeclaration
    val labeledParameters: List<AppleScriptLabeledParameterDeclarationPart> =
        parametersDeclaration.labeledParameterDeclarationPartList
    return buildList {
        directParameter?.let(::add)
        parametersDeclaration.targetListLiteral?.let { addAll(getTargets(it)) }
        parametersDeclaration.targetVariable?.let(::add)
        parametersDeclaration.targetRecordLiteral?.let { addAll(getTargets(it)) }
        addAll(labeledParameters)
    }
}

private fun addRecordFormalParameterRecursive(
    recordParameter: AppleScriptRecordFormalParameter,
    parameters: MutableList<AppleScriptSimpleFormalParameter>,
) {
    for (property in recordParameter.objectNamedPropertyDeclarationList) {
        addFormalParametersFromProperty(property, parameters)
    }
}

private fun addFormalParametersFromProperty(
    property: AppleScriptObjectNamedPropertyDeclaration,
    parameters: MutableList<AppleScriptSimpleFormalParameter>,
) {
    property.simpleFormalParameter?.let(parameters::add)
    property.listFormalParameter?.let { parameters.addAll(it.simpleFormalParameterList) }
    property.recordFormalParameter?.let { addRecordFormalParameterRecursive(it, parameters) }
}
