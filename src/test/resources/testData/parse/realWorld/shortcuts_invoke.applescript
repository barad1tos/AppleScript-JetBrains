(*
    Modern macOS automation: invoking a Shortcuts shortcut via its object
    reference and passing input, plus a property read on the result.
*)
tell application "Shortcuts Events"
    set theShortcut to shortcut "Resize Image"
    set theResult to run theShortcut with input "~/Pictures/photo.png"
    return theResult
end tell

tell application "Finder"
    set picCount to count (every file of (folder "Pictures" of home) whose name extension is "png")
end tell
return picCount
