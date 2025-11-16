#!/bin/bash

# This script regenerates all test data files (.txt files) based on current parser output
# by running the ParsingTestCase tests

# For IntelliJ ParsingTestCase, we can regenerate test data by:
# 1. Using the test runner with a special flag
# 2. Or manually extracting the actual output and updating the files

# First, let's check if there's a regeneration mechanism in IntelliJ's ParsingTestCase
# The typical approach is to set an environment variable or system property

export UPDATE_TEST_DATA=true

# Run the tests
./gradlew test --tests "*ParsingTestCase" -i -Ptest.mode=updateData 2>&1

