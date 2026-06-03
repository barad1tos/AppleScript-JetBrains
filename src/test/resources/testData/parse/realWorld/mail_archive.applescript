(*
    Mail archival: mailbox object references, incoming message handler-style
    access, whose-filter over messages, and the >= non-ASCII operator.
*)
tell application "Mail"
    set inbox to mailbox "INBOX" of account "Work"
    set unreadCount to count (every message of inbox whose read status is false)
    if unreadCount ≥ 1 then
        set archive to mailbox "Archive" of account "Work"
        repeat with eachMessage in (messages of inbox whose date received comes before (current date))
            move eachMessage to archive
        end repeat
    end if
    return unreadCount
end tell
