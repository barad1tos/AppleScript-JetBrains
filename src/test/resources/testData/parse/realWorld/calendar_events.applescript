(*
    Calendar: events whose start date is on or after the current date,
    exercising whose-filter combined with a date comparison and current date.
*)
tell application "Calendar"
    set today to current date
    set cal to calendar "Home"
    set upcoming to (every event of cal whose start date ≥ today)
    repeat with anEvent in upcoming
        set theSummary to summary of anEvent
        if status of anEvent is not cancelled then
            set theDuration to (end date of anEvent) - (start date of anEvent)
            log theSummary & ": " & (theDuration ÷ 60) & " minutes"
        end if
    end repeat
    return (count of upcoming)
end tell
