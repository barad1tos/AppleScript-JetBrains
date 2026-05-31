(*
    Standard Suite text manipulation: text item delimiters, ASCII number,
    ASCII character, and current date used as a primary expression.
*)
set AppleScript's text item delimiters to ","
set theFields to text items of "alpha,beta,gamma"
set AppleScript's text item delimiters to {return}
set joined to theFields as text

set tabChar to ASCII character 9
set codePoint to ASCII number "A"
set stamp to current date
set yearNow to year of stamp

repeat with aField in theFields
    if (length of aField) > 0 then
        set firstChar to character 1 of aField
        log firstChar & tabChar & codePoint
    end if
end repeat
return joined
