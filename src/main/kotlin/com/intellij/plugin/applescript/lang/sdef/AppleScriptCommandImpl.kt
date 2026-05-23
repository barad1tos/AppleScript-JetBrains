package com.intellij.plugin.applescript.lang.sdef

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.xml.XmlTag

/**
 * Hybrid PSI + immutable value type pattern (D-01 keystone of v1.1).
 *
 *  - The class extends `AbstractDictionaryComponent` → `DictionaryComponentBase` →
 *    `FakePsiElement` so it remains a real PSI node (PsiManager caches,
 *    `areElementsEquivalent`, Find Usages, rename refactoring all keep working).
 *  - The class holds `private var data: CommandData` (immutable value type).
 *    Every public accessor reads from `data`.
 *  - Why not `data class : FakePsiElement`? PITFALLS §1.1 BLOCKER: Kotlin
 *    synthesises `equals`/`hashCode` from primary-constructor properties,
 *    overriding the platform's PSI identity contract and breaking caches /
 *    `PsiManager.areElementsEquivalent` / completion deduplication.
 *  - Why not pure-split (PSI separate from value)? Would require an equivalence
 *    audit on every consumer of `dictionary.getAllCommands().contains(cmd)`-style
 *    code (resolver, completion, annotator) — too broad for v1.1. Hybrid keeps
 *    the PSI surface stable; the structural-equality contract that closes the
 *    TODO at `ApplicationDictionaryImpl:177` lives in `CommandData`.
 *  - Why `var` not `val`? The parser's two-pass SDEF walk calls
 *    `setParameters` / `setResult` / `setDirectParameter` AFTER constructing
 *    the impl (D-06 façade). Each setter routes through `AppleScriptCommandBuilder`
 *    to produce a fresh frozen `CommandData`, then swaps the `data` reference.
 *    The impl is never observable in a half-built state from outside the parser,
 *    and the field becomes effectively-final once the parser hands off to the
 *    dictionary registry. v1.3 service split makes this a `val` by routing the
 *    builder publicly from `SDEF_Parser`.
 *  - v1.4 converts the public getters to `val` properties + `@get:JvmName` —
 *    NOT touched in v1.1 (D-03 strict v1.1/v1.4 boundary).
 *
 * Public constructor surface is preserved verbatim — `SDEF_Parser.parseCommandTag`
 * (line 356) continues to compile unchanged.
 */
open class AppleScriptCommandImpl :
    AbstractDictionaryComponent<Suite>,
    AppleScriptCommand {

    private var data: CommandData

    @Suppress("MemberVisibilityCanBePrivate")
    var cocoaClass: String? = null

    constructor(
        suite: Suite,
        name: String,
        code: String,
        parameters: List<CommandParameter>?,
        directParameter: CommandDirectParameter?,
        result: CommandResult?,
        description: String?,
        xmlTagCommand: XmlTag,
    ) : super(suite, name, code, xmlTagCommand, description) {
        this.data = AppleScriptCommandBuilder(name = name, code = code)
            .description(description)
            .parameters(parameters.orEmpty().map { toParameterData(it) })
            .directParameter(directParameter)
            .result(result)
            .build()
    }

    constructor(
        suite: Suite,
        name: String,
        code: String,
        xmlTagCommand: XmlTag,
    ) : super(suite, name, code, xmlTagCommand) {
        this.data = AppleScriptCommandBuilder(name = name, code = code).build()
    }

    override fun getParameterByName(name: String): CommandParameter? {
        // Return the PSI-wrapping form so callers expecting CommandParameter
        // (a PSI interface) get the right element identity. Two PSI views
        // with the same backing data are PSI-distinct nodes by design
        // (FakePsiElement uses reference equality), which matches the
        // pre-Hybrid behaviour of returning the cached PSI-instance via
        // the parametersMap lookup.
        val pData = data.parameters.firstOrNull { it.name == name } ?: return null
        return CommandParameterImpl(this, pData, myXmlElement)
    }

    override fun getParameterNames(): List<String> = data.parameters.map { it.name }

    override fun getParameters(): List<CommandParameter> =
        data.parameters.map { CommandParameterImpl(this, it, myXmlElement) }

    override fun getDirectParameter(): CommandDirectParameter? = data.directParameter

    override fun getResult(): CommandResult? = data.result

    override fun setResult(result: CommandResult?): CommandResult? {
        data = data.copy(result = result)
        return result
    }

    override fun getMandatoryParameters(): List<CommandParameter> =
        // Workaround for "in" / "of" being detected as object references rather than parameters.
        data.parameters
            .filter { !it.optional && it.name != "in" && it.name != "of" }
            .map { CommandParameterImpl(this, it, myXmlElement) }

    override fun setParameters(parameters: List<CommandParameter>) {
        data = AppleScriptCommandBuilder(name = data.name, code = data.code)
            .description(data.description)
            .parameters(parameters.map { toParameterData(it) })
            .directParameter(data.directParameter)
            .result(data.result)
            .build()
    }

    override fun setDirectParameter(directParameter: CommandDirectParameter?) {
        data = data.copy(directParameter = directParameter)
    }

    override fun getCocoaClassName(): String? = cocoaClass

    override fun getDocFooter(): String {
        val sb = StringBuilder()
        val indent = "&nbsp;&nbsp;&nbsp;&nbsp;"
        val p = getDirectParameter()
        val params = getParameters()
        if (p != null || params.isNotEmpty()) {
            sb.append("<p><b>Parameters:</b></p>")
        }
        if (p != null) {
            // CommandDirectParameter is a `data class` (02-03) — read via property
            // syntax to avoid the Kotlin platform-declaration clash that explicit
            // `fun getX()` forwarders would create (the data class already
            // synthesises the JVM accessors for Java callers).
            sb.append(indent).append(indent).append(p.typeSpecifier).append(" : ")
                .append(StringUtil.notNullize(p.description)).append("<br>")
        }
        for (par in params) {
            val (op, cl) = if (par.isOptional()) "[" to "]" else "" to ""
            val pType = StringUtil.notNullize(par.getTypeSpecifier())
            sb.append(indent).append(indent).append(op).append("<b>").append(par.getName()).append("</b> ")
                .append(pType).append(cl).append(" : ").append(par.getDescription()).append("<br>")
        }
        val res = getResult()
        if (res != null) {
            // CommandResult is a `data class` (02-03) — same reasoning as
            // CommandDirectParameter above: property syntax for Kotlin readers.
            sb.append("<p>").append("<b>Returns:</b></p>").append(indent).append(indent)
                .append(res.type).append(" : ").append(StringUtil.notNullize(res.description))
        }
        return sb.toString()
    }

    override fun getSuite(): Suite = myParent

    /**
     * Map any `CommandParameter` PSI element to its `CommandParameterData`
     * backing. The parser produces `CommandParameterImpl` instances exclusively,
     * so the cast is safe; if some future caller passes a stub / mock impl,
     * we fall back to a reconstructed `CommandParameterData` from the public
     * interface accessors (lossy on extra fields but correct for the four
     * declared interface methods).
     */
    private fun toParameterData(p: CommandParameter): CommandParameterData =
        if (p is CommandParameterImpl) p.data
        else CommandParameterData(
            name = p.getName(),
            code = p.getCode() ?: "",
            type = p.getTypeSpecifier(),
            optional = p.isOptional(),
            description = p.getDescription(),
        )
}
