(*
    GenreUpdater single-property update slice: validates guarded argument handling,
    nested Music.app tell blocks, else-if chains, multi-word property reads/writes,
    and nested try/on-error blocks.
*)
on trimText(theText)
    if theText is "" or theText is missing value then return ""
    return theText as text
end trimText

on normalizePropertyName(propName)
    if propName is "" or propName is missing value then return ""
    return my trimText(propName)
end normalizePropertyName

on run argv
    try
        if (count of argv) < 3 then
            return "Error: Not enough arguments."
        end if

        set trackID to item 1 of argv
        if trackID is not missing value and trackID is not "" then
            try
                set trackIDNumber to (trackID as integer)
            on error
                return "Error: Invalid track ID."
            end try
        else
            return "Error: Missing track ID"
        end if

        set rawPropName to item 2 of argv
        set propDisplayName to my trimText(rawPropName)
        if propDisplayName is "" then
            return "Error: Missing property name"
        end if
        set propIdentifier to my normalizePropertyName(propDisplayName)

        set rawPropValue to item 3 of argv
        set propValue to my trimText(rawPropValue)
        if propValue is "" then
            return "Error: Empty property value"
        end if

        set currentYear to year of (current date)
        set maxValidYear to currentYear + 2

        tell application "Music"
            try
                set trackExists to false
                set trackRef to (first track of library playlist 1 whose id is trackIDNumber)
                set trackExists to true
            on error errMsg
                return "Error: Track " & trackID & " not found: " & errMsg
            end try

            if trackExists then
                set currentValue to ""

                if propIdentifier is "name" then
                    set currentValue to name of trackRef
                    if currentValue is not equal to propValue then
                        set name of trackRef to propValue
                    end if
                else if propIdentifier is "album" then
                    set currentValue to album of trackRef
                    if currentValue is not equal to propValue then
                        set album of trackRef to propValue
                    end if
                else if propIdentifier is "artist" then
                    set currentValue to artist of trackRef
                    if currentValue is not equal to propValue then
                        set artist of trackRef to propValue
                    end if
                else if propIdentifier is "album_artist" then
                    set currentValue to album artist of trackRef
                    if currentValue is not equal to propValue then
                        set album artist of trackRef to propValue
                    end if
                else if propIdentifier is "genre" then
                    set currentValue to genre of trackRef
                    if currentValue is not equal to propValue then
                        set genre of trackRef to propValue
                    end if
                else if propIdentifier is "year" then
                    set currentValue to (year of trackRef) as string
                    if currentValue is not equal to propValue then
                        try
                            set propValueInt to propValue as integer
                            if propValueInt < 1900 or propValueInt > maxValidYear then
                                return "Error: Year value out of range"
                            end if
                            set year of trackRef to propValueInt
                        on error yearErr
                            return "Error: Failed to set year: " & yearErr
                        end try
                    end if
                end if

                if currentValue is equal to propValue then
                    return "No Change: Track already set"
                end if
                return "Success: Updated track"
            end if
        end tell
    on error errMsg
        return "Error: " & errMsg
    end try
end run
