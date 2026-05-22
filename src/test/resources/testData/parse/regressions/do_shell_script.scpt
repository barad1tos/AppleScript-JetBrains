tell application "Music"
  set rawProp to "abc"
  set y to do shell script "/usr/bin/python3 -c 'print(1)'" & quoted form of rawProp
end tell
