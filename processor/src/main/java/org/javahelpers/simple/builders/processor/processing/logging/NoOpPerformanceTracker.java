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
 * No-operation implementation of {@link PerformanceTracker} that does nothing.
 *
 * <p>All methods are empty, so the JIT compiler can eliminate them entirely when the tracker is
 * fixed at construction time. This ensures zero overhead when performance tracking is disabled.
 */
public final class NoOpPerformanceTracker implements PerformanceTracker {

  @Override
  public void startPhase() {
    // No-op
  }

  @Override
  public void endPhase(String phase) {
    // No-op
  }

  @Override
  public void startGenerator() {
    // No-op
  }

  @Override
  public void endGenerator(String generatorName) {
    // No-op
  }

  @Override
  public void startEnhancer() {
    // No-op
  }

  @Override
  public void endEnhancer(String enhancerName) {
    // No-op
  }

  @Override
  public void startClass(String className) {
    // No-op
  }

  @Override
  public void endClass(int fieldCount, int collectionCount) {
    // No-op
  }

  @Override
  public void generateReport(ProcessingLogger logger) {
    // No-op
  }
}
