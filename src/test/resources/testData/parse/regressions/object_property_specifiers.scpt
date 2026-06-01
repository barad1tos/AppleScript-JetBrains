(*
    Phase 09 broad object/property specifier fixture. Parse-only: these forms
    must be accepted syntactically without loading application dictionaries.
*)
tell application "Mail"
    set inbox to mailbox "INBOX" of account "Work"
end tell

tell application "Calendar"
    set homeCalendar to calendar "Home"
    set upcoming to (every event of homeCalendar whose start date ≥ current date)
    set eventStart to start date of item 1 of upcoming
end tell

tell application "Safari"
    set theWindow to front window
    set matchingTabs to (every tab of theWindow whose URL contains "example.com")
    set current tab of theWindow to tab 1 of theWindow
end tell

tell application "Finder"
    set reportsFolder to folder "Reports" of documents folder
    set indexedFolder to (first item of folder 1 whose name is "x")
    set pngCount to count (every file of (folder "Pictures" of home) whose name extension is "png")
end tell

set prefsFolder to path to preferences folder from user domain
set fileContents to read file ((prefsFolder as text) & "settings.txt")
