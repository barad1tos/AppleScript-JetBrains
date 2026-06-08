on run argv
    set minDateAdded to missing value

    tell application "Music"
        if minDateAdded<caret> is not missing value then
            set trackRef to a reference to (every track of library playlist 1 whose date added > minDateAdded)
        end if
    end tell
end run
