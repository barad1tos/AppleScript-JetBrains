(*
    PARSE: unrecognized command heads inside tell bodies, parsed dictionary-independently.

    1. `quick search "snippet"` (Typinator, NOT installed) followed by `delay 0.3`: the cascade
       proof. Today `quick search` is eaten as an application object reference and the broken parse
       bleeds onto the next line so `delay 0.3` errors at the decimal `.`. Once `quick search`
       parses as a permissive command (terminated at NLS), `delay 0.3` parses clean (L79→L80
       cascade clears).
    2. `tell application "System Events" to keystroke return using {command down}`:
       `keystroke` is the generic head; `return` is consumed as a direct-parameter value (the
       value-position `return` constant), NOT a return statement.

    Regression coverage for unknown command heads in tell bodies and `return` as a command value.
*)
tell application "Typinator"
    quick search "snippet"
    delay 0.3
end tell
tell application "System Events" to keystroke return using {command down}
