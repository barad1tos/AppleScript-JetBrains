package com.intellij.plugin.applescript.lang.sdef.parser

import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommandImpl
import com.intellij.plugin.applescript.lang.sdef.CommandDirectParameter
import com.intellij.plugin.applescript.lang.sdef.CommandParameter
import com.intellij.plugin.applescript.lang.sdef.CommandParameterData
import com.intellij.plugin.applescript.lang.sdef.CommandParameterImpl
import com.intellij.plugin.applescript.lang.sdef.CommandResult
import com.intellij.plugin.applescript.lang.sdef.Suite
import com.intellij.psi.xml.XmlTag

internal object SdefCommandTagParser {
    fun parse(
        commandTag: XmlTag,
        suite: Suite,
    ): AppleScriptCommand? {
        val identity = SdefTagReader.readRequiredIdentity(commandTag) ?: return null
        val command: AppleScriptCommand = AppleScriptCommandImpl(suite, identity.name, identity.code, commandTag)
        command.description = commandTag.getAttributeValue(ATTRIBUTE_DESCRIPTION)
        command.setDictionaryDoc(commandTag.getSubTagText(TAG_DOCUMENTATION))
        commandTag.findFirstSubTag(TAG_RESULT)?.let(::parseResult)?.let(command::setResult)
        command.directParameter = parseDirectParameter(command, commandTag.findFirstSubTag(TAG_DIRECT_PARAMETER))
        command.parameters = parseParameters(command, commandTag.findSubTags(TAG_PARAMETER))
        return command
    }

    private fun parseResult(resultTag: XmlTag): CommandResult? {
        val resultType = resultTag.getAttributeValue(ATTRIBUTE_TYPE)
        return resultType?.let { CommandResult(it, resultTag.getAttributeValue(ATTRIBUTE_DESCRIPTION)) }
    }

    private fun parseDirectParameter(
        command: AppleScriptCommand,
        directParam: XmlTag?,
    ): CommandDirectParameter? =
        directParam?.let { tag ->
            SdefTagReader.readDirectParameterType(tag)?.let { type ->
                CommandDirectParameter(
                    command,
                    type,
                    tag.getAttributeValue(ATTRIBUTE_DESCRIPTION),
                    YES_VALUE == tag.getAttributeValue(ATTRIBUTE_OPTIONAL),
                )
            }
        }

    private fun parseParameters(
        command: AppleScriptCommand,
        parameterTags: Array<XmlTag>,
    ): List<CommandParameter> =
        parameterTags.mapNotNullTo(ArrayList(parameterTags.size)) { paramTag ->
            parseParameter(command, paramTag)
        }

    private fun parseParameter(
        command: AppleScriptCommand,
        paramTag: XmlTag,
    ): CommandParameter? {
        val identity = SdefTagReader.readRequiredIdentity(paramTag)
        val parameterType = SdefTagReader.readParameterType(paramTag)
        return if (identity != null && parameterType != null) {
            CommandParameterImpl(
                command,
                CommandParameterData(
                    name = identity.name,
                    code = identity.code,
                    type = parameterType,
                    optional = YES_VALUE == paramTag.getAttributeValue(ATTRIBUTE_OPTIONAL),
                    description = paramTag.getAttributeValue(ATTRIBUTE_DESCRIPTION),
                ),
                paramTag,
            )
        } else {
            null
        }
    }
}
