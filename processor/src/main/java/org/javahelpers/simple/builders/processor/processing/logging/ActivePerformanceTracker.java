/*
 * MIT License
 *
 * Copyright (c) 2026 Andreas Igel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.javahelpers.simple.builders.processor.processing.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Active implementation of {@link PerformanceTracker} that measures execution times using {@link
 * System#nanoTime()} and aggregates results for a summary report.
 *
 * <p>Timing uses {@link ThreadLocal} stacks for {@code start*}/{@code end*} calls: each {@code
 * start} pushes a timestamp onto the stack, and the matching {@code end} pops it and accumulates
 * the elapsed time. This allows nested calls without passing identifiers.
 *
 * <p>The tracker maintains per-phase totals (with a hardcoded hierarchy defined by {@link
 * #PHASE_CHILDREN} for report display), per-generator and per-enhancer totals with call counts, and
 * per-class metrics including field and collection counts.
 *
 * <p>When an output file path is provided, {@link #generateReport(ProcessingLogger)} writes a
 * structured JSON report with hierarchical phase breakdown, class metrics, and generator/enhancer
 * statistics for automated analysis.
 *
 * <p><b>Thread-safety:</b> All methods are safe for concurrent use. The {@code start*}/{@code end*}
 * methods use per-thread {@link ThreadLocal} stacks, so each thread's timing is isolated.
 * Aggregation maps are {@link ConcurrentHashMap}s and the class counter is an {@link
 * AtomicInteger}, allowing concurrent writes from multiple threads. The {@code startClass}/{@code
 * endClass} pair uses per-thread state, so each thread must call them in matched pairs.
 */
public final class ActivePerformanceTracker implements PerformanceTracker {

  /** Hardcoded phase hierarchy for report display. Order defines display order. */
  private static final List<String> TOP_LEVEL_PHASES =
      List.of(
          PHASE_CONFIGURATION_RESOLUTION,
          PHASE_BUILDER_DEFINITION_EXTRACTION,
          PHASE_DTO_MAPPING,
          PHASE_CODE_GENERATION);

  private static final String JSON_KEY_ELAPSED_NANOS = "elapsedNanos";

  private static final Map<String, List<String>> PHASE_CHILDREN = new LinkedHashMap<>();

  static {
    PHASE_CHILDREN.put(
        PHASE_CODE_GENERATION, List.of(PHASE_SOURCE_CONSTRUCTION, PHASE_FILE_WRITING));
    PHASE_CHILDREN.put(
        PHASE_SOURCE_CONSTRUCTION,
        List.of(PHASE_ELEMENT_BUILDING, PHASE_STRING_GENERATION, PHASE_FORMATTING));
    PHASE_CHILDREN.put(
        PHASE_ELEMENT_BUILDING,
        List.of(
            PHASE_CLASS_CREATION,
            PHASE_CLASS_METADATA,
            PHASE_FIELDS,
            PHASE_CONSTRUCTORS,
            PHASE_METHODS,
            PHASE_NESTED_TYPES,
            PHASE_CLASS_ANNOTATIONS));
  }

  private final Map<String, Long> phaseTimes = new ConcurrentHashMap<>();
  private final Map<String, Long> generatorTimes = new ConcurrentHashMap<>();
  private final Map<String, Integer> generatorCalls = new ConcurrentHashMap<>();
  private final Map<String, Long> enhancerTimes = new ConcurrentHashMap<>();
  private final Map<String, Integer> enhancerCalls = new ConcurrentHashMap<>();
  private final List<ClassMetric> classMetrics = Collections.synchronizedList(new ArrayList<>());

  private final ThreadLocal<List<Long>> phaseStartStack = ThreadLocal.withInitial(ArrayList::new);
  private final ThreadLocal<List<Long>> generatorStartStack =
      ThreadLocal.withInitial(ArrayList::new);
  private final ThreadLocal<List<Long>> enhancerStartStack =
      ThreadLocal.withInitial(ArrayList::new);

  private final long totalStartTime;
  private final AtomicInteger totalClasses = new AtomicInteger(0);
  private final String outputFilePath;

  private final ThreadLocal<String> currentClassName = new ThreadLocal<>();
  private final ThreadLocal<Long> classStartTime = new ThreadLocal<>();

