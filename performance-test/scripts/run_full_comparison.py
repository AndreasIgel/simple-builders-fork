#!/usr/bin/env python3
"""Run a full performance comparison across all supported builder frameworks.

Generates classes for each builder type, runs N measurement runs, copies
generated builders to a safe location, and compares all results.

Usage:
    python3 scripts/run_full_comparison.py [options]

Options:
    --runs N          Number of compilation runs per framework (default: 10)
    --keep-builders   Copy generated builders to generated-builders/<type>/
                      so they survive Maven clean

This script runs the full pipeline:
  1. For each builder type (simple-builder, simple-minimal-builder,
     record-builder, lombok):
     a. Generate source classes with generate_classes.py
     b. Run N measurement runs with run_performance_measurement.py
     c. Optionally copy generated builders to a safe location
  2. Compare all summaries with compare_performance.py
"""

from __future__ import annotations

import argparse
import shutil
import subprocess
import sys
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
BASE_DIR = SCRIPT_DIR.parent


def safe_rmtree(path: Path) -> None:
    """Remove a directory tree only if it is inside BASE_DIR.

    Guards against accidental deletion of unexpected locations.
    """
    resolved = path.resolve()
    if not resolved.is_relative_to(BASE_DIR):
        raise ValueError(f"Refusing to remove {resolved}: outside {BASE_DIR}")
    if resolved == BASE_DIR:
        raise ValueError(f"Refusing to remove {resolved}: is BASE_DIR itself")
    shutil.rmtree(resolved)

BUILDER_TYPES = ["simple-builder", "simple-minimal-builder", "record-builder", "lombok"]

LABEL_PREFIX = {
    "simple-builder": "sb",
    "simple-minimal-builder": "mb",
    "record-builder": "rb",
    "lombok": "lombok",
}


def run(cmd: list[str], cwd: Path = BASE_DIR) -> int:
    """Run a command and stream output to the terminal."""
    print(f"$ {' '.join(cmd)}")
    result = subprocess.run(cmd, cwd=cwd)
    print()
    return result.returncode


def print_section(title: str) -> None:
    """Print a visible section header."""
    print(f"{'=' * 60}")
    print(f"  {title}")
    print(f"{'=' * 60}")
    print()


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Run full performance comparison across all builder frameworks."
    )
    parser.add_argument(
        "--runs",
        type=int,
        default=10,
        help="Number of compilation runs per framework (default: 10)",
    )
    parser.add_argument(
        "--keep-builders",
        action="store_true",
        help="Copy generated builders to generated-builders/<type>/ "
        "so they survive Maven clean",
    )
    args = parser.parse_args()

    num_runs = args.runs
    keep_builders = args.keep_builders
    summaries: list[str] = []

    for bt in BUILDER_TYPES:
        label = f"{LABEL_PREFIX[bt]}-{num_runs}runs"
        print_section(f"{bt}  (label: {label})")

        # 1. Generate classes
        rc = run([
            sys.executable, str(SCRIPT_DIR / "generate_classes.py"),
            "--builder-type", bt, "--force",
        ])
        if rc != 0:
            print(f"ERROR: generate_classes.py failed for {bt}")
            sys.exit(1)

        # 2. Run measurements
        rc = run([
            sys.executable, str(SCRIPT_DIR / "run_performance_measurement.py"),
            "--runs", str(num_runs),
            "--label", label,
            "--builder-type", bt,
        ])
        if rc != 0:
            print(f"ERROR: run_performance_measurement.py failed for {bt}")
            sys.exit(1)

        # 3. Copy generated builders to safe location
        if keep_builders:
            dst = BASE_DIR / "generated-builders" / bt
            if dst.exists():
                safe_rmtree(dst)
            if bt == "lombok":
                # Lombok instruments bytecode in-place; no separate source files.
                src = BASE_DIR / "target" / "classes"
            else:
                src = BASE_DIR / "generated-performance-builder"
            if src.exists():
                shutil.copytree(src, dst)
                print(f"Copied generated builders to: {dst}")
                print()

        summaries.append(f"{label}/summary.json")

    # 4. Compare all
    print_section("Comparison")

    compare_cmd = [sys.executable, str(SCRIPT_DIR / "compare_performance.py")] + summaries
    rc = run(compare_cmd)
    if rc != 0:
        print("ERROR: compare_performance.py failed")
        sys.exit(1)

    print()
    print("Full comparison complete.")
    print(f"Reports: {BASE_DIR / 'performance-reports'}")


if __name__ == "__main__":
    main()
