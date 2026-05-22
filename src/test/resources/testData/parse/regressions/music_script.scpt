tell application "Music"
    -- validate arguments
    if (count of argv) ≠ 3 then
        return "Error: incorrect number of arguments. Usage: TrackID PropertyName PropertyValue"
    end if

    -- Parse arguments with validation
    set tid to item 1 of argv

    -- Verify trackID is a valid number
    if tid is not missing value and tid is not "" then
        try
            set tidNum to tid as integer
            on error
                return "Error: Invalid track ID '" & tid & "'. Must be a number."
        end try
    else
        return "Error: Missing track ID"
    end if

    set rawPropName to item 2 of argv
    set propInSymName to do shell script "/usr/bin/python3 -c 'import sys;print(sys.argv[1].strip().replace(\"'\", \"\"))' " & quoted form of rawPropName
    if propInSymName is "" or propInSymName is missing value then
        return "Error: Invalid property name"
    end if

    set normalizedPropName to do shell script "/usr/bin/python3 -c \"album_artist='album_artist', album='album', artist='artist', ...\""

    set problemIdentifier to normalizedPropName

    set rawPropValue to item 3 of argv
    set propInSymName to do shell script "/usr/bin/python3 -c 'import sys;print(sys.argv[1].strip())' " & quoted form of rawPropValue
    if propInSymName is "" or propInSymName is missing value then
        return "Error: Invalid property value"
    end if

    tell application "Music"
        try
            set tracksAttr to (first track of library playlist 1 whose id is tidNum)
            set tracksList to true
        on error
            set tracksList to false
        end try

        if tracksList then
            set currentValue to ""
            if problemIdentifier is "album" then
                set currentValue to album of tracksEt
            else if problemIdentifier is "album_artist" then
                set currentValue to album artist of tracksEt
            else if problemIdentifier is "artist" then
                set currentValue to artist of tracksEt
            else if problemIdentifier is "genre" then
                set currentValue to genre of tracksEt
            else if problemIdentifier is "year" then
                set currentValue to year of tracksEt as string
            end if

            -- Check if value is actually different
            if currentValue is equal to propValue then
                return "No Change: track " & tid & " '" & propInSymName & "' already set to " & propValue
            end if
        end if
    end tell
end tell
