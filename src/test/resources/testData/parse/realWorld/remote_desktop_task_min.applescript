(*
    Remote Desktop task references: generic application-object terms can be multi-word
    dictionary-style nouns even when the application dictionary is not loaded.
*)
set theseComputers to computer list "Classroom"
set lockTask to make new lock screen task with properties {name:"Lock Classroom", message:"Please wait"}
execute lockTask on theseComputers
set cleanTask to make new send unix command task with properties {name:"Clean Desktop", showing output:false, script:"date"}
execute cleanTask on theseComputers
execute (make new empty trash task) on theseComputers
execute (make new unlock screen task) on theseComputers
