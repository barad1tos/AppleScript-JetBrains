(*
    StandardAdditions scripting additions: path to <constant>, do shell
    script, and a try/on-error guard around a file read.
*)
set appsFolder to path to applications folder
set prefsFolder to path to preferences folder from user domain
set homeFolder to path to home folder

set hostName to do shell script "/bin/hostname -s"

try
    set theData to read file ((prefsFolder as text) & "settings.txt")
on error errMsg number errNum
    set theData to ""
    log "read failed: " & errMsg & " (" & errNum & ")"
end try

return {appsFolder, homeFolder, hostName}
