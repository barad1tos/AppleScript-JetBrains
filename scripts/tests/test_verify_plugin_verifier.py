#!/usr/bin/env python3
"""CLI-boundary tests for verify-plugin-verifier.py."""

from __future__ import annotations

import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / "verify-plugin-verifier.py"


class VerifyPluginVerifierTest(unittest.TestCase):
    def assert_gate_failed(
        self,
        result: subprocess.CompletedProcess[str],
        *expected_error_fragments: str,
    ) -> None:
        self.assertNotEqual(0, result.returncode)
        for expected_error_fragment in expected_error_fragments:
            self.assertIn(expected_error_fragment, result.stderr)

    def assert_gate_succeeded(
        self,
        result: subprocess.CompletedProcess[str],
        *expected_output_fragments: str,
    ) -> None:
        self.assertEqual(0, result.returncode)
        for expected_output_fragment in expected_output_fragments:
            self.assertIn(expected_output_fragment, result.stdout)

    def test_missing_reports_directory_fails(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            reports_dir = Path(temporary_directory) / "missing"

            result = run_gate(reports_dir)

        self.assert_gate_failed(result, "reports directory not found")

    def test_reports_without_verdict_files_fail(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            reports_dir = Path(temporary_directory)

            result = run_gate(reports_dir)

        self.assert_gate_failed(result, "No verification-verdict.txt files found")

    def test_internal_api_usage_fails(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            target = make_target(Path(temporary_directory))
            (target / "internal-api-usages.txt").write_text(
                "Internal API method com.intellij.Secret.call() invocation\n",
                encoding="utf-8",
            )

            result = run_gate(Path(temporary_directory))

        self.assert_gate_failed(
            result,
            "reported internal API usage",
            "com.intellij.Secret.call()",
        )

    def test_deprecated_and_experimental_usage_is_advisory_by_default(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            result = run_gate_with_api_warnings(Path(temporary_directory))

        self.assert_gate_succeeded(result, "deprecated=1", "experimental=1")
        self.assertIn("Deprecated API method", result.stderr)
        self.assertIn("Experimental API method", result.stderr)

    def test_strict_mode_fails_unapproved_api_warnings(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            reports_dir = Path(temporary_directory)

            result = run_gate_with_api_warnings(
                reports_dir,
                "--fail-on-api-warnings",
            )

        self.assert_gate_failed(
            result,
            "Deprecated API usage",
            "Experimental API usage",
        )

    def test_strict_mode_passes_without_api_warnings(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            reports_dir = Path(temporary_directory)
            make_target(reports_dir)

            result = run_gate(
                reports_dir,
                "--fail-on-api-warnings",
            )

        self.assert_gate_succeeded(result, "deprecated=0", "experimental=0")


def make_target(reports_dir: Path) -> Path:
    target = (
        reports_dir
        / "IC-251"
        / "plugins"
        / "software.barad1tos.applescript.toolkit"
        / "2.0.5"
    )
    target.mkdir(parents=True)
    (target / "verification-verdict.txt").write_text("Compatible\n", encoding="utf-8")
    return target


def write_api_warning_files(target: Path) -> None:
    (target / "deprecated-usages.txt").write_text(
        "Deprecated API method com.intellij.Old.call() invocation\n",
        encoding="utf-8",
    )
    (target / "experimental-api-usages.txt").write_text(
        "Experimental API method com.intellij.New.call() invocation\n",
        encoding="utf-8",
    )


def run_gate_with_api_warnings(
    reports_dir: Path,
    *extra_arguments: str,
) -> subprocess.CompletedProcess[str]:
    write_api_warning_files(make_target(reports_dir))
    return run_gate(reports_dir, *extra_arguments)


def run_gate(
    reports_dir: Path,
    *extra_arguments: str,
) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [
            sys.executable,
            str(SCRIPT),
            "--reports-dir",
            str(reports_dir),
            *extra_arguments,
        ],
        check=False,
        text=True,
        capture_output=True,
    )


if __name__ == "__main__":
    unittest.main()
