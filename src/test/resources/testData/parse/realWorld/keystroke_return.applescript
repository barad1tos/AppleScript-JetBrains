(*
    PARSE: the `return` constant inside a command tail must not collide with the RETURN keyword.
    `keystroke return using {command down}` uses `return` as a value-position text constant, not a
    return statement. `keystroke` is also an unknown command head, so this guards the permissive
    command-name fallback and its handling of `return` as a command value.
*)
tell application "System Events" to keystroke return using {command down}
