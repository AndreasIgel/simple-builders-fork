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
import javax.tools.JavaFileObject;
import org.javahelpers.simple.builders.processor.testing.ProcessorAsserts;
import org.javahelpers.simple.builders.processor.testing.ProcessorTestUtils;
import org.junit.jupiter.api.Test;

/** Compile-testing coverage for builder generation and usage package scopes. */
class BuilderScopeProcessingTest {

  @Test
  void bothScopesUnset_UsesAnnotatedInCompilationHelperBuilder() {
    Compilation compilation =
        ProcessorTestUtils.createCompiler()
            .compile(dto("test", "ScopeDto", "HelperAnno"), helper("test"));

    assertThat(compilation).succeeded();
    assertBuilderConsumer(compilation, "ScopeDtoBuilder", "HelperAnnoBuilder");
    ProcessorAsserts.assertContaining(
        ProcessorTestUtils.loadGeneratedSource(compilation, "ScopeDtoBuilder"),
        "public ScopeDtoBuilder helper(HelperAnno helper)");
  }

  @Test
  void generationScope_IncludesExactPackageAndSubpackages() {
    Compilation exact =
        ProcessorTestUtils.createCompiler()
            .withOptions("-Asimplebuilder.builderGenerationPackages=test")
            .compile(dto("test", "ExactDto", "HelperAnno"), helper("test"));
    Compilation subpackage =
        ProcessorTestUtils.createCompiler()
            .withOptions("-Asimplebuilder.builderGenerationPackages=test")
            .compile(dto("test.sub", "SubDto", "HelperAnno"), helper("test.sub"));

    assertThat(exact).succeeded();
    assertThat(subpackage).succeeded();
    assertBuilderConsumer(exact, "ExactDtoBuilder", "HelperAnnoBuilder");
    assertBuilderConsumer(subpackage, "SubDtoBuilder", "HelperAnnoBuilder");
  }

  @Test
  void generationScope_ExcludesDtoOutsideScope() {
    Compilation compilation =
        ProcessorTestUtils.createCompiler()
            .withOptions("-Asimplebuilder.builderGenerationPackages=other.pkg")
            .compile(dto("test", "OutOfScopeDto", "HelperAnno"), helper("test"));

    assertThat(compilation).succeeded();
    ProcessorAsserts.assertNoBuilderGenerated(
        compilation, "OutOfScopeDto", "An out-of-scope DTO must not get a builder");
  }

  @Test
  void usageScope_RequiresExistingPrecompiledBuilder() {
    JavaFileObject dto = dto("test", "LibraryUsageDto", "LibHelper", "lib");
    JavaFileObject helper = helper("lib", "LibHelper");

    Compilation compilation =
        ProcessorTestUtils.createCompiler()
            .withOptions(
                "-Asimplebuilder.builderGenerationPackages=test",
                "-Asimplebuilder.builderUsagePackages=lib")
            .compile(dto, helper);

    assertThat(compilation).succeeded();
    String generated =
        ProcessorTestUtils.loadGeneratedSource(compilation, "LibraryUsageDtoBuilder");
    ProcessorAsserts.assertContaining(
        generated, "public LibraryUsageDtoBuilder helper(LibHelper helper)");
    ProcessorAsserts.assertNotContaining(generated, "helperBuilderConsumer", "LibHelperBuilder");
    ProcessorAsserts.assertNoBuilderGenerated(
        compilation, "LibHelper", "The library helper must not be generated in this compilation");
  }

  @Test
  void usageScope_ReferencesBuilderWhenGenerationScopeAlsoIncludesLibrary() {
    Compilation compilation =
        ProcessorTestUtils.createCompiler()
            .withOptions(
                "-Asimplebuilder.builderGenerationPackages=test,lib",
                "-Asimplebuilder.builderUsagePackages=lib")
            .compile(
                dto("test", "LibraryUsageDto", "LibHelper", "lib"), helper("lib", "LibHelper"));

    assertThat(compilation).succeeded();
    assertBuilderConsumer(compilation, "LibraryUsageDtoBuilder", "LibHelperBuilder");
    ProcessorAsserts.assertContaining(
        ProcessorTestUtils.loadGeneratedSource(compilation, "LibHelperBuilder"),
        "public LibHelper build()");
  }