  /**
   * Creates a new ActivePerformanceTracker and records the overall start time.
   *
   * @param outputFilePath optional path for JSON report output; null or empty disables file output
   */
  public ActivePerformanceTracker(String outputFilePath) {
    this.totalStartTime = System.nanoTime();
    this.outputFilePath = outputFilePath;
  }

  @Override
  public void startPhase() {
    phaseStartStack.get().add(System.nanoTime());
  }

  /**
   * Pops the most recent timestamp from the given ThreadLocal stack and returns the elapsed
   * nanoseconds since that timestamp. Returns -1 if the stack is empty (unmatched end call).
   */
  private static long popElapsed(ThreadLocal<List<Long>> startStack) {
    List<Long> stack = startStack.get();
    if (stack.isEmpty()) {
      return -1;
    }
    long start = stack.remove(stack.size() - 1);
    return System.nanoTime() - start;
  }

  @Override
  public void endPhase(String phase) {
    long elapsed = popElapsed(phaseStartStack);
    if (elapsed >= 0) {
      phaseTimes.merge(phase, elapsed, Long::sum);
    }
  }

  @Override
  public void startGenerator() {
    generatorStartStack.get().add(System.nanoTime());
  }

  @Override
  public void endGenerator(String generatorName) {
    long elapsed = popElapsed(generatorStartStack);
    if (elapsed >= 0) {
      generatorTimes.merge(generatorName, elapsed, Long::sum);
      generatorCalls.merge(generatorName, 1, Integer::sum);
    }
  }

  @Override
  public void startEnhancer() {
    enhancerStartStack.get().add(System.nanoTime());
  }

  @Override
  public void endEnhancer(String enhancerName) {
    long elapsed = popElapsed(enhancerStartStack);
    if (elapsed >= 0) {
      enhancerTimes.merge(enhancerName, elapsed, Long::sum);
      enhancerCalls.merge(enhancerName, 1, Integer::sum);
    }
  }

  @Override
  public void startClass(String className) {
    this.currentClassName.set(className);
    this.classStartTime.set(System.nanoTime());
  }

  @Override
  public void endClass(int fieldCount, int collectionCount) {
    String name = currentClassName.get();
    if (name == null) {
      return;
    }
    long elapsed = System.nanoTime() - classStartTime.get();
    classMetrics.add(new ClassMetric(name, elapsed, fieldCount, collectionCount));
    totalClasses.incrementAndGet();
    currentClassName.remove();
    classStartTime.remove();
  }

  @Override
  public void generateReport(ProcessingLogger logger) {
    long totalTime = System.nanoTime() - totalStartTime;
    double totalSeconds = totalTime / 1_000_000_000.0;

    logger.info("simple-builders: PERFORMANCE REPORT");
    logger.info("================================");
    logger.info("Total classes processed: %d", totalClasses.get());
    logger.info(String.format(Locale.US, "Total processing time: %.1fs", totalSeconds));
    logger.info("");

    // Phase breakdown (hierarchical, using hardcoded hierarchy)
    logger.info("Phase breakdown:");
    for (int i = 0; i < TOP_LEVEL_PHASES.size(); i++) {
      reportPhase(
          logger, TOP_LEVEL_PHASES.get(i), totalSeconds, "", i == TOP_LEVEL_PHASES.size() - 1);
    }
    logger.info("");

    // Average per class
    if (totalClasses.get() > 0) {
      double avgPerClass = (totalTime / 1_000_000.0) / totalClasses.get();
      logger.info(String.format(Locale.US, "Average per class: %.1fms", avgPerClass));
      logger.info("");
    }

    reportTopClasses(logger);
    reportTopGenerators(logger);
    reportTopEnhancers(logger);
    writeJsonReportIfNeeded(logger, totalTime);

    // Clean up ThreadLocals to prevent memory leaks
    phaseStartStack.remove();
    generatorStartStack.remove();
    enhancerStartStack.remove();
  }

