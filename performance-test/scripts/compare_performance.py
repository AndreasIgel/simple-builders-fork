#!/usr/bin/env python3
"""Compare performance summary JSON files.

Usage:
    python3 scripts/compare_performance.py [options] FILE1 [FILE2] ...

Examples:
    # Compare 3-run vs 30-run simplebuilder results
    python3 scripts/compare_performance.py \
        3runs/summary.json \
        30runs/summary.json

    # Compare simplebuilder vs lombok (wall-time only)
    python3 scripts/compare_performance.py \
        sb-30runs/summary.json \
        lombok-30runs/summary.json

    # Compare three frameworks
    python3 scripts/compare_performance.py \
        sb-30runs/summary.json \
        rb-30runs/summary.json \
        lombok-30runs/summary.json

Each FILE argument is a path to a summary.json file. The parent directory
name is used as the column label (e.g. `sb-30runs/summary.json` → label
`sb-30runs`). The script auto-discovers phase names from the data and
gracefully handles summaries that lack phase data (e.g. wall-time-only
runs from RecordBuilder or Lombok).
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

BASE = Path(__file__).resolve().parent.parent / "performance-reports"


def load_summary(filepath: str) -> dict | None:
    """Load a summary JSON file, returning None on error."""
    path = Path(filepath)
    if not path.is_absolute():
        path = BASE / path
    if not path.exists():
        print(f"Warning: file not found: {path}", file=sys.stderr)
        return None
    with path.open() as f:
        try:
            return json.load(f)
        except json.JSONDecodeError as e:
            print(f"Error: invalid JSON in {path}: {e}", file=sys.stderr)
            return None


def shorten_phase(name: str) -> str:
    """Abbreviate long phase names for compact display."""
    return (
        name.replace("Code Generation.", "CG.")
        .replace("Source Construction.", "SC.")
        .replace("Element Building.", "EB.")
        .replace("Configuration Resolution", "Config Resolution")
        .replace("Builder Definition Extraction", "Builder Def Extraction")
    )


def print_min_max_avg(title: str, summaries: list[tuple[str, dict]], key: str,
                       labels: list[str]) -> None:
    """Print a Min/Max/Avg table for a summary section that all summaries have."""
    n = len(labels)
    header = "{:<16}" + " {:>12}" * n
    row = "{:<16}" + " {:>12.1f}" * n
    print(f"--- {title} ---")
    print(header.format("Metric", *labels))
    for stat in ("min", "max", "avg"):
        vals = [s[key][stat] for _, s in summaries]
        print(row.format(stat.capitalize(), *vals))
    print()


def print_min_max_avg_opt(title: str, summaries: list[tuple[str, dict]], key: str,
                           labels: list[str]) -> None:
    """Like print_min_max_avg but for sections that only some summaries have.
    Uses .get() with 0.0 fallback for missing keys."""
    if not any(key in s for _, s in summaries):
        return
    n = len(labels)
    header = "{:<16}" + " {:>12}" * n
    row = "{:<16}" + " {:>12.1f}" * n
    print(f"--- {title} ---")
    print(header.format("Metric", *labels))
    for stat in ("min", "max", "avg"):
        vals = [s.get(key, {}).get(stat, 0.0) for _, s in summaries]
        print(row.format(stat.capitalize(), *vals))
    print()


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Compare performance summary JSON files.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    parser.add_argument(
        "files",
        nargs="+",
        help="Paths to summary.json files (e.g. sb-30runs/summary.json)",
    )
    args = parser.parse_args()

    # Derive labels from parent directory names
    entries: list[tuple[str, str]] = []
    for f in args.files:
        p = Path(f)
        label = p.parent.name if p.parent.name else p.name
        entries.append((f, label))

    # Load all summaries
    summaries: list[tuple[str, dict]] = []
    for filepath, label in entries:
        s = load_summary(filepath)
        if s is None:
            print(f"Error: could not load summary for '{label}' from {filepath}", file=sys.stderr)
            sys.exit(1)
        summaries.append((label, s))

    if not summaries:
        print("Error: no valid summary files loaded.", file=sys.stderr)
        sys.exit(1)

    labels = [label for label, _ in summaries]
    n = len(labels)

    # Determine which sections are available
    has_processor_time = all("processorTime" in s for _, s in summaries)
    has_per_class = all("averagePerClassMs" in s for _, s in summaries)
    has_phases = all("phaseAverageNanos" in s for _, s in summaries)

    # Header
    print("=" * 80)
    print(f"PERFORMANCE COMPARISON: {' vs '.join(labels)}")
    print("=" * 80)
    print()

    # Total classes (from first summary that has it)
    for label, s in summaries:
        if "totalClasses" in s:
            print(f"Classes per run ({label}): {s['totalClasses']}")
            break
    if not has_processor_time:
        wall_only_labels = [label for label, s in summaries if s.get("wallTimeOnly", False)]
        if wall_only_labels:
            print(f"Wall-time-only (no JSON): {', '.join(wall_only_labels)}")
    print()

    # --- Source & Builder Counts (always available) ---
    print("--- Source & Builder Counts ---")
    count_fmt = "{:<24}" + " {:>12}" * n
    print(count_fmt.format("Metric", *labels))
    print(count_fmt.format("Source files",
          *[s.get("sourceFileCount", 0) for _, s in summaries]))
    print(count_fmt.format("Generated builders",
          *[s.get("generatedBuilderCount", 0) for _, s in summaries]))
    print(count_fmt.format("Processor-reported",
          *[s.get("totalClasses", "-") for _, s in summaries]))
    print()

    # --- Timing tables ---
    print_min_max_avg("Wall Time (seconds)", summaries, "wallTime", labels)
    print_min_max_avg_opt("Compiler Time (seconds, from Maven timestamps)",
                          summaries, "compilerTime", labels)
    print_min_max_avg_opt("Compiler Time per Builder (ms)",
                          summaries, "compilerTimePerBuilderMs", labels)
    if has_processor_time:
        print_min_max_avg("Processor Time (seconds)", summaries, "processorTime", labels)
    if has_per_class:
        print_min_max_avg("Average per Class (ms)", summaries, "averagePerClassMs", labels)

    # --- Phase Averages ---
    if has_phases:
        phases: list[str] = []
        seen: set[str] = set()
        for _label, s in summaries:
            for p in s.get("phaseAverageNanos", {}):
                if p not in seen:
                    seen.add(p)
                    phases.append(p)
        if phases:
            print("--- Phase Average (seconds) ---")
            phase_fmt = "{:<50}" + " {:>10}" * n
            print(phase_fmt.format("Phase", *labels))
            for p in phases:
                vals = [s.get("phaseAverageNanos", {}).get(p, 0) / 1e9 for _, s in summaries]
                print(phase_fmt.format(shorten_phase(p), *vals))
            print()


if __name__ == "__main__":
    main()
