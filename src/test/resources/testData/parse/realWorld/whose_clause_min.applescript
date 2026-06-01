(*
    PARSE-02 minimal fixture: a compound-boolean `whose` filter with doubled parentheses.
    Reuses the existing filterReference rule (CD-01). The subject is a cold-parsing object
    reference so this isolates the whose-clause capability from dictionary-dependent
    every-element reference forms (`every X of Y`, tracked separately).
*)
tell application "Music"
    set matches to (library playlist 1 whose ((rating > 80) or (genre is "Jazz")))
    return matches
end tell
