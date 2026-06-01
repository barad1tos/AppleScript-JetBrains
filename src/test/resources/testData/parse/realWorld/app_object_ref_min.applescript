(*
    PARSE-03 minimal fixture: generic application-object references — library playlist N,
    current track, and track N of library playlist M. Top-level (no tell block, no app
    dictionary) so the D-07 cold-cache assertion holds — these parse syntactically, they are
    not dictionary-resolved. The single-bareword `folder 1` index case is intentionally NOT
    here; it is deferred to 08-07 (whose-clause owner) per the 08-06 checkpoint verdict.
*)
set p to library playlist 1
set t to current track
set x to track 2 of library playlist 1
