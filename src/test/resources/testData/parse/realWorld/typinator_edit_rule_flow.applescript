(*
    PARSE: Typinator edit-rule flow with Dialog Toolkit command output, nested
    `if ... then` / `tell application` / `try` blocks, direct `first rule set whose ...`,
    and single-line tell commands after an `else if`.

    This is intentionally dictionary-independent: Typinator and Dialog Toolkit Plus do not
    need to be installed for the parser to keep the block structure intact.
*)
tell application "Typinator"
    set containerid to unique id of containing set
    set uid to unique id
end tell

set {buttonName, suppressedState, controlResults} to display enhanced alert "Edit Last Typinator Rule" message "Use this window to edit directly, or click Show to see the snippet in Typinator" as informational alert buttons {"Cancel", "Show", "OK"} giving up after 120 acc view width 400 acc view height theTop acc view controls allControls without suppression
set {unused, descrip, unused, snip, unused, abbreve} to controlResults

if buttonName = "OK" then
    tell application "Typinator"
        try
            set c to first rule set whose unique id is containerid
            tell c
                set r to first rule whose unique id is uid

                tell r
                    set abbreviation to abbreve
                    set description to descrip
                    set plain expansion to snip
                end tell
            end tell

        on error errMsg number errNum
            if errNum = -1719 then
                display notification "No unique id found" with title "Edit Snippet" subtitle "Error:" sound name "Basso"
            end if
        end try
    end tell
else if buttonName = "Show" then
    tell application "Typinator" to quick search "last"
    delay 0.3
    tell application "System Events" to keystroke return using {command down}
end if
