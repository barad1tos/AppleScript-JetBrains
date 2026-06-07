(*
    PARSE: assigning date components inside a handler must stay dictionary-independent.
    `year`, `month`, `day`, and `time` are built-in date properties; the parser must not
    collapse after the first assignment while dictionary readiness is still cold.
*)
on date_from_unix_timestamp(timestampSeconds)
    try
        set epochDate to (current date)
        set year of epochDate to 1970
        set month of epochDate to January
        set day of epochDate to 1
        set time of epochDate to 0
        return epochDate + timestampSeconds
    on error errMsg
        log "date_from_unix_timestamp error: " & errMsg
        return missing value
    end try
end date_from_unix_timestamp