  /**
   * Reports the top 20 slowest classes to the logger.
   *
   * @param logger the processing logger to output the report
   */
  private void reportTopClasses(ProcessingLogger logger) {
    List<ClassMetric> topClasses = new ArrayList<>(classMetrics);
    topClasses.sort(Comparator.comparingLong(ClassMetric::elapsedNanos).reversed());
    int classLimit = Math.min(20, topClasses.size());
    if (classLimit <= 0) {
      return;
    }
    logger.info("Top %d slowest classes:", classLimit);
    for (int i = 0; i < classLimit; i++) {
      ClassMetric cm = topClasses.get(i);
      double ms = cm.elapsedNanos() / 1_000_000.0;
      logger.info(
          String.format(
              Locale.US,
              "  %d. %s - %.1fms (%d fields, %d collections)",
              i + 1,
              cm.className(),
              ms,
              cm.fieldCount(),
              cm.collectionCount()));
    }
    logger.info("");
  }

  /**
   * Reports the top 5 slowest MethodGenerators to the logger.
   *
   * @param logger the processing logger to output the report
   */
  private void reportTopGenerators(ProcessingLogger logger) {
    List<Map.Entry<String, Long>> topGenerators = new ArrayList<>(generatorTimes.entrySet());
    topGenerators.sort(Map.Entry.<String, Long>comparingByValue().reversed());
    int genLimit = Math.min(5, topGenerators.size());
    if (genLimit <= 0) {
      return;
    }
    logger.info("Top %d slowest MethodGenerators:", genLimit);
    for (int i = 0; i < genLimit; i++) {
      Map.Entry<String, Long> entry = topGenerators.get(i);
      double seconds = entry.getValue() / 1_000_000_000.0;
      int calls = generatorCalls.getOrDefault(entry.getKey(), 0);
      double avgMs = calls > 0 ? (entry.getValue() / 1_000_000.0) / calls : 0;
      logger.info(
          String.format(
              Locale.US,
              "  %d. %s - %.1fs (%d calls, %.2fms/call)",
              i + 1,
              entry.getKey(),
              seconds,
              calls,
              avgMs));
    }
    logger.info("");
  }

  /**
   * Reports the top 5 slowest BuilderEnhancers to the logger.
   *
   * @param logger the processing logger to output the report
   */
  private void reportTopEnhancers(ProcessingLogger logger) {
    List<Map.Entry<String, Long>> topEnhancers = new ArrayList<>(enhancerTimes.entrySet());
    topEnhancers.sort(Map.Entry.<String, Long>comparingByValue().reversed());
    int enhLimit = Math.min(5, topEnhancers.size());
    if (enhLimit <= 0) {
      return;
    }
    logger.info("Top %d slowest BuilderEnhancers:", enhLimit);
    for (int i = 0; i < enhLimit; i++) {
      Map.Entry<String, Long> entry = topEnhancers.get(i);
      double seconds = entry.getValue() / 1_000_000_000.0;
      int calls = enhancerCalls.getOrDefault(entry.getKey(), 0);
      double avgMs = calls > 0 ? (entry.getValue() / 1_000_000.0) / calls : 0;
      logger.info(
          String.format(
              Locale.US,
              "  %d. %s - %.1fs (%d calls, %.2fms/call)",
              i + 1,
              entry.getKey(),
              seconds,
              calls,
              avgMs));
    }
  }

  /**
   * Writes the JSON report to the configured output file if one is set.
   *
   * @param logger the processing logger for status messages
   * @param totalTime total processing time in nanoseconds
   */
  private void writeJsonReportIfNeeded(ProcessingLogger logger, long totalTime) {
    if (outputFilePath == null || outputFilePath.isBlank()) {
      return;
    }
    try {
      writeJsonReport(totalTime);
      logger.info("Performance JSON report written to: %s", outputFilePath);
    } catch (IOException | java.nio.file.InvalidPathException | SecurityException e) {
      logger.warning("Failed to write performance JSON report: %s", e.getMessage());
    }
  }

  /** Record for per-class performance metrics. */
  private record ClassMetric(
      String className, long elapsedNanos, int fieldCount, int collectionCount) {}

  private void reportPhase(
      ProcessingLogger logger, String phase, double parentSeconds, String prefix, boolean isLast) {
    long nanos = phaseTimes.getOrDefault(phase, 0L);
    double seconds = nanos / 1_000_000_000.0;
    double percentage = parentSeconds > 0 ? (seconds / parentSeconds) * 100 : 0;
    String connector = isLast ? "└─ " : "├─ ";
    logger.info(
        String.format(
            Locale.US, "%s%s%s: %.1fs (%.1f%%)", prefix, connector, phase, seconds, percentage));
    List<String> children = PHASE_CHILDREN.get(phase);
    if (children != null) {
      String childPrefix = prefix + (isLast ? "   " : "│  ");
      for (int i = 0; i < children.size(); i++) {
        reportPhase(logger, children.get(i), seconds, childPrefix, i == children.size() - 1);
      }
    }
  }

