#!/usr/bin/env python3
"""Run the performance-test compilation N times and aggregate results.

Usage:
    python3 scripts/run_performance_measurement.py [options]

Options:
    --runs N          Number of compilation runs (default: 10)
    --label LABEL     Subdirectory name under performance-reports (default: <runs>runs)
    --builder-type T  Builder framework preset, same names as generate_classes.py
                      (simple-builder, simple-minimal-builder, record-builder, lombok)

For builder types with JSON reports (simple-builder, simple-minimal-builder), each
run does a clean compile with performanceTracking enabled and writes a JSON report
to performance-reports/<label>/run-<N>.json. After all runs, an aggregated
summary is written to performance-reports/<label>/summary.json.

For builder types without JSON reports (record-builder, lombok), only wall time
is measured. A minimal summary with wall time statistics is written.
"""

from __future__ import annotations

import argparse
import json
import re
import shutil
import subprocess
import sys
import time
from pathlib import Path
from typing import Optional

BASE_DIR = Path(__file__).resolve().parent.parent


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

# Builder types that use the simple-builders processor (produce JSON performance reports)
SIMPLE_BUILDERS_TYPES = {"simple-builder", "simple-minimal-builder"}

# Mapping from --builder-type to Maven profile names
BUILDER_TYPE_TO_PROFILE = {
    "simple-builder": "simplebuilder",
    "simple-minimal-builder": "minimalbuilder",
    "record-builder": "recordbuilder",
    "lombok": "lombok",
}

# Mapping from --builder-type to the annotation name used in generated source files
BUILDER_TYPE_ANNOTATION = {
    "simple-builder": "@SimpleBuilder",
    "simple-minimal-builder": "@SimpleMinimalBuilder",
    "record-builder": "@RecordBuilder",
    "lombok": "@Builder",
}


def count_source_files() -> int:
    """Count Java source files in src/main/java before compilation."""
    src_dir = BASE_DIR / "src" / "main" / "java"
    if not src_dir.exists():
        return 0
    return sum(1 for _ in src_dir.rglob("*.java"))


def count_generated_builders() -> int:
    """Count generated builder files in generated-performance-builder after compilation."""
    gen_dir = BASE_DIR / "generated-performance-builder"
    if not gen_dir.exists():
        return 0
    return sum(1 for _ in gen_dir.rglob("*.java"))


def count_annotated_sources(annotation: str) -> int:
    """Count Java source files in src/main/java containing the given annotation.

    Used as a fallback for builder types that modify classes in-place (e.g. Lombok)
    rather than generating separate source files.
    """
    src_dir = BASE_DIR / "src" / "main" / "java"
    if not src_dir.exists():
        return 0
    count = 0
    for f in src_dir.rglob("*.java"):
        try:
            text = f.read_text()
        except OSError:
            continue
        if annotation in text:
            count += 1
    return count


_TIMESTAMP_RE = re.compile(r"^(\d{2}):(\d{2}):(\d{2})\.(\d{3})")


def parse_compiler_time(output: str) -> Optional[float]:
    """Extract compiler phase duration in seconds from Maven timestamped output.

    Looks for the 'compiler:3.x:compile' line as start and 'BUILD SUCCESS' as end.
    Returns None if timestamps are not present or cannot be parsed.
    """
    lines = output.splitlines()
    start_ts: Optional[float] = None
    end_ts: Optional[float] = None

    for line in lines:
        m = _TIMESTAMP_RE.match(line)
        if not m:
            continue
        h, mi, s, ms = int(m.group(1)), int(m.group(2)), int(m.group(3)), int(m.group(4))
        ts = h * 3600 + mi * 60 + s + ms / 1000.0

        if "compiler:" in line and "compile" in line and start_ts is None:
            start_ts = ts
        if "BUILD SUCCESS" in line and start_ts is not None:
            end_ts = ts
            break

    if start_ts is not None and end_ts is not None:
        return round(end_ts - start_ts, 3)
    return None


