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

/**
 * Interface for tracking performance metrics during annotation processing.
 *
 * <p>Implementations:
 *
 * <ul>
 *   <li>{@link NoOpPerformanceTracker} - all methods are no-ops, zero overhead (default)
 *   <li>{@link ActivePerformanceTracker} - actual measurement with nanoTime and aggregation
 * </ul>
 *
 * <p>The No-Op pattern ensures that when performance tracking is disabled, the JIT compiler can
 * eliminate all tracking calls entirely, as the receiver type is fixed at construction time and all
 * methods are empty.
 */
public interface PerformanceTracker {

  // Top-level phases
  String PHASE_CONFIGURATION_RESOLUTION = "Configuration Resolution";
  String PHASE_BUILDER_DEFINITION_EXTRACTION = "Builder Definition Extraction";
  String PHASE_DTO_MAPPING = "DTO Mapping";
  String PHASE_CODE_GENERATION = "Code Generation";

  // Code Generation children
  String PHASE_SOURCE_CONSTRUCTION = "Source Construction";
  String PHASE_FILE_WRITING = "File Writing";

  // Source Construction children
  String PHASE_ELEMENT_BUILDING = "Element Building";
  String PHASE_STRING_GENERATION = "String Generation";
  String PHASE_FORMATTING = "Formatting";

  // Element Building children
  String PHASE_CLASS_CREATION = "Class Creation";
  String PHASE_CLASS_METADATA = "Class Metadata";
  String PHASE_FIELDS = "Fields";
  String PHASE_CONSTRUCTORS = "Constructors";
  String PHASE_METHODS = "Methods";
  String PHASE_NESTED_TYPES = "Nested Types";
  String PHASE_CLASS_ANNOTATIONS = "Class Annotations";

  /**
   * Starts tracking a processing phase.
   *
   * <p>The phase name is passed to {@link #endPhase(String)} for recording; this method only
   * records the start timestamp.
   */
  void startPhase();

  /**
   * Ends tracking a processing phase.
   *
   * @param phase the phase identifier that was started
   */
  void endPhase(String phase);

  /**
   * Starts tracking an individual method generator invocation.
   *
   * <p>The generator name is passed to {@link #endGenerator(String)} for recording; this method
   * only records the start timestamp.
   */
  void startGenerator();

  /**
   * Ends tracking an individual method generator invocation.
   *
   * @param generatorName the simple class name of the method generator that was started
   */
  void endGenerator(String generatorName);

  /**
   * Starts tracking an individual builder enhancer invocation.
   *
   * <p>The enhancer name is passed to {@link #endEnhancer(String)} for recording; this method only
   * records the start timestamp.
   */
  void startEnhancer();

  /**
   * Ends tracking an individual builder enhancer invocation.
   *
   * @param enhancerName the simple class name of the builder enhancer that was started
   */
  void endEnhancer(String enhancerName);

  /**
   * Records the start of processing for a specific class.
   *
   * <p>Call this before any work begins for the class. Field and collection counts are not yet
   * known at this point; they are passed to {@link #endClass(int, int)} after extraction.
   *
   * @param className the simple name of the class being processed
   */
  void startClass(String className);

  /**
   * Records the end of processing for the current class.
   *
   * @param fieldCount the number of fields in the class
   * @param collectionCount the number of collection-type fields
   */
  void endClass(int fieldCount, int collectionCount);

  /**
   * Generates and logs the performance report.
   *
   * @param logger the processing logger to output the report
   */
  void generateReport(ProcessingLogger logger);
}
