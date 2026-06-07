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
    entries = parse_changelog_entries()
    return entries[0] if entries else None


def parse_changelog_entries() -> list[ChangelogEntry]:
    text = CHANGELOG.read_text(encoding="utf-8")
    headings = list(_VERSION_HEADER.finditer(text))
    if not headings:
        return []

    entries: list[ChangelogEntry] = []
    for index, heading in enumerate(headings):
        body_start = heading.end()
        body_end = headings[index + 1].start() if index + 1 < len(headings) else len(text)
        entries.append(
            ChangelogEntry(
                version=heading.group("version"),
                date=heading.group("date"),
                body=text[body_start:body_end],
            ),
        )
    return entries
