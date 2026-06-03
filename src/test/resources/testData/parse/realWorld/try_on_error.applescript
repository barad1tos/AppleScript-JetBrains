(*
    try ... on error errMsg number errNum ... end try blocks, including the
    full on-error signature with number and an explicit error -128 raise.
*)
on safeDivide(a, b)
    try
        if b is 0 then
            error "division by zero" number -2700
        end if
        return a / b
    on error errMsg number errNum
        log errMsg & " (" & errNum & ")"
        return missing value
    end try
end safeDivide

try
    set result to safeDivide(10, 0)
    if result is missing value then error number -128
on error number -128
    set result to 0
end try
return result
