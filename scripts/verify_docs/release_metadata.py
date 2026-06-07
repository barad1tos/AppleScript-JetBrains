"""Release metadata invariants across gradle.properties, CHANGELOG, and plugin.xml."""

from __future__ import annotations

from .changelog import ChangelogEntry, parse_changelog_entries, parse_latest_changelog_entry
from .gradle_properties import load_gradle_properties
from .normalization import html_bullets, markdown_bullets
from .plugin_xml import load_plugin_xml_metadata
from .report import Report


CHANGELOG_FILE = "CHANGELOG.md"
PLUGIN_XML_FILE = "plugin.xml"


def check_release_metadata(report: Report) -> None:
    gradle_properties = load_gradle_properties()
    plugin_version = gradle_properties.get("pluginVersion")
    if not plugin_version:
        report.error("gradle.properties", "missing pluginVersion")
        return

    latest_changelog = parse_latest_changelog_entry()
    if latest_changelog is None:
        report.error(CHANGELOG_FILE, "missing latest release entry shaped as '## [x.y.z] - YYYY-MM-DD'")
        return

    try:
        plugin_xml = load_plugin_xml_metadata()
    except ValueError as error:
        report.error(PLUGIN_XML_FILE, str(error))
        return

    if latest_changelog.version != plugin_version:
        report.error(
            CHANGELOG_FILE,
            f"latest version {latest_changelog.version} does not match pluginVersion {plugin_version}",
        )

    expected_release_version = expected_marketplace_release_version(plugin_version)
    expected_release_date = expected_marketplace_release_date(plugin_version, parse_changelog_entries())
    if expected_release_date is None:
        report.error(
            CHANGELOG_FILE,
            f"missing initial changelog entry for Marketplace release-version {expected_release_version}",
        )
    elif plugin_xml.release_date != expected_release_date:
        report.error(
            PLUGIN_XML_FILE,
            f"release-date {plugin_xml.release_date} does not match Marketplace major release date "
            f"{expected_release_date}",
        )

    if plugin_xml.release_version != expected_release_version:
        report.error(
            PLUGIN_XML_FILE,
            f"release-version {plugin_xml.release_version} does not match pluginVersion prefix "
            f"{expected_release_version}",
        )

    if not plugin_xml.change_note_entries:
        report.error(PLUGIN_XML_FILE, "<change-notes> has no <h3>version</h3> entries")
        return

    latest_change_notes = plugin_xml.change_note_entries[0]
    if latest_change_notes.version != plugin_version:
        report.error(
            PLUGIN_XML_FILE,
            f"first change-note version {latest_change_notes.version} does not match pluginVersion {plugin_version}",
        )

    changelog_bullets = markdown_bullets(latest_changelog.body)
    change_note_bullets = html_bullets(latest_change_notes.body)
    for bullet in sorted(changelog_bullets - change_note_bullets):
        report.error(PLUGIN_XML_FILE, f"CHANGELOG bullet missing from change-notes: {bullet}")
    for bullet in sorted(change_note_bullets - changelog_bullets):
        report.error(CHANGELOG_FILE, f"plugin.xml change-note bullet missing from CHANGELOG: {bullet}")


def expected_marketplace_release_version(plugin_version: str) -> str:
    parts = plugin_version.split(".")
    if len(parts) < 2 or not all(part.isdigit() for part in parts[:2]):
        raise ValueError(f"Unsupported pluginVersion format: {plugin_version}")
    return f"{int(parts[0])}{int(parts[1])}"


def expected_marketplace_release_date(plugin_version: str, changelog_entries: list[ChangelogEntry]) -> str | None:
    release_version = expected_marketplace_release_version(plugin_version)
    matching_entries = [
        entry
        for entry in changelog_entries
        if expected_marketplace_release_version(entry.version) == release_version
    ]
    if not matching_entries:
        return None
    return matching_entries[-1].date.replace("-", "")
