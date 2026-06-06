(*
    PARSE: record/property labels can be multi-word dictionary-style nouns AND keyword nouns
    that are not plain identifiers (`count`, `class`, `id`). The label position before `:` is
    unambiguous, so all of these must parse without a loaded dictionary.
*)
set tableProps to {column count:5, row count:3, name:"Sheet"}
set moreProps to {count:10, class:"box", id:"x7"}
