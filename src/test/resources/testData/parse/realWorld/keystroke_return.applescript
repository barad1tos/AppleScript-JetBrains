(*
    PARSE: the `return` constant inside a command tail must not collide with the RETURN keyword.
    `keystroke return using {command down}` — `return` here is the value-position constant, not a
    return statement. The Step-1 concern is the `return`-constant-in-tail half (BUCKETA-02). NOTE:
    `keystroke` is a generic (unrecognized) command head — if that generic-head dependency
    dominates the RED dump, this fixture's GREEN folds into Plan 10-03 (the permissive generic
    head) and stays RED-skipped here. The executor decides from the actual Task-1 RED dump.
*)
tell application "System Events" to keystroke return using {command down}
