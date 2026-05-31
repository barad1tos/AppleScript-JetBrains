(*
    on run argv handler body containing a nested tell APP "name" ... end tell
    block — the composite-fallback keystone case from D-08.
*)
on run argv
    if (count of argv) < 1 then
        return "usage: trackId"
    end if
    set tid to item 1 of argv

    tell application "Music"
        set theTrack to (first track of library playlist 1 whose id is (tid as integer))
        set theName to name of theTrack
        tell application "System Events"
            set isRunning to (exists process "Music")
        end tell
        return theName & " running=" & isRunning
    end tell
end run
