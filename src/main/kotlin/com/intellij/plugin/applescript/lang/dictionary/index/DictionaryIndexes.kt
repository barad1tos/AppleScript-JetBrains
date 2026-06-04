package com.intellij.plugin.applescript.lang.dictionary.index

import com.intellij.plugin.applescript.lang.sdef.AppleScriptClass
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand
import com.intellij.plugin.applescript.lang.sdef.AppleScriptPropertyDefinition
import com.intellij.plugin.applescript.lang.sdef.DictionaryEnumeration
import com.intellij.plugin.applescript.lang.sdef.DictionaryEnumerator
import com.intellij.plugin.applescript.lang.sdef.DictionaryRecord
import com.intellij.plugin.applescript.psi.sdef.impl.ApplicationDictionaryImpl
import java.util.concurrent.ConcurrentHashMap

/**
 * Encapsulates the index-map cluster previously declared inline on
 * [ApplicationDictionaryImpl] (the 9-map block formerly at lines 70-93 plus the
 * secondary `dictionaryCommandListMap` added by Plan 02-04). Provides the
 * thread-safe storage layer that closes the `processInclude` race latent since
 * v1.0.0 — Phase 1 explicitly deferred this fix to v1.1 (Phase 1 CONTEXT D-01)
 * to avoid touching the same file twice.
 *
 * Design choices (Phase 02 CONTEXT D-07):
 *  - Regular `class`, NOT `@JvmInline value class`. STACK.md flags name
 *    mangling on public API surfaces; `RECURRING_PITFALLS.md` Pattern K
 *    (value-class swap holes) is the other side of the same concern. Keeping
 *    this as an ordinary holder type leaves no room for either failure mode.
 *  - Every map backed by [ConcurrentHashMap]. The canonical analog is
 *    `AppleScriptSystemDictionaryRegistryService` from v1.0.1
 *    (lines 57-80): same package convention, same `ConcurrentHashMap()` /
 *    `ConcurrentHashMap.newKeySet()` shape.
 *  - Field names preserved verbatim from the impl class to minimise the
 *    refactor diff at call sites — `dictionaryCommandMap` etc. stay; only the
 *    container moved.
 *
 * Race scenario closed (T-02-02): `ApplicationDictionaryImpl.processInclude:148`
 * re-enters `SdefParser.parseRootTag`, which mutates the maps via
 * [ApplicationDictionaryImpl.addCommand] / `addClass` / `addProperty` /
 * `addEnumeration`. Background threads (resolver, completion, annotator) call
 * `findCommand`, `findClass`, `findAllCommandsWithName`, and `getAllCommands`
 * concurrently. Raw `HashMap.put` under such contention is JLS-undefined
 * (observed: silent lost updates under resize collision; the regression test
 * `ApplicationDictionaryConcurrencyTest` deterministically surfaces 95-200 lost
 * puts per 4×1000 stress run on the pre-fix code). `ConcurrentHashMap` makes
 * every put durable and every iteration weakly-consistent.
 *
 * Future trajectory: v1.3 service decomposition (`SdefIndexService`) can move
 * the entire [DictionaryIndexes] instance behind a service boundary without
 * touching the impl's accessors — `private val indexes` becomes the
 * cleanly-extractable seam.
 *
 * @see ApplicationDictionaryImpl
 */
internal class DictionaryIndexes {
    val dictionaryPropertyMap: MutableMap<String, AppleScriptPropertyDefinition> = ConcurrentHashMap()

    val dictionaryRecordMap: MutableMap<String, DictionaryRecord> = ConcurrentHashMap()

    val dictionaryEnumeratorMap: MutableMap<String, DictionaryEnumerator> = ConcurrentHashMap()

    val dictionaryEnumerationMap: MutableMap<String, DictionaryEnumeration> = ConcurrentHashMap()

    val dictionaryCommandMap: MutableMap<String, AppleScriptCommand> = ConcurrentHashMap()

    /**
     * Secondary index keyed by command name → 0..N commands (D-02 closure from
     * Plan 02-04). The primary [dictionaryCommandMap] keeps its first-by-name
     * contract (D-03 interface freeze — `getDictionaryCommandMap` returns
     * `Map<String, AppleScriptCommand>`); this list-keyed companion lets
     * `findAllCommandsWithName` return the full overload set.
     *
     * The value-side list is [java.util.concurrent.CopyOnWriteArrayList] so
     * the read-mostly iteration paths (`findAllCommandsWithName` returns a
     * defensive `.toList()` copy) are lock-free and CME-free, while the rare
     * `addCommand` writes pay the copy-on-write cost — acceptable because
     * `addCommand` is a parser-init-time operation, not a hot path. Dedupe by
     * `CommandData` structural equality stays in `ApplicationDictionaryImpl.addCommand`.
     */
    val dictionaryCommandListMap: MutableMap<String, MutableList<AppleScriptCommand>> = ConcurrentHashMap()

    val dictionaryClassMap: MutableMap<String, AppleScriptClass> = ConcurrentHashMap()

    val dictionaryClassToPluralNameMap: MutableMap<String, AppleScriptClass> = ConcurrentHashMap()

    val dictionaryClassByCodeMap: MutableMap<String, AppleScriptClass> = ConcurrentHashMap()
}