  /**
   * Writes the performance report as structured JSON to the configured output file.
   *
   * <p>Builds the report as a {@link Map}/{@link List} structure and serializes it with a simple
   * recursive serializer, avoiding external JSON dependencies.
   *
   * @param totalNanos total processing time in nanoseconds
   * @throws IOException if the file cannot be written
   */
  private void writeJsonReport(long totalNanos) throws IOException {
    double totalSeconds = totalNanos / 1_000_000_000.0;
    int classCount = totalClasses.get();
    double avgPerClassMs = classCount > 0 ? (totalNanos / 1_000_000.0) / classCount : 0;

    Map<String, Object> root = new LinkedHashMap<>();
    root.put("timestamp", Instant.now().toString());
    root.put("totalClasses", classCount);
    root.put("totalProcessingTimeNanos", totalNanos);
    root.put("totalProcessingTimeSeconds", totalSeconds);
    root.put("averagePerClassMs", avgPerClassMs);

    // Phase breakdown (hierarchical)
    Map<String, Object> phaseBreakdown = new LinkedHashMap<>();
    for (String phase : TOP_LEVEL_PHASES) {
      phaseBreakdown.put(phase, buildPhaseJson(phase, totalNanos));
    }
    root.put("phaseBreakdown", phaseBreakdown);

    // Class metrics (sorted by elapsed time descending)
    List<ClassMetric> sortedClasses = new ArrayList<>(classMetrics);
    sortedClasses.sort(Comparator.comparingLong(ClassMetric::elapsedNanos).reversed());
    List<Map<String, Object>> classMetricsList = new ArrayList<>();
    for (ClassMetric cm : sortedClasses) {
      Map<String, Object> metric = new LinkedHashMap<>();
      metric.put("className", cm.className());
      metric.put(JSON_KEY_ELAPSED_NANOS, cm.elapsedNanos());
      metric.put("elapsedMs", cm.elapsedNanos() / 1_000_000.0);
      metric.put("fieldCount", cm.fieldCount());
      metric.put("collectionCount", cm.collectionCount());
      classMetricsList.add(metric);
    }
    root.put("classMetrics", classMetricsList);

    // Generator and enhancer stats
    root.put("generatorStats", buildNamedStatsJson(generatorTimes, generatorCalls));
    root.put("enhancerStats", buildNamedStatsJson(enhancerTimes, enhancerCalls));

    String json = toJsonString(root);
    Path outPath = Paths.get(outputFilePath);
    if (outPath.getParent() != null) {
      Files.createDirectories(outPath.getParent());
    }
    Files.writeString(outPath, json + "\n", StandardCharsets.UTF_8);
  }

  /**
   * Builds a phase entry as a JSON-compatible map, including children recursively.
   *
   * @param phase the phase name
   * @param parentNanos the parent phase total in nanoseconds (for percentage calculation)
   * @return a map representing the phase entry
   */
  private Map<String, Object> buildPhaseJson(String phase, long parentNanos) {
    long nanos = phaseTimes.getOrDefault(phase, 0L);
    double seconds = nanos / 1_000_000_000.0;
    double percentage = parentNanos > 0 ? (nanos * 100.0 / parentNanos) : 0;
    Map<String, Object> phaseMap = new LinkedHashMap<>();
    phaseMap.put(JSON_KEY_ELAPSED_NANOS, nanos);
    phaseMap.put("elapsedSeconds", seconds);
    phaseMap.put("percentage", percentage);
    List<String> children = PHASE_CHILDREN.get(phase);
    if (children != null && !children.isEmpty()) {
      Map<String, Object> childrenMap = new LinkedHashMap<>();
      for (String child : children) {
        childrenMap.put(child, buildPhaseJson(child, nanos));
      }
      phaseMap.put("children", childrenMap);
    }
    return phaseMap;
  }

