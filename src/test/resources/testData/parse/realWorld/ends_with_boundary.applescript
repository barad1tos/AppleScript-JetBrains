(*
    Word-boundary regression for the ENDS_WITH lexer macro: a handler named with_*
    closes as `end with_local`, which must lex as END + identifier, while both
    comparison operator spellings (`ends with`, `end with`) keep working.
    osacompile-validated.
*)
on with_local()
    return 1
end with_local

set fullMatch to "abc" ends with "c"
set aliasMatch to "abc" end with "c"
return {with_local(), fullMatch, aliasMatch}
