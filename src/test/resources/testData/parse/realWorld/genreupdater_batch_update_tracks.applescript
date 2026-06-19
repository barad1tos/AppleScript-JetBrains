(*
    GenreUpdater batch update slice: validates nested try/on-error blocks, else-if
    chains, and Music.app multi-word property assignments without requiring a loaded
    application dictionary.
*)
on run argv
    if (count of argv) is 0 then
        return "Error: No update string provided."
    end if

    set updateString to item 1 of argv
    set fieldSeparator to ASCII character 30
    set commandSeparator to ASCII character 29
    set currentYear to year of (current date)
    set maxValidYear to currentYear + 2
    set oldDelimiters to AppleScript's text item delimiters

    try
        set AppleScript's text item delimiters to commandSeparator
        set commandList to text items of updateString

        tell application "Music"
            repeat with aCommand in commandList
                if aCommand is not "" then
                    set AppleScript's text item delimiters to fieldSeparator
                    set commandParts to text items of aCommand
                    set trackID to item 1 of commandParts
                    set propName to item 2 of commandParts
                    set propValue to item 3 of commandParts

                    try
                        set trackRef to (first track of library playlist 1 whose id is trackID)

                        if propName is "genre" then
                            set genre of trackRef to propValue
                        else if propName is "year" then
                            set propValueInt to propValue as integer
                            if propValueInt < 1900 or propValueInt > maxValidYear then
                                log "Year out of range for track " & trackID
                            else
                                set year of trackRef to propValueInt
                            end if
                        else if propName is "name" then
                            set name of trackRef to propValue
                        else if propName is "album" then
                            set album of trackRef to propValue
                        else if propName is "artist" then
                            set artist of trackRef to propValue
                        else if propName is "album_artist" then
                            set album artist of trackRef to propValue
                        end if
                    on error errMsg number errNum
                        log "Error updating track ID " & trackID & ": " & errMsg & " (" & errNum & ")"
                    end try
                end if
            end repeat
        end tell

        set AppleScript's text item delimiters to oldDelimiters
        return "Success: Batch update process completed."
    on error errMsg
        set AppleScript's text item delimiters to oldDelimiters
        return "Error: " & errMsg
    end try
end run
