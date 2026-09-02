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

import static com.google.testing.compile.CompilationSubject.assertThat;

import com.google.testing.compile.Compilation;
import org.javahelpers.simple.builders.processor.testing.ProcessorAsserts;
import org.javahelpers.simple.builders.processor.testing.ProcessorTestUtils;
import org.junit.jupiter.api.Test;

/** Compile-testing coverage for the resolver's builder availability decisions. */
class BuilderScopeResolverTest {

  @Test
  void usageScopeWithoutResolvedBuilderFallsBackToPlainSetter() {
    Compilation compilation =
        ProcessorTestUtils.createCompiler()
            .withOptions(
                "-Asimplebuilder.builderGenerationPackages=test",
                "-Asimplebuilder.builderUsagePackages=lib")
            .compile(
                ProcessorTestUtils.forSource(
                    """
                    package test;
                    import lib.LibHelper;
                    import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
                    @SimpleBuilder
                    public class ResolverDto {
                      private LibHelper helper;
                      public LibHelper getHelper() { return helper; }
                      public void setHelper(LibHelper helper) { this.helper = helper; }
                    }
                    """),
                ProcessorTestUtils.forSource(
                    """
                    package lib;
                    import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
                    @SimpleBuilder
                    public class LibHelper { public LibHelper() {} }
                    """));

    assertThat(compilation).succeeded();
    String generated = ProcessorTestUtils.loadGeneratedSource(compilation, "ResolverDtoBuilder");
    ProcessorAsserts.assertContaining(generated, "helper(LibHelper helper)");
    ProcessorAsserts.assertNotContaining(generated, "helperBuilderConsumer", "LibHelperBuilder");
  }
}