  @Test
  void inlineOptions_ConfigureBuilderScopes() {
    JavaFileObject dto =
        ProcessorTestUtils.forSource(
            """
            package test;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder(options = @SimpleBuilder.Options(
                builderGenerationPackages = "test",
                builderUsagePackages = "test"
            ))
            public class InlineScopeDto {
              private HelperAnno helper;
              public HelperAnno getHelper() { return helper; }
              public void setHelper(HelperAnno helper) { this.helper = helper; }
            }
            """);

    Compilation compilation = ProcessorTestUtils.createCompiler().compile(dto, helper("test"));

    assertThat(compilation).succeeded();
    assertBuilderConsumer(compilation, "InlineScopeDtoBuilder", "HelperAnnoBuilder");
  }

  @Test
  void optOutTakesPrecedenceOverScopes() {
    Compilation ignored =
        ProcessorTestUtils.createCompiler()
            .withOptions(
                "-Asimplebuilder.builderGenerationPackages=test",
                "-Asimplebuilder.builderUsagePackages=test")
            .compile(dto("test", "IgnoredHelperDto", "IgnoredHelper"), ignoredHelper());
    Compilation unannotated =
        ProcessorTestUtils.createCompiler()
            .withOptions(
                "-Asimplebuilder.builderGenerationPackages=test",
                "-Asimplebuilder.builderUsagePackages=test")
            .compile(dto("test", "PlainHelperDto", "PlainHelper"), plainHelper());

    assertThat(ignored).succeeded();
    assertThat(unannotated).succeeded();
    assertNoBuilderConsumer(
        ignored, "IgnoredHelperDtoBuilder", "IgnoredHelper", "IgnoredHelperBuilder");
    assertNoBuilderConsumer(
        unannotated, "PlainHelperDtoBuilder", "PlainHelper", "PlainHelperBuilder");
  }

  private static JavaFileObject dto(String packageName, String className, String helperType) {
    return dto(packageName, className, helperType, packageName);
  }

  private static JavaFileObject dto(
      String packageName, String className, String helperType, String helperPackage) {
    String importLine =
        packageName.equals(helperPackage)
            ? ""
            : "import " + helperPackage + "." + helperType + ";\n";
    return ProcessorTestUtils.forSource(
        """
        package %s;
        import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
        %s
        @SimpleBuilder
        public class %s {
          private %s helper;
          public %s getHelper() { return helper; }
          public void setHelper(%s helper) { this.helper = helper; }
        }
        """
            .formatted(packageName, importLine, className, helperType, helperType, helperType));
  }

  private static JavaFileObject helper(String packageName) {
    return helper(packageName, "HelperAnno");
  }

  private static JavaFileObject helper(String packageName, String className) {
    return ProcessorTestUtils.forSource(
        """
        package %s;
        import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
        @SimpleBuilder
        public class %s { public %s() {} }
        """
            .formatted(packageName, className, className));
  }

  private static JavaFileObject ignoredHelper() {
    return ProcessorTestUtils.forSource(
        """
        package test;
        import org.javahelpers.simple.builders.core.annotations.Ignore4BuilderGeneration;
        import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
        @SimpleBuilder
        @Ignore4BuilderGeneration
        public class IgnoredHelper { public IgnoredHelper() {} }
        """);
  }

  private static JavaFileObject plainHelper() {
    return ProcessorTestUtils.forSource(
        """
        package test;
        public class PlainHelper { public PlainHelper() {} }
        """);
  }

  private static void assertBuilderConsumer(
      Compilation compilation, String builderName, String helperBuilderName) {
    String generated = ProcessorTestUtils.loadGeneratedSource(compilation, builderName);
    ProcessorAsserts.assertContaining(
        generated,
        "helperBuilderConsumer",
        helperBuilderName + " builder",
        "helperBuilderConsumer.accept(builder)");
  }

  private static void assertNoBuilderConsumer(
      Compilation compilation,
      String builderName,
      String helperTypeName,
      String helperBuilderName) {
    String generated = ProcessorTestUtils.loadGeneratedSource(compilation, builderName);
    ProcessorAsserts.assertContaining(generated, "helper(" + helperTypeName + " helper)");
    ProcessorAsserts.assertNotContaining(generated, "helperBuilderConsumer", helperBuilderName);
  }
}
