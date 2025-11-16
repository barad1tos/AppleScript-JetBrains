#!/usr/bin/env python3
import os
import glob

def normalize_file(filepath):
    """Normalize line endings to LF only"""
    try:
        with open(filepath, 'rb') as f:
            content = f.read()

        # Replace CRLF with LF, then any remaining CR with LF
        content = content.replace(b'\r\n', b'\n')
        content = content.replace(b'\r', b'\n')

        with open(filepath, 'wb') as f:
            f.write(content)
        return True
    except Exception as e:
        print(f"Error processing {filepath}: {e}")
        return False

# Process all test data files
test_data_dir = "src/test/resources/testData"
patterns = ["**/*.txt", "**/*.scpt"]

for pattern in patterns:
    files = glob.glob(os.path.join(test_data_dir, pattern), recursive=True)
    for file in files:
        if normalize_file(file):
            print(f"Normalized: {file}")
