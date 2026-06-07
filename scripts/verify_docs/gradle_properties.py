"""gradle.properties parser for release metadata checks."""

from __future__ import annotations

from .paths import GRADLE_PROPERTIES


def load_gradle_properties() -> dict[str, str]:
    properties: dict[str, str] = {}
    for line in GRADLE_PROPERTIES.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        properties[key.strip()] = value.strip()
    return properties
