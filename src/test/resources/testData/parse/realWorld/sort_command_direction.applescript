(*
    PARSER DEBT: structural fallback command parameters. `sort by <ref> direction <value>`
    uses the `by` preposition plus a bare `direction` label, which the current name-gated
    fallback command path does not parse. To be resolved by the structural command-gate refactor.
*)
tell application "Numbers"
sort by column 1 direction ascending
end tell
