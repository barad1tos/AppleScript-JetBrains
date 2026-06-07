-- considering / ignoring statements, including the `application responses` attribute
-- (the dominant root-trigger class in the differential corpus scan).
ignoring application responses
	tell application "Finder" to activate
end ignoring

considering application responses
	set x to 1
end considering

considering case and diacriticals but ignoring white space
	set matches to "a" is "A"
end considering

ignoring white space and punctuation
	set y to 2
end ignoring
