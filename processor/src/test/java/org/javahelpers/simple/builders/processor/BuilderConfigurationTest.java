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

package org.javahelpers.simple.builders.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.javahelpers.simple.builders.processor.model.core.BuilderConfiguration;
import org.junit.jupiter.api.Test;

/** Unit tests for package scope configuration parsing and matching. */
class BuilderConfigurationTest {

  @Test
  void packageScopes_SplitAndTrimValues() {
    BuilderConfiguration config =
        BuilderConfiguration.builder()
            .builderGenerationPackages("a.b , c.d")
            .builderUsagePackages("x.y, z.w")
            .build();

    assertEquals(java.util.Set.of("a.b", "c.d"), config.getBuilderGenerationPackagesSet());
    assertEquals(java.util.Set.of("x.y", "z.w"), config.getBuilderUsagePackagesSet());
  }

  @Test
  void blankScopesAreUnscoped() {
    BuilderConfiguration config =
        BuilderConfiguration.builder()
            .builderGenerationPackages("  ")
            .builderUsagePackages(null)
            .build();

    assertTrue(config.getBuilderGenerationPackagesSet().isEmpty());
    assertTrue(config.getBuilderUsagePackagesSet().isEmpty());
    assertFalse(config.isInGenerationScope("anything"));
    assertFalse(config.isInUsageScope("anything"));
  }

  @Test
  void packageScopesMatchExactAndSubpackagesOnly() {
    BuilderConfiguration config =
        BuilderConfiguration.builder()
            .builderGenerationPackages("a.b")
            .builderUsagePackages("c.d")
            .build();

    assertTrue(config.isInGenerationScope("a.b"));
    assertTrue(config.isInGenerationScope("a.b.child"));
    assertFalse(config.isInGenerationScope("a.bc"));
    assertFalse(config.isInGenerationScope("c.d"));
    assertTrue(config.isInUsageScope("c.d"));
    assertTrue(config.isInUsageScope("c.d.child"));
    assertFalse(config.isInUsageScope("c.de"));
  }
}
