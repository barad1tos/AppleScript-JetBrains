(*
    PARSE: Dialog Toolkit Plus unrecognized command heads in command-legal positions, parsed
    dictionary-independently (the osax is NOT installed/loaded). Each head (`max`, `create`,
    `display enhanced alert`) is an unrecognized application/osax command that today is eaten as a
    plain reference / application object reference, dangling the command tail.

    Exercises the keyword-noun tails the generic head must consume: `width for labels`
    (FOR keyword), `placeholder text` (TEXT keyword), `without suppression` (WITHOUT preposition),
    `giving up after` (the existing bare-label special-case). Variables are intentionally
    undeclared — only the command-head + tail shape is under test, not name resolution.

    Regression coverage for permissive unknown-command heads and keyword-noun parameter labels.
*)
set maxLabelWidth to max width for labels theLabelStrings
create rule (theTop + 12) rule width 400
create side labeled field (theLeft) placeholder text "x" left inset 8 total width 400 field left 100
display enhanced alert "Title" message "Body" buttons {"Cancel"} giving up after 5 without suppression
