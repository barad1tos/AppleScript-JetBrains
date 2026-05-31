(*
    Safari window/tab object references with a whose-filter over tabs and
    a compound boolean condition inside the filter parentheses.
*)
tell application "Safari"
    set theWindow to front window
    set keepers to (every tab of theWindow whose (URL contains "example.com" or name contains "Docs"))
    repeat with aTab in keepers
        set theURL to URL of aTab
        if theURL is not missing value then
            log theURL
        end if
    end repeat
    set current tab of theWindow to tab 1 of theWindow
    return (count of keepers)
end tell
