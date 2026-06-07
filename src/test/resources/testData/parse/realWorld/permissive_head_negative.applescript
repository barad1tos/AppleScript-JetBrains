(*
    PARSE: NEGATIVE sentinel for the permissive generic command head (Pitfall 2). The generic
    head must engage ONLY at a command-entry position, never mid-expression. `max` here is a plain
    variable inside an arithmetic expression, not a command head; `controlLeft` likewise. Both lines
    are valid AppleScript and MUST parse with zero PsiErrorElement both before AND after the fix —
    this is the regression sentinel that proves the generic head does not shadow plain variables.

    This fixture is NOT debt-skipped: it passes today and must keep passing after the generic head
    lands. This is the negative guard for permissive unknown-command parsing.
*)
set y to x + max
set z to (controlLeft + 8)
