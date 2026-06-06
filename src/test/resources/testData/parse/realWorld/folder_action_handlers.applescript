-- AppleScript built-in Folder Action handler signatures (dictionary-independent).
-- The handler closes with the full signature repeated after `end`, as real scripts write it.
on adding folder items to this_folder after receiving these_items
	set itemCount to count of these_items
end adding folder items to this_folder after receiving these_items

on removing folder items from this_folder after losing those_items
	set x to 1
end removing folder items from this_folder after losing those_items

on opening folder this_folder
	set y to 1
end opening folder this_folder

on closing folder window for this_folder
	set z to 1
end closing folder window for this_folder

on moving folder window for this_folder from old_bounds
	set b to 1
end moving folder window for this_folder from old_bounds

-- bare `end` form is also valid
on opening folder another_folder
	set c to 1
end
