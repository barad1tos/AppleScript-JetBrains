(*
    Non-ASCII operators in arithmetic and comparison contexts:
    ÷ (divide), ≥, ≤, ≠ — the PARSE-05 character set, exercised together.
*)
set total to 100
set parts to 7
set average to total ÷ parts

set lower to 10
set upper to 90

repeat with n from lower to upper
    if n ≥ average and n ≤ upper then
        if n ≠ 42 then
            set total to total + n
        end if
    end if
end repeat

if (total ÷ 2) ≥ 50 then
    return "high"
else
    return "low"
end if
