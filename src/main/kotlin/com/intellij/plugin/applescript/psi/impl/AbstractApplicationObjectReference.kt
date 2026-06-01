package com.intellij.plugin.applescript.psi.impl

import com.intellij.lang.ASTNode

/**
 * PSI mixin base for the generic `application_object_reference` rule (PARSE-03, plan 08-06).
 *
 * Navigation hook only — it carries no resolver wiring (D-09). Turning these nodes into
 * resolvable references against a loaded application dictionary is a tracked follow-up; for
 * now the node exists so the parse tree is queryable and the construct stops being a
 * `PsiErrorElement`. Extends the same base (`AppleScriptPsiElementImpl`) the Grammar-Kit
 * generated `AppleScriptApplicationObjectReferenceImpl` expects via this mixin.
 */
open class AbstractApplicationObjectReference(node: ASTNode) : AppleScriptPsiElementImpl(node)
