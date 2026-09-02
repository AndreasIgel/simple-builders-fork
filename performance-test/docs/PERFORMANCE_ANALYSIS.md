# Performance Analysis Guide

This guide explains how to run performance analysis for different builder
frameworks using the scripts in the `performance-test` module.

## Overview

The performance test module generates a large set of Java classes (records and
plain classes) from a JSON catalog, then compiles them with a builder annotation
processor to measure processing time. Three scripts work together:

| Script | Purpose |
|---|---|
| `generate_classes.py` | Generate Java source files with a chosen builder annotation |
| `run_performance_measurement.py` | Run N compilations and aggregate timing results |
| `compare_performance.py` | Compare results from multiple measurement runs side-by-side |
| `run_full_comparison.py` | Run all frameworks end-to-end and compare (convenience) |

## Supported Builder Types

| `--builder-type` | Annotation | Maven Profile | JSON Report | Notes |
|---|---|---|---|---|
| `simple-builder` | `@SimpleBuilder` | `simplebuilder` | Yes | Full feature set (default) |
| `simple-minimal-builder` | `@MinimalBuilder` | `minimalbuilder` | Yes | All optional features disabled |
| `record-builder` | `@RecordBuilder` | `recordbuilder` | No | RecordBuilder library (wall-time only) |
| `lombok` | `@Builder` | `lombok` | No | Lombok (wall-time only) |

Builder types with JSON reports produce detailed phase/class/generator/enhancer
metrics. Builder types without JSON reports only measure overall wall time.

## Quick Start

Run all four frameworks with N runs each, then compare:

```bash
python3 scripts/run_full_comparison.py --runs 10
```

Use `--keep-builders` to copy generated builders to `generated-builders/<type>/`
so they survive Maven clean:

```bash
python3 scripts/run_full_comparison.py --runs 30 --keep-builders
```

Labels are auto-generated as `sb-<N>runs`, `mb-<N>runs`, `rb-<N>runs`,
`lombok-<N>runs`.

### Running Individual Frameworks

For single-framework measurements, use the individual scripts directly.
Use `--force` when regenerating classes after switching builder types.

```bash
python3 scripts/generate_classes.py --builder-type simple-builder --force
python3 scripts/run_performance_measurement.py --runs 30 --label sb-30runs --builder-type simple-builder
```

Results are written to `performance-test/performance-reports/<label>/`. Compare paths
are relative to that directory (or use absolute paths). The parent directory
name is used as the column label.

When comparing JSON-profile runs with wall-time-only runs, the script
automatically shows wall time for all summaries and detailed metrics
(processor time, phases, classes, generators, enhancers) only when
available.

## Output File Locations

```
performance-test/
└── performance-reports/
    ├── sb-30runs/
    │   ├── run-01.json
    │   ├── run-02.json
    │   ├── ...
    │   └── summary.json
    ├── mb-30runs/
    │   ├── run-01.json
    │   ├── ...
    │   └── summary.json
    ├── rb-30runs/
    │   └── summary.json       (wall-time only)
    └── lombok-30runs/
        └── summary.json       (wall-time only)
```

## Summary JSON Structure

### JSON-Profile Summaries (simple-builder, simple-minimal-builder)

```json
{
  "runCount": 30,
  "wallTime": { "min": ..., "max": ..., "avg": ..., "values": [...] },
  "sourceFileCount": 1077,
  "generatedBuilderCount": 1077,
  "compilerTime": { "min": ..., "max": ..., "avg": ..., "values": [...] },
  "compilerTimePerBuilderMs": { "min": ..., "max": ..., "avg": ... },
  "processorTime": { "min": ..., "max": ..., "avg": ..., "values": [...] },
  "averagePerClassMs": { "min": ..., "max": ..., "avg": ..., "values": [...] },
  "totalClasses": 1077,
  "phaseAverageNanos": { "Phase.Name": avgNanos, ... },
  "topClassesByAvg": [
    { "className": ..., "avgMs": ..., "minMs": ..., "maxMs": ...,
      "fieldCount": ..., "collectionCount": ... }
  ],
  "generatorStats": [
    { "name": ..., "avgElapsedNanos": ..., "avgCalls": ..., "avgMsPerCall": ... }
  ],
  "enhancerStats": [
    { "name": ..., "avgElapsedNanos": ..., "avgCalls": ..., "avgMsPerCall": ... }
  ]
}
```

### Wall-Time-Only Summaries (record-builder, lombok)

```json
{
  "runCount": 30,
  "wallTime": { "min": ..., "max": ..., "avg": ..., "values": [...] },
  "sourceFileCount": 1077,
  "generatedBuilderCount": 1077,
  "compilerTime": { "min": ..., "max": ..., "avg": ..., "values": [...] },
  "compilerTimePerBuilderMs": { "min": ..., "max": ..., "avg": ... },
  "wallTimeOnly": true
}
```
