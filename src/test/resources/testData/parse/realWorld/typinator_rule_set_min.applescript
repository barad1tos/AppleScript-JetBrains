(*
    PARSE: `set`-keyword multi-word dictionary class names in the object-reference `of`-operand
    position (`rule set`, `containing set`), parsed dictionary-independently (Typinator is NOT
    installed/loaded). The `rule set` / `containing set` operand is recognized as a
    DICTIONARY_CLASS_IDENTIFIER_PLURAL rather than choking on the `set` keyword token. This is the
    This fixture covers the OF/IN-operand family: the `set` assignment statement at line start must
    stay intact, and `set` may be consumed only as a non-first reference/class word after OF/IN.

    The leading `first rule set whose ...` form has no preceding OF/IN operand, follows a different
    INDEX_REFERENCE / USER_CLASS_NAME path, and lives in typinator_rule_set_nofof.applescript as
    deferred parser debt.
*)
tell application "Typinator"
    set lastrule to first rule of rule set theSet whose abbreviation is lastexp
    set containerid to unique id of containing set
end tell