def run_one(run_index: int, profile: str, is_simple_builders: bool, report_dir: Path,
           builder_type: str = "") -> Optional[dict]:
    """Run a single clean compile and return the parsed JSON report (or wall-time-only dict).

    The report file uses the run_index in its name so that retries overwrite the failed
    attempt's file rather than accumulating stale files.
    """
    report_file = report_dir / f"run-{run_index:02d}.json"

    source_count = count_source_files()

    print(f"  Run {run_index}: compiling...", flush=True)
    start = time.time()

    cmd = [
        "mvn", "clean", "compile", "-P", profile,
        "-Dorg.slf4j.simpleLogger.showDateTime=true",
        "-Dorg.slf4j.simpleLogger.dateTimeFormat=HH:mm:ss.SSS",
        "--no-transfer-progress",
    ]
    if is_simple_builders:
        cmd.extend([
            "-Dsimplebuilder.performanceTracking=true",
            f"-Dsimplebuilder.performanceOutputFile={report_file}",
        ])

    result = subprocess.run(
        cmd,
        cwd=BASE_DIR,
        capture_output=True,
        text=True,
    )

    elapsed = time.time() - start

    if result.returncode != 0:
        print(f"  Run {run_index}: FAILED (exit {result.returncode}, {elapsed:.1f}s)")
        print(result.stderr[-500:] if result.stderr else "(no stderr)")
        return None

    builder_count = count_generated_builders()
    if builder_count == 0 and builder_type:
        annotation = BUILDER_TYPE_ANNOTATION.get(builder_type, "")
        if annotation:
            builder_count = count_annotated_sources(annotation)
    compiler_time = parse_compiler_time(result.stdout + result.stderr)

    if is_simple_builders:
        if not report_file.exists():
            print(f"  Run {run_index}: compiled OK but no JSON report found ({elapsed:.1f}s)")
            return None

        with report_file.open() as f:
            try:
                data = json.load(f)
            except json.JSONDecodeError as e:
                print(f"  Run {run_index}: JSON report is invalid: {e}", file=sys.stderr)
                return None

        data["sourceFileCount"] = source_count
        data["generatedBuilderCount"] = builder_count
        if compiler_time is not None:
            data["compilerTimeSeconds"] = compiler_time
        compiler_str = f", {compiler_time:.1f}s compiler" if compiler_time else ""
        print(
            f"  Run {run_index}: OK - {data['totalClasses']} builders from {source_count} sources, "
            f"{data['totalProcessingTimeSeconds']}s processor{compiler_str}, "
            f"{elapsed:.1f}s wall",
            flush=True,
        )
        return data
    else:
        compiler_str = f", {compiler_time:.1f}s compiler" if compiler_time else ""
        print(
            f"  Run {run_index}: OK - {builder_count} builders from {source_count} sources, "
            f"{elapsed:.1f}s wall{compiler_str} (no JSON report)",
            flush=True,
        )
        return {
            "_wallTimeSeconds": elapsed,
            "_wallTimeOnly": True,
            "sourceFileCount": source_count,
            "generatedBuilderCount": builder_count,
            "compilerTimeSeconds": compiler_time if compiler_time is not None else 0,
        }


