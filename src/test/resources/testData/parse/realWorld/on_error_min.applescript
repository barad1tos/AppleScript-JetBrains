(*
    PARSE-06 minimal fixture: a try / on error block binding both errMsg and errNum —
    must parse without Incomplete-expression false positives (existing tryStatement rule).
*)
try
    set x to 1 / 0
on error errMsg number errNum
    log errMsg & " (" & errNum & ")"
end try
