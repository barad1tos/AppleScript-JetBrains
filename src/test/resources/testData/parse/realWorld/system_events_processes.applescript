(*
    System Events process inspection: every ... whose visible is true,
    UI element traversal, and a property read on an app-object reference.
*)
tell application "System Events"
    set visibleApps to (every process whose visible is true and background only is false)
    set names to {}
    repeat with proc in visibleApps
        set end of names to name of proc
        if (count of windows of proc) > 0 then
            set frontWindow to window 1 of proc
            set position of frontWindow to {0, 0}
        end if
    end repeat
    return names
end tell
