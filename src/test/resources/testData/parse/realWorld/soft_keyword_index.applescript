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

set picked to {}
repeat with index from 1 to trackCount
    set end of picked to item index of statusList
end repeat

set rec to {index: 5, name: "x"}
set recIndex to index of rec

global index

tell application "Music"
    set index to 4
    set frontIndex to index of front window
end tell

return {collected, picked, recIndex}

on scope_demo()
    local index
    set index to 2
    return index
end scope_demo

on move_by given index: startValue
    return startValue
end move_by
