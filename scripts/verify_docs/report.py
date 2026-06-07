"""Finding + Report aggregation surface for docs-verification checks."""

from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True)
class Finding:
    severity: str
    source: str
    message: str


def _empty_findings() -> list[Finding]:
    return []


@dataclass
class Report:
    findings: list[Finding] = field(default_factory=_empty_findings)

    def error(self, source: str, message: str) -> None:
        self.findings.append(Finding("error", source, message))

    def warn(self, source: str, message: str) -> None:
        self.findings.append(Finding("warn", source, message))

    @property
    def has_errors(self) -> bool:
        return any(finding.severity == "error" for finding in self.findings)

    def print(self) -> None:
        if not self.findings:
            print("Docs metadata verification passed.")
            return

        for finding in self.findings:
            prefix = "ERROR" if finding.severity == "error" else "warn "
            print(f"  [{prefix}] {finding.source}: {finding.message}")
