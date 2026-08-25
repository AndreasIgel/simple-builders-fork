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

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link ActivePerformanceTracker}.
 *
 * <p>Tests cover phase tracking, generator/enhancer tracking, class metrics, JSON report output
 * structure and correctness, edge cases, and ThreadLocal cleanup.
 */
class ActivePerformanceTrackerTest {

  @TempDir Path tempDir;

  /** Creates a ProcessingLogger with a capturing Messager for verification. */
  private ProcessingLogger createLogger(List<String> messages) {
    ProcessingEnvironment env =
        new ProcessingEnvironment() {
          @Override
          public Map<String, String> getOptions() {
            return Collections.emptyMap();
          }

          @Override
          public Messager getMessager() {
            return new CapturingMessager(messages);
          }

          @Override
          public Filer getFiler() {
            return null;
          }

          @Override
          public Elements getElementUtils() {
            return null;
          }

          @Override
          public Types getTypeUtils() {
            return null;
          }

          @Override
          public SourceVersion getSourceVersion() {
            return SourceVersion.latest();
          }

          @Override
          public Locale getLocale() {
            return Locale.getDefault();
          }
        };
    return new ProcessingLogger(env);
  }

  /** Messager that captures all messages into a list for assertion. */
  private static final class CapturingMessager implements Messager {
    private final List<String> messages;

    CapturingMessager(List<String> messages) {
      this.messages = messages;
    }

    @Override
    public void printMessage(Diagnostic.Kind kind, CharSequence msg) {
      messages.add(kind + ": " + msg);
    }

    @Override
    public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e) {
      messages.add(kind + ": " + msg);
    }

    @Override
    public void printMessage(
        Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a) {
      messages.add(kind + ": " + msg);
    }

