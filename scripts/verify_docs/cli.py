"""argparse + orchestration for scripts/verify-docs.py."""

from __future__ import annotations

import argparse

from .release_metadata import check_release_metadata
from .report import Report
from .visible_traces import check_visible_traces


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Verify release docs metadata across gradle.properties, CHANGELOG.md, and plugin.xml.",
    )
    parser.parse_args()

    report = Report()
    check_release_metadata(report)
    check_visible_traces(report)
    report.print()
    return 1 if report.has_errors else 0
