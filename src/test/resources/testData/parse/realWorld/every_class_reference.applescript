(*
    PARSE: generic `every <class>` element references for ANY scriptable app, with no
    application dictionary loaded. The class noun after `every` is unambiguous, so a bare
    dictionary-style noun must parse in every following position: line end, before `to`,
    and before `of`, not only before a class anchor token.
*)
set groupNames to the name of every group
set somePeople to every person of group "Friends"
set the height of every row to 24
set allTables to every table
