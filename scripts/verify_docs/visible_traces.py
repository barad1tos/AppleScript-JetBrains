"""Repository-visible generated-trace guard for release docs metadata."""

from __future__ import annotations

from .changelog import parse_latest_changelog_entry
from .plugin_xml import load_plugin_xml_metadata
from .report import Report

_FORBIDDEN_VISIBLE_TRACES = (
    "Sou" + "rcery",
    "Cla" + "ude",
    "Cod" + "ex",
    "Chat" + "GPT",
    "AI " + "assi" + "stant",
    "assi" + "stant",
)


def check_visible_traces(report: Report) -> None:
    changelog = parse_latest_changelog_entry()
    plugin_xml = load_plugin_xml_metadata()

    visible_docs = {
        "CHANGELOG.md": changelog.body if changelog is not None else "",
        "plugin.xml description": plugin_xml.description,
        "plugin.xml change-notes": plugin_xml.change_notes,
    }
    for source, text in visible_docs.items():
        for trace in _FORBIDDEN_VISIBLE_TRACES:
            if trace in text:
                report.error(source, f"contains forbidden visible trace: {trace}")
