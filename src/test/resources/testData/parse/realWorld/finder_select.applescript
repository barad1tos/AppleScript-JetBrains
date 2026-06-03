(*
    Finder selection and desktop object references with a whose-filter
    over the contents of a folder. Uses alias-style path coercion.
*)
tell application "Finder"
    set theDesktop to desktop
    set chosen to selection
    if (count of chosen) is 0 then
        set chosen to (every file of home whose name extension is "txt")
    end if
    set targetFolder to folder "Reports" of documents folder
    repeat with anItem in chosen
        set name of anItem to (name of anItem as text)
    end repeat
    return chosen
end tell