def aggregate(runs: list[dict]) -> dict:
    """Aggregate results from multiple runs into a summary."""
    n = len(runs)
    if n == 0:
        return {"error": "no successful runs"}

    wall_times = [r.get("_wallTimeSeconds", 0) for r in runs]

    summary = {
        "runCount": n,
        "wallTime": {
            "min": min(wall_times),
            "max": max(wall_times),
            "avg": sum(wall_times) / n,
            "values": wall_times,
        },
    }

    # Add source/builder counts (available for all builder types)
    summary["sourceFileCount"] = runs[0].get("sourceFileCount", 0)
    summary["generatedBuilderCount"] = runs[0].get("generatedBuilderCount", 0)

    # Add compiler time aggregation (available for all builder types via Maven timestamps)
    compiler_times = [r.get("compilerTimeSeconds", 0) for r in runs if r.get("compilerTimeSeconds")]
    if compiler_times:
        summary["compilerTime"] = {
            "min": min(compiler_times),
            "max": max(compiler_times),
            "avg": sum(compiler_times) / len(compiler_times),
            "values": compiler_times,
        }
        builder_count = summary["generatedBuilderCount"]
        if builder_count > 0:
            summary["compilerTimePerBuilderMs"] = {
                "min": round(min(compiler_times) / builder_count * 1000, 1),
                "max": round(max(compiler_times) / builder_count * 1000, 1),
                "avg": round(sum(compiler_times) / len(compiler_times) / builder_count * 1000, 1),
            }

    # Check if this is a wall-time-only run (no JSON reports)
    if all(r.get("_wallTimeOnly", False) for r in runs):
        summary["wallTimeOnly"] = True
        return summary

    # Full JSON-based aggregation
    total_times = [r["totalProcessingTimeSeconds"] for r in runs]
    avg_times = [r["averagePerClassMs"] for r in runs]

    summary["processorTime"] = {
        "min": min(total_times),
        "max": max(total_times),
        "avg": sum(total_times) / n,
        "values": total_times,
    }
    summary["averagePerClassMs"] = {
        "min": min(avg_times),
        "max": max(avg_times),
        "avg": sum(avg_times) / n,
        "values": avg_times,
    }
    summary["totalClasses"] = runs[0]["totalClasses"]

    # Aggregate phase breakdown (average elapsedNanos across runs)
    first_phases = runs[0]["phaseBreakdown"]
    phase_avgs = {}  # type: dict[str, float]

    def collect_phase_avg(phases: dict, all_runs_phases: list[dict], path: str = ""):
        for phase_name, phase_data in phases.items():
            full_path = f"{path}.{phase_name}" if path else phase_name
            nanos_list = []
            for run_phases in all_runs_phases:
                p = run_phases
                for part in full_path.split("."):
                    if part in p:
                        p = p[part]
                    elif "children" in p and part in p["children"]:
                        p = p["children"][part]
                    else:
                        p = None
                        break
                if p and "elapsedNanos" in p:
                    nanos_list.append(p["elapsedNanos"])
            if nanos_list:
                phase_avgs[full_path] = sum(nanos_list) / len(nanos_list)
            children = phase_data.get("children")
            if children:
                child_phases = []
                for rp in all_runs_phases:
                    p = rp
                    for part in full_path.split("."):
                        if part in p:
                            p = p[part]
                        elif "children" in p and part in p["children"]:
                            p = p["children"][part]
                        else:
                            p = None
                            break
                    child_phases.append(p.get("children", {}) if p else {})
                collect_phase_avg(children, child_phases, full_path)

    collect_phase_avg(first_phases, [r["phaseBreakdown"] for r in runs])
    summary["phaseAverageNanos"] = phase_avgs

    # Aggregate top 20 slowest classes (by average elapsedMs across runs)
    class_times = {}  # type: dict[str, list[float]]
    class_info: dict[str, dict] = {}
    for run in runs:
        for cm in run["classMetrics"]:
            name = cm["className"]
            class_times.setdefault(name, []).append(cm["elapsedMs"])
            class_info[name] = {
                "fieldCount": cm["fieldCount"],
                "collectionCount": cm["collectionCount"],
            }

    class_avgs = []
    for name, times in class_times.items():
        class_avgs.append({
            "className": name,
            "avgMs": sum(times) / len(times),
            "minMs": min(times),
            "maxMs": max(times),
            "fieldCount": class_info[name]["fieldCount"],
            "collectionCount": class_info[name]["collectionCount"],
        })
    class_avgs.sort(key=lambda x: x["avgMs"], reverse=True)
    summary["topClassesByAvg"] = class_avgs[:20]

    # Aggregate generator stats
    gen_times = {}  # type: dict[str, dict[str, list]]
    for run in runs:
        for gs in run["generatorStats"]:
            name = gs["name"]
            gen_times.setdefault(name, {"elapsedNanos": [], "calls": [], "avgMsPerCall": []})
            gen_times[name]["elapsedNanos"].append(gs["elapsedNanos"])
            gen_times[name]["calls"].append(gs["calls"])
            gen_times[name]["avgMsPerCall"].append(gs["avgMsPerCall"])

    summary["generatorStats"] = []
    for name, vals in gen_times.items():
        summary["generatorStats"].append({
            "name": name,
            "avgElapsedNanos": sum(vals["elapsedNanos"]) / len(vals["elapsedNanos"]),
            "avgCalls": sum(vals["calls"]) / len(vals["calls"]),
            "avgMsPerCall": sum(vals["avgMsPerCall"]) / len(vals["avgMsPerCall"]),
        })
    summary["generatorStats"].sort(key=lambda x: x["avgElapsedNanos"], reverse=True)

    # Aggregate enhancer stats
    enh_times = {}  # type: dict[str, dict[str, list]]
    for run in runs:
        for es in run["enhancerStats"]:
            name = es["name"]
            enh_times.setdefault(name, {"elapsedNanos": [], "calls": [], "avgMsPerCall": []})
            enh_times[name]["elapsedNanos"].append(es["elapsedNanos"])
            enh_times[name]["calls"].append(es["calls"])
            enh_times[name]["avgMsPerCall"].append(es["avgMsPerCall"])

    summary["enhancerStats"] = []
    for name, vals in enh_times.items():
        summary["enhancerStats"].append({
            "name": name,
            "avgElapsedNanos": sum(vals["elapsedNanos"]) / len(vals["elapsedNanos"]),
            "avgCalls": sum(vals["calls"]) / len(vals["calls"]),
            "avgMsPerCall": sum(vals["avgMsPerCall"]) / len(vals["avgMsPerCall"]),
        })
    summary["enhancerStats"].sort(key=lambda x: x["avgElapsedNanos"], reverse=True)

    return summary


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Run performance-test compilation N times and aggregate results."
    )
    parser.add_argument(
        "--runs",
        type=int,
        default=10,
        help="Number of compilation runs (default: 10)",
    )
    parser.add_argument(
        "--label",
        type=str,
        default=None,
        help="Subdirectory name under target/performance-reports (default: <runs>runs)",
    )
    parser.add_argument(
        "--builder-type",
        type=str,
        default="simple-builder",
        choices=list(BUILDER_TYPE_TO_PROFILE.keys()),
        help="Builder framework preset (same names as generate_classes.py --builder-type). "
        "Options: " + ", ".join(BUILDER_TYPE_TO_PROFILE.keys())
        + " (default: simple-builder).",
    )
    parser.add_argument(
        "--max-retries",
        type=int,
        default=3,
        help="Maximum retries per run before giving up (default: 3). "
        "A run that fails (non-zero exit, missing/invalid JSON) is retried "
        "up to this many times before being skipped.",
    )
    parser.add_argument(
        "--no-tracking",
        action="store_true",
        help="Disable JSON performance tracking for simple-builders types. "
        "Runs are measured with wall-time and compiler-time only (like "
        "lombok/record-builder), avoiding the overhead of the processor's "
        "internal performance tracker.",
    )
    args = parser.parse_args()

    num_runs = args.runs
    run_label = args.label or f"{num_runs}runs"
    builder_type = args.builder_type
    profile = BUILDER_TYPE_TO_PROFILE[builder_type]
    is_simple_builders = builder_type in SIMPLE_BUILDERS_TYPES
    # When --no-tracking is set, disable JSON performance tracking for simple-builders
    # types so they are measured with wall-time/compiler-time only (like lombok/record-builder).
    use_tracking = is_simple_builders and not args.no_tracking

    report_dir = BASE_DIR / "performance-reports" / run_label
    if report_dir.exists():
        safe_rmtree(report_dir)
    report_dir.mkdir(parents=True)

    print(f"Running {num_runs} performance measurement runs...")
    print(f"Builder type: {builder_type}")
    print(f"JSON reports: {'yes' if use_tracking else 'no (wall-time only)'}")
    print(f"Report directory: {report_dir}")
    print()

    max_retries = args.max_retries
    runs: list[dict] = []
    total_attempts = 0
    for i in range(1, num_runs + 1):
        data = None
        attempt = 0
        while data is None and attempt <= max_retries:
            attempt += 1
            total_attempts += 1
            if attempt > 1:
                print(f"  Run {i}: retry {attempt - 1}/{max_retries}...", flush=True)
            run_start = time.time()
            data = run_one(i, profile, use_tracking, report_dir, builder_type)
            if data is not None:
                if "_wallTimeSeconds" not in data:
                    data["_wallTimeSeconds"] = time.time() - run_start
                runs.append(data)
            elif attempt > max_retries:
                print(f"  Run {i}: giving up after {max_retries} retries", flush=True)

    print()
    print(f"Successful runs: {len(runs)}/{num_runs}"
          f" ({total_attempts - num_runs} retries used)")

    if not runs:
        print("No successful runs to aggregate.")
        sys.exit(1)

    summary = aggregate(runs)
    report_dir.mkdir(parents=True, exist_ok=True)
    summary_file = report_dir / "summary.json"
    with summary_file.open("w") as f:
        json.dump(summary, f, indent=2)

    print()
    print("=== Summary ===")
    print(f"  Runs: {summary['runCount']}")
    if "totalClasses" in summary:
        print(f"  Classes per run: {summary['totalClasses']}")
        print(f"  Processor time avg: {summary['processorTime']['avg']:.1f}s "
              f"(min: {summary['processorTime']['min']:.1f}s, max: {summary['processorTime']['max']:.1f}s)")
        print(f"  Avg per class: {summary['averagePerClassMs']['avg']:.1f}ms "
              f"(min: {summary['averagePerClassMs']['min']:.1f}ms, max: {summary['averagePerClassMs']['max']:.1f}ms)")
    print(f"  Source files: {summary.get('sourceFileCount', '?')}")
    print(f"  Generated builders: {summary.get('generatedBuilderCount', '?')}")
    if "compilerTime" in summary:
        print(f"  Compiler time avg: {summary['compilerTime']['avg']:.1f}s "
              f"(min: {summary['compilerTime']['min']:.1f}s, max: {summary['compilerTime']['max']:.1f}s)")
    if "compilerTimePerBuilderMs" in summary:
        print(f"  Compiler time per builder avg: {summary['compilerTimePerBuilderMs']['avg']:.1f}ms "
              f"(min: {summary['compilerTimePerBuilderMs']['min']:.1f}ms, max: {summary['compilerTimePerBuilderMs']['max']:.1f}ms)")
    print(f"  Wall time avg: {summary['wallTime']['avg']:.1f}s "
          f"(min: {summary['wallTime']['min']:.1f}s, max: {summary['wallTime']['max']:.1f}s)")
    if "topClassesByAvg" in summary:
        print()
        print("  Top 5 classes by avg time:")
        for c in summary["topClassesByAvg"][:5]:
            print(f"    {c['className']}: {c['avgMs']:.1f}ms avg "
                  f"({c['minMs']:.1f}-{c['maxMs']:.1f}ms, {c['fieldCount']} fields, {c['collectionCount']} collections)")
    print()
    print(f"Summary written to: {summary_file}")


if __name__ == "__main__":
    main()
