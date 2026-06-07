"""Repo-rooted path constants shared across docs-verification modules."""

from __future__ import annotations

from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
GRADLE_PROPERTIES = REPO_ROOT / "gradle.properties"
CHANGELOG = REPO_ROOT / "CHANGELOG.md"
PLUGIN_XML = REPO_ROOT / "src" / "main" / "resources" / "META-INF" / "plugin.xml"
