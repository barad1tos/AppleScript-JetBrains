tell application id "com.apple.systemevents"
    set processExists to exists process 1
end tell

tell application id "com.apple.systemevents" to launch

using terms from application id "com.apple.systemevents"
    set processExists to exists process 1
end using terms from

if application id "com.apple.systemevents" is running then
    set applicationIsRunning to true
end if
