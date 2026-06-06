(*
    PARSE: `set`-keyword multi-word dictionary class names in the object-reference `of`-operand
    position (`rule set`, `containing set`) and a leading `first rule set whose …` form, all
    parsed dictionary-independently (Typinator is NOT installed/loaded). The `rule set` /
    `containing set` operand must be recognized as a DICTIONARY_CLASS_NAME rather than choking on
    the `set` keyword token. This is the Step-1 (Bucket A) family — the `set` ASSIGNMENT statement
    at line start must stay intact; `set` is consumed only as a NON-first reference/class word.
*)
tell application "Typinator"
    set lastrule to first rule of rule set theSet whose abbreviation is lastexp
    set containerid to unique id of containing set
    set c to first rule set whose unique id is containerid
end tell
