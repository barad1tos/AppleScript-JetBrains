"""Latest CHANGELOG release entry parser."""

from __future__ import annotations

import re
from dataclasses import dataclass

from .paths import CHANGELOG

_VERSION_HEADER = re.compile(
    r"^## \[(?P<version>[^]]+)] - (?P<date>\d{4}-\d{2}-\d{2})\s*$",
    re.MULTILINE,
)


@dataclass(frozen=True)
class ChangelogEntry:
    version: str
    date: str
    body: str


def parse_latest_changelog_entry() -> ChangelogEntry | None:
    text = CHANGELOG.read_text(encoding="utf-8")
    entries = list(_VERSION_HEADER.finditer(text))
    if not entries:
        return None

    first = entries[0]
    body_start = first.end()
    body_end = entries[1].start() if len(entries) > 1 else len(text)
    return ChangelogEntry(
        version=first.group("version"),
        date=first.group("date"),
        body=text[body_start:body_end],
    )
