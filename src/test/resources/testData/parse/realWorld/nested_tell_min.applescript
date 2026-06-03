(*
    PARSE-06 minimal fixture: a nested `tell application "name" ... end tell` block inside
    an `on run argv ... end run` handler body — must parse without an Incomplete-expression
    false positive.
*)
on run argv
    tell application "Music"
        play
    end tell
end run
