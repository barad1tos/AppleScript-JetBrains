(*
    PARSE: `display notification` (Standard Additions) with a trailing `with <label> <value>` labeled
    parameter tail (`with title … subtitle … sound name …`), parsed dictionary-independently and in
    the motivating-script context: nested inside `tell application "Typinator"` (Typinator is NOT
    installed/loaded). The command head IS recognized via Standard Additions even inside the foreign
    tell; the labeled VALUE tail must not dangle as a PsiErrorElement.

    The second statement (`set done to true`) after the labeled command is the BOUNDARY PROOF for
    Pitfall 4: the labeled tail must stop at NLS and must NOT consume the following statement's leading
    token. This guards the command-tail boundary after a labeled Standard Additions command.
*)
tell application "Typinator"
    display notification "No unique id found" with title "Edit Snippet" subtitle "Error:" sound name "Basso"
    set done to true
end tell
