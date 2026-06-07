#!/usr/bin/env python3
"""Verify release metadata stays synchronized across project docs.

Adapted from the ayu-jetbrains docs verifier, but scoped to this repository's
metadata sources:

  gradle.properties                  pluginVersion
  CHANGELOG.md                       latest user-facing release entry
  src/main/resources/META-INF/plugin.xml
                                      product descriptor + Marketplace notes

This script is intentionally stdlib-only so CI and release workflows can run it
with the system Python.
"""

from __future__ import annotations

import html
import re
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
GRADLE_PROPERTIES = REPO_ROOT / "gradle.properties"
CHANGELOG = REPO_ROOT / "CHANGELOG.md"
PLUGIN_XML = REPO_ROOT / "src/main/resources/META-INF/plugin.xml"

FORBIDDEN_VISIBLE_TRACES = (
    "Sou" + "rcery",
    "Cla" + "ude",
    "Cod" + "ex",
    "Chat" + "GPT",
    "AI " + "assi" + "stant",
    "assi" + "stant",
)


@dataclass(frozen=True)
class ReleaseEntry:
    version: str
    date: str
    body: str


def main() -> int:
    findings: list[str] = []

    gradle_properties = load_properties(GRADLE_PROPERTIES)
    plugin_version = gradle_properties.get("pluginVersion")
    if not plugin_version:
        findings.append("gradle.properties is missing pluginVersion")
        return report(findings)

    changelog = CHANGELOG.read_text(encoding="utf-8")
    plugin_xml = ET.parse(PLUGIN_XML).getroot()
    product_descriptor = plugin_xml.find("product-descriptor")
    if product_descriptor is None:
        findings.append("plugin.xml is missing <product-descriptor>")
        return report(findings)

    change_notes = plugin_xml.findtext("change-notes") or ""
    description = plugin_xml.findtext("description") or ""

    latest_entry = parse_latest_changelog_entry(changelog)
    if latest_entry is None:
        findings.append("CHANGELOG.md has no release entry shaped as '## [x.y.z] - YYYY-MM-DD'")
        return report(findings)

    if latest_entry.version != plugin_version:
        findings.append(
            f"CHANGELOG latest version {latest_entry.version} does not match pluginVersion {plugin_version}",
        )

    release_date = product_descriptor.get("release-date")
    expected_release_date = latest_entry.date.replace("-", "")
    if release_date != expected_release_date:
        findings.append(
            f"plugin.xml release-date {release_date} does not match CHANGELOG date {expected_release_date}",
        )

    release_version = product_descriptor.get("release-version")
    expected_release_version = expected_marketplace_release_version(plugin_version)
    if release_version != expected_release_version:
        findings.append(
            f"plugin.xml release-version {release_version} does not match pluginVersion prefix "
            f"{expected_release_version}",
        )

    change_note_entries = parse_change_note_entries(change_notes)
    if not change_note_entries:
        findings.append("plugin.xml <change-notes> has no <h3>version</h3> entries")
    elif change_note_entries[0].version != plugin_version:
        findings.append(
            f"plugin.xml first change-note version {change_note_entries[0].version} "
            f"does not match pluginVersion {plugin_version}",
        )

    if change_note_entries:
        changelog_bullets = normalized_bullets_from_changelog(latest_entry.body)
        change_note_bullets = normalized_bullets_from_html(change_note_entries[0].body)
        missing_from_plugin = sorted(changelog_bullets - change_note_bullets)
        missing_from_changelog = sorted(change_note_bullets - changelog_bullets)
        for bullet in missing_from_plugin:
            findings.append(f"CHANGELOG bullet missing from plugin.xml change-notes: {bullet}")
        for bullet in missing_from_changelog:
            findings.append(f"plugin.xml change-note bullet missing from CHANGELOG: {bullet}")

    visible_docs = {
        "CHANGELOG.md": latest_entry.body,
        "plugin.xml description": description,
        "plugin.xml change-notes": change_notes,
    }
    for source, text in visible_docs.items():
        for trace in FORBIDDEN_VISIBLE_TRACES:
            if trace in text:
                findings.append(f"{source} contains forbidden visible trace: {trace}")

    return report(findings)


def load_properties(path: Path) -> dict[str, str]:
    properties: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        properties[key.strip()] = value.strip()
    return properties


def parse_latest_changelog_entry(text: str) -> ReleaseEntry | None:
    entries = list(
        re.finditer(
            r"^## \[(?P<version>[^\]]+)\] - (?P<date>\d{4}-\d{2}-\d{2})\s*$",
            text,
            re.MULTILINE,
        ),
    )
    if not entries:
        return None

    first = entries[0]
    body_start = first.end()
    body_end = entries[1].start() if len(entries) > 1 else len(text)
    return ReleaseEntry(
        version=first.group("version"),
        date=first.group("date"),
        body=text[body_start:body_end],
    )


def parse_change_note_entries(change_notes: str) -> list[ReleaseEntry]:
    headings = list(
        re.finditer(
            r"<h3>\s*(?P<version>[^<]+?)\s*</h3>",
            change_notes,
            re.IGNORECASE,
        ),
    )
    entries: list[ReleaseEntry] = []
    for index, heading in enumerate(headings):
        body_start = heading.end()
        body_end = headings[index + 1].start() if index + 1 < len(headings) else len(change_notes)
        entries.append(
            ReleaseEntry(
                version=html.unescape(heading.group("version").strip()),
                date="",
                body=change_notes[body_start:body_end],
            ),
        )
    return entries


def normalized_bullets_from_changelog(text: str) -> set[str]:
    return {normalize_text(match.group(1)) for match in re.finditer(r"^- (.+)$", text, re.MULTILINE)}


def normalized_bullets_from_html(text: str) -> set[str]:
    bullets = re.findall(r"<li>\s*(.*?)\s*</li>", text, re.IGNORECASE | re.DOTALL)
    return {normalize_text(bullet) for bullet in bullets}


def normalize_text(text: str) -> str:
    without_tags = re.sub(r"<[^>]+>", "", html.unescape(text))
    without_markdown_code = without_tags.replace("`", "")
    return re.sub(r"\s+", " ", without_markdown_code).strip()


def expected_marketplace_release_version(plugin_version: str) -> str:
    parts = plugin_version.split(".")
    if len(parts) < 2 or not all(part.isdigit() for part in parts[:2]):
        raise ValueError(f"Unsupported pluginVersion format: {plugin_version}")
    return f"{int(parts[0])}{int(parts[1])}"


def report(findings: list[str]) -> int:
    if not findings:
        print("Docs metadata verification passed.")
        return 0

    print("Docs metadata verification failed:", file=sys.stderr)
    for finding in findings:
        print(f"- {finding}", file=sys.stderr)
    return 1


if __name__ == "__main__":
    sys.exit(main())
