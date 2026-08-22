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

import java.util.Set;
import org.javahelpers.simple.builders.processor.model.core.BuilderConfiguration;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link BuilderConfiguration} package scope helpers. */
class BuilderConfigurationScopeTest {

  @Test
  void emptyScope_isUnscoped() {
    BuilderConfiguration config = BuilderConfiguration.DEFAULT;

    assertTrue(config.isInGenerationScope("com.example"), "Empty generation scope is unscoped");
    assertTrue(config.isInUsageScope("com.example"), "Empty usage scope is unscoped");
    assertFalse(
        config.isPackageInGenerationScope("com.example"), "Empty generation set matches nothing");
    assertFalse(config.isPackageInUsageScope("com.example"), "Empty usage set matches nothing");
  }

  @Test
  void packageSet_parsesCommaSeparatedListWithTrimming() {
    BuilderConfiguration config =
        BuilderConfiguration.builder()
            .builderGenerationPackages(" a , b.c , com.example.nested ")
            .builderUsagePackages("com.library, com.library.sub ")
            .build();

    assertEquals(
        Set.of("a", "b.c", "com.example.nested"), config.getBuilderGenerationPackagesSet());
    assertEquals(Set.of("com.library", "com.library.sub"), config.getBuilderUsagePackagesSet());
  }

  @Test
  void packageSet_treatsBlankAsEmpty() {
    BuilderConfiguration config =
        BuilderConfiguration.builder()
            .builderGenerationPackages("   ")
            .builderUsagePackages("")
            .build();

    assertTrue(config.getBuilderGenerationPackagesSet().isEmpty());
    assertTrue(config.getBuilderUsagePackagesSet().isEmpty());
  }

  @Test
  void scopeMatches_exactPackageAndSubpackages() {
    BuilderConfiguration config =
        BuilderConfiguration.builder().builderGenerationPackages("com.example").build();

    assertTrue(config.isInGenerationScope("com.example"));
    assertTrue(config.isInGenerationScope("com.example.sub"));
    assertTrue(config.isInGenerationScope("com.example.sub.deep"));
    assertFalse(config.isInGenerationScope("com.exampleother"));
    assertFalse(config.isInGenerationScope("com.other"));
    assertFalse(config.isInGenerationScope(""));
  }

  @Test
  void usageScope_matchesIndependently() {
    BuilderConfiguration config =
        BuilderConfiguration.builder()
            .builderGenerationPackages("com.example")
            .builderUsagePackages("com.library")
            .build();

    assertTrue(config.isInUsageScope("com.library"));
    assertTrue(config.isInUsageScope("com.library.sub"));
    assertFalse(config.isInUsageScope("com.example"));
  }
}
