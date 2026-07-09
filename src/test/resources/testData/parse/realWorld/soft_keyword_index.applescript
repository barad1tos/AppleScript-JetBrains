(*
    `index` is not an AppleScript reserved word: osacompile accepts it as a range/list
    loop variable, a positional handler parameter, an assignment target, and an
    expression operand. Regression fixture for the soft-keyword identifier seam
    (softKeywordIdentifier in AppleScript.bnf); shapes mirror the motivating
    GenreUpdater batch script that produced 6 false parse errors.
*)
on item_or_missing(values, position)
    if (count of values) >= position then return item position of values
    return missing value
end item_or_missing

on describe(index)
    return index as text
end describe

set index to 5
set statusList to {"local only", "purchased", "matched"}
set trackCount to count of statusList

set collected to {}
repeat with index from 1 to trackCount
    set end of collected to my item_or_missing(statusList, index)
end repeat

repeat with index in statusList
    set end of collected to my describe(index)
end repeat

return collected
