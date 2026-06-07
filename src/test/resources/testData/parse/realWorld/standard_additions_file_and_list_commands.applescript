on appendLine(logFilePath, lineText)
    try
        set logAlias to POSIX file logFilePath
        set fileHandle to open for access logAlias with write permission
        write (lineText & linefeed) to fileHandle starting at eof
        close access fileHandle
    on error errorMessage
        display dialog "Failed writing log: " & errorMessage buttons {"OK"} default button "OK"
    end try
end appendLine

on chooseMatchMode()
    set choices to {"Path+Size (fast, default)", "MD5 hash (slow, exact file content)"}
    set selectedChoice to choose from list choices with prompt "Choose how to detect duplicates:" default items {"Path+Size (fast, default)"} without multiple selections allowed
    if selectedChoice is false then error number -128
    return item 1 of selectedChoice
end chooseMatchMode
