"""Shared text normalization helpers for docs metadata comparisons."""

from __future__ import annotations

import html
import re


def normalize_text(text: str) -> str:
    without_tags = re.sub(r"<[^>]+>", "", html.unescape(text))
    without_markdown_code = without_tags.replace("`", "")
    return re.sub(r"\s+", " ", without_markdown_code).strip()


def markdown_bullets(text: str) -> set[str]:
    return {normalize_text(match.group(1)) for match in re.finditer(r"^- (.+)$", text, re.MULTILINE)}


def html_bullets(text: str) -> set[str]:
    bullets = re.findall(r"<li>\s*(.*?)\s*</li>", text, re.IGNORECASE | re.DOTALL)
    return {normalize_text(bullet) for bullet in bullets}
