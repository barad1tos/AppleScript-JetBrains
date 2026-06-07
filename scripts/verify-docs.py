#!/usr/bin/env python3
"""Verify release docs metadata.

Thin entry point. All logic lives in the `verify_docs` package:

  paths              repo-rooted file path constants
  report             Finding / Report aggregation
  gradle_properties  gradle.properties parser
  changelog          latest CHANGELOG release parser
  plugin_xml         plugin.xml descriptor and change-note parser
  normalization      shared text and bullet normalization helpers
  release_metadata   version/date/change-note consistency checks
  visible_traces     repository-visible trace guard
  cli                orchestration
"""

from __future__ import annotations

import sys

from verify_docs.cli import main


if __name__ == "__main__":
    sys.exit(main())
