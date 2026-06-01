on formatDate(theDate)
    try
        if class of theDate is date then
            set y to year of theDate
            set mInt to (month of theDate as integer)
            set dInt to day of theDate
            set hhInt to hours of theDate
            set mmInt to minutes of theDate
            set ssInt to seconds of theDate

            return (y as string) & "-" & mInt & "-" & dInt & " " & hhInt & ":" & mmInt & ":" & ssInt
        else
            return ""
        end if
    on error
        return ""
    end try
end formatDate