  /**
   * Builds named statistics (generators or enhancers) as a JSON-compatible list.
   *
   * @param timesMap map of names to elapsed nanoseconds
   * @param callsMap map of names to call counts
   * @return a list of maps representing each stat entry
   */
  private List<Map<String, Object>> buildNamedStatsJson(
      Map<String, Long> timesMap, Map<String, Integer> callsMap) {
    List<Map.Entry<String, Long>> sorted = new ArrayList<>(timesMap.entrySet());
    sorted.sort(Map.Entry.<String, Long>comparingByValue().reversed());
    List<Map<String, Object>> stats = new ArrayList<>();
    for (Map.Entry<String, Long> entry : sorted) {
      int calls = callsMap.getOrDefault(entry.getKey(), 0);
      double avgMs = calls > 0 ? (entry.getValue() / 1_000_000.0) / calls : 0;
      Map<String, Object> stat = new LinkedHashMap<>();
      stat.put("name", entry.getKey());
      stat.put(JSON_KEY_ELAPSED_NANOS, entry.getValue());
      stat.put("calls", calls);
      stat.put("avgMsPerCall", avgMs);
      stats.add(stat);
    }
    return stats;
  }

  /**
   * Serializes a JSON-compatible object (Map, List, String, Number, Boolean, or null) to a
   * pretty-printed JSON string with 2-space indentation.
   *
   * @param value the value to serialize
   * @return the JSON string representation
   */
  private static String toJsonString(Object value) {
    StringBuilder sb = new StringBuilder(4096);
    appendJson(sb, value, 0);
    return sb.toString();
  }

  /**
   * Recursively appends a JSON-compatible value to the string builder.
   *
   * @param sb the string builder to append to
   * @param value the value to serialize
   * @param indent the current indentation level
   */
  private static void appendJson(StringBuilder sb, Object value, int indent) {
    if (value == null) {
      sb.append("null");
    } else if (value instanceof Map<?, ?> map) {
      appendJsonMap(sb, map, indent);
    } else if (value instanceof List<?> list) {
      appendJsonList(sb, list, indent);
    } else if (value instanceof String s) {
      sb.append(jsonString(s));
    } else if (value instanceof Number n) {
      sb.append(n);
    } else if (value instanceof Boolean b) {
      sb.append(b);
    } else {
      sb.append(jsonString(value.toString()));
    }
  }

  private static void appendJsonMap(StringBuilder sb, Map<?, ?> map, int indent) {
    if (map.isEmpty()) {
      sb.append("{}");
      return;
    }
    String pad = "  ".repeat(indent);
    String pad2 = pad + "  ";
    sb.append("{\n");
    List<? extends Map.Entry<?, ?>> entries = new ArrayList<>(map.entrySet());
    for (int i = 0; i < entries.size(); i++) {
      Map.Entry<?, ?> entry = entries.get(i);
      sb.append(pad2).append(jsonString(entry.getKey().toString())).append(": ");
      appendJson(sb, entry.getValue(), indent + 1);
      if (i < entries.size() - 1) {
        sb.append(",");
      }
      sb.append("\n");
    }
    sb.append(pad).append("}");
  }

  private static void appendJsonList(StringBuilder sb, List<?> list, int indent) {
    if (list.isEmpty()) {
      sb.append("[]");
      return;
    }
    String pad = "  ".repeat(indent);
    String pad2 = pad + "  ";
    sb.append("[\n");
    for (int i = 0; i < list.size(); i++) {
      sb.append(pad2);
      appendJson(sb, list.get(i), indent + 1);
      if (i < list.size() - 1) {
        sb.append(",");
      }
      sb.append("\n");
    }
    sb.append(pad).append("]");
  }

  /**
   * Escapes a string value for JSON output.
   *
   * @param value the raw string
   * @return the JSON-escaped string wrapped in double quotes
   */
  private static String jsonString(String value) {
    if (value == null) {
      return "null";
    }
    StringBuilder escaped = new StringBuilder(value.length() + 2);
    escaped.append('"');
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '"' -> escaped.append("\\\"");
        case '\\' -> escaped.append("\\\\");
        case '\n' -> escaped.append("\\n");
        case '\r' -> escaped.append("\\r");
        case '\t' -> escaped.append("\\t");
        case '\b' -> escaped.append("\\b");
        case '\f' -> escaped.append("\\f");
        default -> {
          if (c < 0x20) {
            escaped.append(String.format("\\u%04x", (int) c));
          } else {
            escaped.append(c);
          }
        }
      }
    }
    escaped.append('"');
    return escaped.toString();
  }
}