    @Override
    public void printMessage(
        Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a, AnnotationValue v) {
      messages.add(kind + ": " + msg);
    }
  }

  /** Helper to create a tracker, track some data, and generate report with JSON output. */
  private JsonNode generateReportAndParseJson(String outputFile) throws IOException {
    ActivePerformanceTracker tracker = new ActivePerformanceTracker(outputFile);
    tracker.startClass("TestClassA");
    tracker.startPhase();
    tracker.endPhase(PerformanceTracker.PHASE_BUILDER_DEFINITION_EXTRACTION);
    tracker.startGenerator();
    tracker.endGenerator("FieldSupplierGenerator");
    tracker.startEnhancer();
    tracker.endEnhancer("CoreMethodsEnhancer");
    tracker.endClass(5, 2);

    tracker.startClass("TestClassB");
    tracker.startPhase();
    tracker.endPhase(PerformanceTracker.PHASE_DTO_MAPPING);
    tracker.endClass(3, 1);

    List<String> messages = new ArrayList<>();
    ProcessingLogger logger = createLogger(messages);
    tracker.generateReport(logger);

    String jsonContent = Files.readString(Path.of(outputFile));
    return new ObjectMapper().readTree(jsonContent);
  }

  @Test
  void generateReport_withNoData_logsBasicReport() {
    ActivePerformanceTracker tracker = new ActivePerformanceTracker(null);
    List<String> messages = new ArrayList<>();
    ProcessingLogger logger = createLogger(messages);

    tracker.generateReport(logger);

    assertTrue(messages.stream().anyMatch(m -> m.contains("PERFORMANCE REPORT")));
    assertTrue(messages.stream().anyMatch(m -> m.contains("Total classes processed: 0")));
  }

  @Test
  void generateReport_withClassData_logsClassCount() {
    ActivePerformanceTracker tracker = new ActivePerformanceTracker(null);
    tracker.startClass("MyClass");
    tracker.endClass(4, 1);

    List<String> messages = new ArrayList<>();
    ProcessingLogger logger = createLogger(messages);
    tracker.generateReport(logger);

    assertTrue(messages.stream().anyMatch(m -> m.contains("Total classes processed: 1")));
    assertTrue(messages.stream().anyMatch(m -> m.contains("MyClass")));
  }

  @Test
  void generateReport_withGeneratorData_logsGeneratorStats() {
    ActivePerformanceTracker tracker = new ActivePerformanceTracker(null);
    tracker.startClass("MyClass");
    tracker.startGenerator();
    tracker.endGenerator("MyGenerator");
    tracker.endClass(2, 0);

    List<String> messages = new ArrayList<>();
    ProcessingLogger logger = createLogger(messages);
    tracker.generateReport(logger);

    assertTrue(messages.stream().anyMatch(m -> m.contains("MethodGenerators")));
    assertTrue(messages.stream().anyMatch(m -> m.contains("MyGenerator")));
  }

  @Test
  void generateReport_withEnhancerData_logsEnhancerStats() {
    ActivePerformanceTracker tracker = new ActivePerformanceTracker(null);
    tracker.startClass("MyClass");
    tracker.startEnhancer();
    tracker.endEnhancer("MyEnhancer");
    tracker.endClass(2, 0);

    List<String> messages = new ArrayList<>();
    ProcessingLogger logger = createLogger(messages);
    tracker.generateReport(logger);

    assertTrue(messages.stream().anyMatch(m -> m.contains("BuilderEnhancers")));
    assertTrue(messages.stream().anyMatch(m -> m.contains("MyEnhancer")));
  }

  @Test
  void generateReport_withPhaseData_logsPhaseBreakdown() {
    ActivePerformanceTracker tracker = new ActivePerformanceTracker(null);
    tracker.startPhase();
    tracker.endPhase(PerformanceTracker.PHASE_CONFIGURATION_RESOLUTION);
    tracker.startPhase();
    tracker.endPhase(PerformanceTracker.PHASE_CODE_GENERATION);

    List<String> messages = new ArrayList<>();
    ProcessingLogger logger = createLogger(messages);
    tracker.generateReport(logger);

    assertTrue(messages.stream().anyMatch(m -> m.contains("Phase breakdown")));
    assertTrue(
        messages.stream()
            .anyMatch(m -> m.contains(PerformanceTracker.PHASE_CONFIGURATION_RESOLUTION)));
    assertTrue(
        messages.stream().anyMatch(m -> m.contains(PerformanceTracker.PHASE_CODE_GENERATION)));
  }

  @Test
  void generateReport_withNullOutputFile_doesNotWriteFile() {
    ActivePerformanceTracker tracker = new ActivePerformanceTracker(null);
    tracker.startClass("MyClass");
    tracker.endClass(1, 0);

    List<String> messages = new ArrayList<>();
    ProcessingLogger logger = createLogger(messages);
    tracker.generateReport(logger);

    assertFalse(messages.stream().anyMatch(m -> m.contains("JSON report written")));
  }

  @Test
  void generateReport_withOutputFile_writesValidJson() throws IOException {
    Path jsonFile = tempDir.resolve("report.json");
    JsonNode root = generateReportAndParseJson(jsonFile.toString());

    assertTrue(root.has("timestamp"));
    assertEquals(2, root.get("totalClasses").asInt());
    assertTrue(root.has("totalProcessingTimeNanos"));
    assertTrue(root.has("totalProcessingTimeSeconds"));
    assertTrue(root.has("averagePerClassMs"));
    assertTrue(root.has("phaseBreakdown"));
    assertTrue(root.has("classMetrics"));
    assertTrue(root.has("generatorStats"));
    assertTrue(root.has("enhancerStats"));
  }

  @Test
  void jsonReport_classMetrics_containsAllClassesSortedByElapsedDesc() throws IOException {
    Path jsonFile = tempDir.resolve("report.json");
    JsonNode root = generateReportAndParseJson(jsonFile.toString());

    JsonNode classMetrics = root.get("classMetrics");
    assertEquals(2, classMetrics.size());
    assertEquals("TestClassA", classMetrics.get(0).get("className").asText());
    assertEquals("TestClassB", classMetrics.get(1).get("className").asText());
    assertTrue(
        classMetrics.get(0).get("elapsedNanos").asLong()
            >= classMetrics.get(1).get("elapsedNanos").asLong());
  }

  @Test
  void jsonReport_classMetrics_containsFieldAndCollectionCounts() throws IOException {
    Path jsonFile = tempDir.resolve("report.json");
    JsonNode root = generateReportAndParseJson(jsonFile.toString());

    JsonNode classA = root.get("classMetrics").get(0);
    assertEquals(5, classA.get("fieldCount").asInt());
    assertEquals(2, classA.get("collectionCount").asInt());
    assertTrue(classA.has("elapsedMs"));
  }

  @Test
  void jsonReport_generatorStats_containsGeneratorData() throws IOException {
    Path jsonFile = tempDir.resolve("report.json");
    JsonNode root = generateReportAndParseJson(jsonFile.toString());

    JsonNode genStats = root.get("generatorStats");
    assertEquals(1, genStats.size());
    assertEquals("FieldSupplierGenerator", genStats.get(0).get("name").asText());
    assertEquals(1, genStats.get(0).get("calls").asInt());
    assertTrue(genStats.get(0).has("elapsedNanos"));
    assertTrue(genStats.get(0).has("avgMsPerCall"));
  }

  @Test
  void jsonReport_enhancerStats_containsEnhancerData() throws IOException {
    Path jsonFile = tempDir.resolve("report.json");
    JsonNode root = generateReportAndParseJson(jsonFile.toString());

    JsonNode enhStats = root.get("enhancerStats");
    assertEquals(1, enhStats.size());
    assertEquals("CoreMethodsEnhancer", enhStats.get(0).get("name").asText());
    assertEquals(1, enhStats.get(0).get("calls").asInt());
  }

  @Test
  void jsonReport_phaseBreakdown_containsPhaseHierarchy() throws IOException {
    Path jsonFile = tempDir.resolve("report.json");
    JsonNode root = generateReportAndParseJson(jsonFile.toString());

    JsonNode phases = root.get("phaseBreakdown");
    assertTrue(phases.has(PerformanceTracker.PHASE_CONFIGURATION_RESOLUTION));
    assertTrue(phases.has(PerformanceTracker.PHASE_BUILDER_DEFINITION_EXTRACTION));
    assertTrue(phases.has(PerformanceTracker.PHASE_DTO_MAPPING));
    assertTrue(phases.has(PerformanceTracker.PHASE_CODE_GENERATION));

    JsonNode codeGen = phases.get(PerformanceTracker.PHASE_CODE_GENERATION);
    assertTrue(codeGen.has("elapsedNanos"));
    assertTrue(codeGen.has("elapsedSeconds"));
    assertTrue(codeGen.has("percentage"));
  }

  @Test
  void jsonReport_phaseBreakdown_codeGenerationHasChildren() throws IOException {
    Path jsonFile = tempDir.resolve("report.json");
    JsonNode root = generateReportAndParseJson(jsonFile.toString());

    JsonNode codeGen = root.get("phaseBreakdown").get(PerformanceTracker.PHASE_CODE_GENERATION);
    assertTrue(codeGen.has("children"));
    JsonNode children = codeGen.get("children");
    assertTrue(children.has(PerformanceTracker.PHASE_SOURCE_CONSTRUCTION));
    assertTrue(children.has(PerformanceTracker.PHASE_FILE_WRITING));
  }

  @Test
  void jsonReport_withNoData_hasEmptyArrays() throws IOException {
    Path jsonFile = tempDir.resolve("report-empty.json");
    ActivePerformanceTracker tracker = new ActivePerformanceTracker(jsonFile.toString());

    List<String> messages = new ArrayList<>();
    ProcessingLogger logger = createLogger(messages);
    tracker.generateReport(logger);

    JsonNode root = new ObjectMapper().readTree(Files.readString(jsonFile));
    assertEquals(0, root.get("totalClasses").asInt());
    assertTrue(root.get("classMetrics").isArray());
    assertEquals(0, root.get("classMetrics").size());
    assertTrue(root.get("generatorStats").isArray());
    assertEquals(0, root.get("generatorStats").size());
    assertTrue(root.get("enhancerStats").isArray());
    assertEquals(0, root.get("enhancerStats").size());
  }

  @Test
  void jsonReport_createsParentDirectoriesIfMissing() throws IOException {
    Path jsonFile = tempDir.resolve("subdir").resolve("nested").resolve("report.json");
    JsonNode root = generateReportAndParseJson(jsonFile.toString());

    assertTrue(Files.exists(jsonFile));
    assertEquals(2, root.get("totalClasses").asInt());
  }

  @Test
  void endMethodsWithoutStart_doesNothing() {
    ActivePerformanceTracker tracker = new ActivePerformanceTracker(null);
    tracker.endPhase(PerformanceTracker.PHASE_CONFIGURATION_RESOLUTION);
    tracker.endGenerator("NonexistentGenerator");
    tracker.endEnhancer("NonexistentEnhancer");
    tracker.endClass(5, 2);

    List<String> messages = new ArrayList<>();
    ProcessingLogger logger = createLogger(messages);
    tracker.generateReport(logger);

    assertTrue(messages.stream().anyMatch(m -> m.contains("PERFORMANCE REPORT")));
    assertTrue(messages.stream().anyMatch(m -> m.contains("Total classes processed: 0")));
    assertFalse(messages.stream().anyMatch(m -> m.contains("MethodGenerators")));
    assertFalse(messages.stream().anyMatch(m -> m.contains("BuilderEnhancers")));
  }

  @Test
  void multipleGenerators_accumulateTimeAndCalls() throws IOException {
    Path jsonFile = tempDir.resolve("report-multi-gen.json");
    ActivePerformanceTracker tracker = new ActivePerformanceTracker(jsonFile.toString());
    tracker.startClass("MyClass");
    tracker.startGenerator();
    tracker.endGenerator("GenA");
    tracker.startGenerator();
    tracker.endGenerator("GenA");
    tracker.startGenerator();
    tracker.endGenerator("GenB");
    tracker.endClass(2, 0);

    List<String> messages = new ArrayList<>();
    ProcessingLogger logger = createLogger(messages);
    tracker.generateReport(logger);

    JsonNode root = new ObjectMapper().readTree(Files.readString(jsonFile));
    JsonNode genStats = root.get("generatorStats");
    assertEquals(2, genStats.size());

    JsonNode genA =
        genStats.get(0).get("name").asText().equals("GenA") ? genStats.get(0) : genStats.get(1);
    assertEquals(2, genA.get("calls").asInt());
  }

  @Test
  void multiplePhases_accumulateTime() throws IOException {
    Path jsonFile = tempDir.resolve("report-multi-phase.json");
    ActivePerformanceTracker tracker = new ActivePerformanceTracker(jsonFile.toString());
    tracker.startPhase();
    tracker.endPhase(PerformanceTracker.PHASE_CONFIGURATION_RESOLUTION);
    tracker.startPhase();
    tracker.endPhase(PerformanceTracker.PHASE_CONFIGURATION_RESOLUTION);

    List<String> messages = new ArrayList<>();
    ProcessingLogger logger = createLogger(messages);
    tracker.generateReport(logger);

    JsonNode root = new ObjectMapper().readTree(Files.readString(jsonFile));
    JsonNode phase =
        root.get("phaseBreakdown").get(PerformanceTracker.PHASE_CONFIGURATION_RESOLUTION);
    assertTrue(phase.get("elapsedNanos").asLong() > 0);
  }

  @Test
  void generateReport_logsWarningOnInvalidPath() {
    ActivePerformanceTracker tracker =
        new ActivePerformanceTracker("/nonexistent\0invalid/path.json");
    tracker.startClass("MyClass");
    tracker.endClass(1, 0);

    List<String> messages = new ArrayList<>();
    ProcessingLogger logger = createLogger(messages);
    tracker.generateReport(logger);

    assertTrue(
        messages.stream().anyMatch(m -> m.contains("WARNING") && m.contains("Failed to write")));
  }

  @Test
  void noOpPerformanceTracker_allMethodsAreNoOps() {
    NoOpPerformanceTracker tracker = new NoOpPerformanceTracker();
    List<String> messages = new ArrayList<>();
    ProcessingLogger logger = createLogger(messages);

    tracker.startPhase();
    tracker.endPhase("Phase");
    tracker.startGenerator();
    tracker.endGenerator("Gen");
    tracker.startEnhancer();
    tracker.endEnhancer("Enh");
    tracker.startClass("Class");
    tracker.endClass(1, 0);
    tracker.generateReport(logger);

    assertTrue(messages.isEmpty());
  }

  @Test
  void jsonReport_generatorAndEnhancerStats_sortedByElapsedDesc() throws IOException {
    Path jsonFile = tempDir.resolve("report-sorted.json");
    ActivePerformanceTracker tracker = new ActivePerformanceTracker(jsonFile.toString());
    tracker.startClass("MyClass");
    tracker.startGenerator();
    tracker.endGenerator("SlowGen");
    tracker.startGenerator();
    tracker.endGenerator("FastGen");
    tracker.startEnhancer();
    tracker.endEnhancer("SlowEnh");
    tracker.startEnhancer();
    tracker.endEnhancer("FastEnh");
    tracker.endClass(2, 0);

    List<String> messages = new ArrayList<>();
    ProcessingLogger logger = createLogger(messages);
    tracker.generateReport(logger);

    JsonNode root = new ObjectMapper().readTree(Files.readString(jsonFile));
    JsonNode genStats = root.get("generatorStats");
    long first = genStats.get(0).get("elapsedNanos").asLong();
    long second = genStats.get(1).get("elapsedNanos").asLong();
    assertTrue(first >= second, "Generator stats should be sorted by elapsed descending");

    JsonNode enhStats = root.get("enhancerStats");
    long firstE = enhStats.get(0).get("elapsedNanos").asLong();
    long secondE = enhStats.get(1).get("elapsedNanos").asLong();
    assertTrue(firstE >= secondE, "Enhancer stats should be sorted by elapsed descending");
  }
}
