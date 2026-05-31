(*
    Music library inspection: exercises whose-filter on a nested object
    reference (library playlist N), current track, and the non-ASCII
    not-equal operator in expression context.
*)
tell application "Music"
    set theArtist to "Miles Davis"
    set matches to (every track of library playlist 1 whose artist is theArtist)
    if (count of matches) ≠ 0 then
        set firstMatch to item 1 of matches
        set theName to name of firstMatch
        set theKind to ASCII character 9
        return theName & theKind & (artist of firstMatch)
    end if

    if player state is playing then
        set nowPlaying to current track
        return name of nowPlaying & " — " & artist of nowPlaying
    end if
    return "no match"
end tell
