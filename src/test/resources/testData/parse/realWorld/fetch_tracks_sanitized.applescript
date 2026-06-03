(*
    Sanitized fetch-tracks corpus slice: preserves the Music object/property
    specifier shapes from the local smoke script without private paths, IDs, or
    personal library data. Parse-only fixture; it must never execute Music.
*)
on run argv
    if (count of argv) > 0 then
        set selectedArtist to item 1 of argv
    else
        set selectedArtist to ""
    end if

    set batchOffset to 1
    set batchLimit to 25
    set minDateAdded to missing value
    set fieldSeparator to ASCII character 30
    set finalResult to {}

    tell application "Music"
        if selectedArtist is not "" then
            if minDateAdded is not missing value then
                set trackRef to a reference to (every track of library playlist 1 whose ((artist is selectedArtist) or (album artist is selectedArtist)) and (date added > minDateAdded))
            else
                set trackRef to a reference to (every track of library playlist 1 whose (artist is selectedArtist) or (album artist is selectedArtist))
            end if
        else if batchLimit > 0 then
            set totalTracks to (count of tracks of library playlist 1)
            set endIndex to batchOffset + batchLimit - 1
            if endIndex > totalTracks then
                set endIndex to totalTracks
            end if
            set trackRef to a reference to (tracks batchOffset thru endIndex of library playlist 1)
        else
            set trackRef to a reference to (every track of library playlist 1 whose date added > minDateAdded)
        end if

        set trackCount to count of trackRef
        set idList to id of trackRef
        set nameList to name of trackRef
        set artistList to artist of trackRef
        set albumArtistList to album artist of trackRef
        set dateAddedList to date added of trackRef
        set statusList to cloud status of trackRef

        repeat with trackIndex from 1 to trackCount
            set trackId to item trackIndex of idList
            set trackName to item trackIndex of nameList
            set trackArtist to item trackIndex of artistList
            set albumArtist to item trackIndex of albumArtistList
            set dateAdded to item trackIndex of dateAddedList
            set trackStatus to item trackIndex of statusList
            set end of finalResult to {trackId, trackName, trackArtist, albumArtist, dateAdded, trackStatus}
        end repeat
    end tell

    set oldDelimiters to AppleScript's text item delimiters
    set AppleScript's text item delimiters to fieldSeparator
    set resultString to finalResult as text
    set AppleScript's text item delimiters to oldDelimiters
    return resultString
end run
