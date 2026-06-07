(*
    PARSE (DEFERRED): the leading `first rule set whose ...` object reference, where `rule set`
    is a multi-word dictionary class name and there is NO preceding `of`/`in` operand.

    This is a different grammar path from the OF/IN-operand forms in typinator_rule_set_min.applescript:
    `first rule set` is reached via INDEX_REFERENCE as a direct RHS before the fallback dictionary-class
    operand path. The operand-position safety gate correctly does not fire because the token before
    `set` is `first`, not OF/IN. Fixing this needs a dedicated BNF object-reference operand change
    plus parser regeneration; widening the Kotlin operand gate would risk the `set` assignment guard.
*)
tell application "Typinator"
    set c to first rule set whose unique id is containerid
end tell
