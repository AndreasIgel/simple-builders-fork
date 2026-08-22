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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import javax.tools.JavaFileObject;
import org.javahelpers.simple.builders.processor.testing.ProcessorAsserts;
import org.javahelpers.simple.builders.processor.testing.ProcessorTestUtils;
import org.junit.jupiter.api.Test;

/** Integration tests for builder generation and usage package scoping (issue #114). */
class BuilderScopeProcessorTest {

  private static final String NESTED_DTO_BODY =
      """
      private String value;
      public String getValue() { return value; }
      public void setValue(String value) { this.value = value; }
      """;

  private static final String LIBRARY_DTO_BODY =
      """
      private String data;
      public String getData() { return data; }
      public void setData(String data) { this.data = data; }
      """;

  private static final String LIBRARY_BUILDER_SOURCE =
      """
      package com.library;
      public class LibraryDtoBuilder {
          private LibraryDto instance;
          public LibraryDtoBuilder() {}
          public LibraryDtoBuilder(LibraryDto instance) { this.instance = instance; }
          public LibraryDtoBuilder data(String data) {
              if (instance == null) instance = new LibraryDto();
              instance.setData(data);
              return this;
          }
          public LibraryDto build() { return instance; }
      }
      """;

  /** (a) Both options unset: nested in-compilation DTO still gets its builder consumer. */
  @Test
  void bothOptionsUnset_nestedInCompilationDto_referencedAsBuilder() {
    JavaFileObject nested =
        ProcessorTestUtils.simpleBuilderClass("com.example", "NestedDto", NESTED_DTO_BODY);
    JavaFileObject parent =
        ProcessorTestUtils.simpleBuilderClass(
            "com.example",
            "ParentDto",
            """
            private NestedDto nested;
            public NestedDto getNested() { return nested; }
            public void setNested(NestedDto nested) { this.nested = nested; }
            """);

    Compilation compilation = ProcessorTestUtils.createCompiler().compile(nested, parent);

    assertThat(compilation).succeededWithoutWarnings();
    String parentCode = ProcessorTestUtils.loadGeneratedSource(compilation, "ParentDtoBuilder");
    ProcessorAsserts.assertContaining(
        parentCode,
        "public ParentDtoBuilder nested(Consumer<NestedDtoBuilder> nestedBuilderConsumer)",
        "new NestedDtoBuilder(this.nested.value())",
        "new NestedDtoBuilder()");
  }

  /** (b) Generation scope includes the DTO package and a subpackage: generation and reference. */
  @Test
  void builderGenerationPackages_includesPackageAndSubpackage_bothGeneratedAndReferenced() {
    JavaFileObject nested =
        ProcessorTestUtils.simpleBuilderClass("com.example.nested", "NestedDto", NESTED_DTO_BODY);
    JavaFileObject parent =
        ProcessorTestUtils.forSource(
            """
            package com.example;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public class ParentDto {
                private com.example.nested.NestedDto nested;
                public com.example.nested.NestedDto getNested() { return nested; }
                public void setNested(com.example.nested.NestedDto nested) { this.nested = nested; }
            }
            """);

    Compilation compilation =
        compilerWithOptions("-Asimplebuilder.builderGenerationPackages=com.example")
            .compile(nested, parent);

    assertThat(compilation).succeededWithoutWarnings();
    // NestedDto is in a subpackage of com.example, so its builder must be generated.
    String nestedCode = ProcessorTestUtils.loadGeneratedSource(compilation, "NestedDtoBuilder");
    assertNotNull(nestedCode, "NestedDtoBuilder should be generated");
    assertTrue(nestedCode.contains("class NestedDtoBuilder"));
    String parentCode = ProcessorTestUtils.loadGeneratedSource(compilation, "ParentDtoBuilder");
    ProcessorAsserts.assertContaining(
        parentCode,
        "public ParentDtoBuilder nested(Consumer<NestedDtoBuilder> nestedBuilderConsumer)",
        "new NestedDtoBuilder(this.nested.value())");
  }

  /** (b) Generation scope excludes the nested DTO package: no builder and no reference. */
  @Test
  void builderGenerationPackages_excludesNestedPackage_fallsBackToSetter() {
    JavaFileObject nested =
        ProcessorTestUtils.simpleBuilderClass("com.other", "NestedDto", NESTED_DTO_BODY);
    JavaFileObject parent =
        ProcessorTestUtils.forSource(
            """
            package com.example;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public class ParentDto {
                private com.other.NestedDto nested;
                public com.other.NestedDto getNested() { return nested; }
                public void setNested(com.other.NestedDto nested) { this.nested = nested; }
            }
            """);

    Compilation compilation =
        compilerWithOptions("-Asimplebuilder.builderGenerationPackages=com.example")
            .compile(nested, parent);

    assertThat(compilation).succeededWithoutWarnings();
    ProcessorAsserts.assertNoBuilderGenerated(
        compilation, "NestedDto", "NestedDtoBuilder should not be generated");
    String parentCode = ProcessorTestUtils.loadGeneratedSource(compilation, "ParentDtoBuilder");
    ProcessorAsserts.assertNotContaining(
        parentCode, "Consumer<NestedDtoBuilder>", "new NestedDtoBuilder");
    ProcessorAsserts.assertContaining(
        parentCode, "public ParentDtoBuilder nested(NestedDto nested)");
  }

  /** (c) Usage scope includes a package whose compiled builder exists: referenced. */
  @Test
  void builderUsagePackages_existingBuilder_referenced() {
    JavaFileObject libraryDto =
        ProcessorTestUtils.simpleBuilderClass("com.library", "LibraryDto", LIBRARY_DTO_BODY);
    JavaFileObject libraryBuilder = ProcessorTestUtils.forSource(LIBRARY_BUILDER_SOURCE);
    JavaFileObject parent =
        ProcessorTestUtils.forSource(
            """
            package com.example;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public class ParentDto {
                private com.library.LibraryDto library;
                public com.library.LibraryDto getLibrary() { return library; }
                public void setLibrary(com.library.LibraryDto library) { this.library = library; }
            }
            """);

    Compilation compilation =
        compilerWithOptions(
                "-Asimplebuilder.builderGenerationPackages=com.example",
                "-Asimplebuilder.builderUsagePackages=com.library")
            .compile(libraryDto, libraryBuilder, parent);

    assertThat(compilation).succeededWithoutWarnings();
    ProcessorAsserts.assertNoBuilderGenerated(
        compilation, "LibraryDto", "LibraryDtoBuilder should not be generated by the processor");
    String parentCode = ProcessorTestUtils.loadGeneratedSource(compilation, "ParentDtoBuilder");
    ProcessorAsserts.assertContaining(
        parentCode,
        "public ParentDtoBuilder library(Consumer<LibraryDtoBuilder> libraryBuilderConsumer)",
        "new LibraryDtoBuilder(this.library.value())",
        "new LibraryDtoBuilder()");
  }

  /** (c) Usage scope includes a package whose builder does NOT exist: no broken reference. */
  @Test
  void builderUsagePackages_missingBuilder_noReference() {
    JavaFileObject libraryDto =
        ProcessorTestUtils.simpleBuilderClass("com.library", "LibraryDto", LIBRARY_DTO_BODY);
    JavaFileObject parent =
        ProcessorTestUtils.forSource(
            """
            package com.example;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public class ParentDto {
                private com.library.LibraryDto library;
                public com.library.LibraryDto getLibrary() { return library; }
                public void setLibrary(com.library.LibraryDto library) { this.library = library; }
            }
            """);

    Compilation compilation =
        compilerWithOptions(
                "-Asimplebuilder.builderGenerationPackages=com.example",
                "-Asimplebuilder.builderUsagePackages=com.library")
            .compile(libraryDto, parent);

    assertThat(compilation).succeededWithoutWarnings();
    String parentCode = ProcessorTestUtils.loadGeneratedSource(compilation, "ParentDtoBuilder");
    ProcessorAsserts.assertNotContaining(
        parentCode, "Consumer<LibraryDtoBuilder>", "new LibraryDtoBuilder");
    ProcessorAsserts.assertContaining(
        parentCode, "public ParentDtoBuilder library(LibraryDto library)");
  }

  /** (d) Opt-out precedence: an @Ignore4BuilderGeneration DTO is never referenced. */
  @Test
  void ignoreAnnotation_takesPrecedenceOverPackageScope() {
    JavaFileObject ignoredDto =
        ProcessorTestUtils.forSource(
            """
            package com.library;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
            import org.javahelpers.simple.builders.core.annotations.Ignore4BuilderGeneration;

            @SimpleBuilder
            @Ignore4BuilderGeneration
            public class IgnoredDto {
                private String value;
                public String getValue() { return value; }
                public void setValue(String value) { this.value = value; }
            }
            """);
    JavaFileObject parent =
        ProcessorTestUtils.forSource(
            """
            package com.example;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public class ParentDto {
                private com.library.IgnoredDto ignored;
                public com.library.IgnoredDto getIgnored() { return ignored; }
                public void setIgnored(com.library.IgnoredDto ignored) { this.ignored = ignored; }
            }
            """);

    Compilation compilation =
        compilerWithOptions(
                "-Asimplebuilder.builderGenerationPackages=com.example",
                "-Asimplebuilder.builderUsagePackages=com.library")
            .compile(ignoredDto, parent);

    assertThat(compilation).succeededWithoutWarnings();
    ProcessorAsserts.assertNoBuilderGenerated(
        compilation, "IgnoredDto", "IgnoredDtoBuilder should not be generated");
    String parentCode = ProcessorTestUtils.loadGeneratedSource(compilation, "ParentDtoBuilder");
    ProcessorAsserts.assertNotContaining(
        parentCode, "Consumer<IgnoredDtoBuilder>", "new IgnoredDtoBuilder");
    ProcessorAsserts.assertContaining(
        parentCode, "public ParentDtoBuilder ignored(IgnoredDto ignored)");
  }

  /** (e) Compiler-arg and inline @SimpleBuilder.Options are both parsed. */
  @Test
  void compilerArgAndInlineOptions_bothParsedAndApplied() {
    JavaFileObject libraryDto =
        ProcessorTestUtils.simpleBuilderClass("com.library", "LibraryDto", LIBRARY_DTO_BODY);
    JavaFileObject libraryBuilder = ProcessorTestUtils.forSource(LIBRARY_BUILDER_SOURCE);
    JavaFileObject parent =
        ProcessorTestUtils.forSource(
            """
            package com.example;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder(options = @SimpleBuilder.Options(
                builderUsagePackages = "com.library"
            ))
            public class ParentDto {
                private com.library.LibraryDto library;
                public com.library.LibraryDto getLibrary() { return library; }
                public void setLibrary(com.library.LibraryDto library) { this.library = library; }
            }
            """);

    Compilation compilation =
        compilerWithOptions("-Asimplebuilder.builderGenerationPackages=com.example")
            .compile(libraryDto, libraryBuilder, parent);

    assertThat(compilation).succeededWithoutWarnings();
    String parentCode = ProcessorTestUtils.loadGeneratedSource(compilation, "ParentDtoBuilder");
    ProcessorAsserts.assertContaining(
        parentCode,
        "public ParentDtoBuilder library(Consumer<LibraryDtoBuilder> libraryBuilderConsumer)",
        "new LibraryDtoBuilder(this.library.value())");
  }

  /**
   * Empty builderGenerationPackages with a non-empty builderUsagePackages must not block references
   * to local nested builders that the processor will generate in the same round.
   */
  @Test
  void builderUsagePackagesOnly_localNestedBuilderInOtherPackage_referenced() {
    JavaFileObject nested =
        ProcessorTestUtils.simpleBuilderClass("com.other", "NestedDto", NESTED_DTO_BODY);
    JavaFileObject parent =
        ProcessorTestUtils.forSource(
            """
            package com.example;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public class ParentDto {
                private com.other.NestedDto nested;
                public com.other.NestedDto getNested() { return nested; }
                public void setNested(com.other.NestedDto nested) { this.nested = nested; }
            }
            """);

    Compilation compilation =
        compilerWithOptions("-Asimplebuilder.builderUsagePackages=com.other")
            .compile(nested, parent);

    assertThat(compilation).succeededWithoutWarnings();
    String nestedCode = ProcessorTestUtils.loadGeneratedSource(compilation, "NestedDtoBuilder");
    assertNotNull(nestedCode, "NestedDtoBuilder should be generated");
    assertTrue(nestedCode.contains("class NestedDtoBuilder"));

    String parentCode = ProcessorTestUtils.loadGeneratedSource(compilation, "ParentDtoBuilder");
    ProcessorAsserts.assertContaining(
        parentCode,
        "public ParentDtoBuilder nested(Consumer<NestedDtoBuilder> nestedBuilderConsumer)",
        "new NestedDtoBuilder(this.nested.value())");
  }

  /**
   * A per-class builderGenerationPackages override must not cause references to builders that are
   * not actually generated because the global generation scope excludes their package.
   */
  @Test
  void perClassGenerationPackagesOverride_doesNotReferenceNonGeneratedBuilder() {
    JavaFileObject nested =
        ProcessorTestUtils.simpleBuilderClass("com.other", "NestedDto", NESTED_DTO_BODY);
    JavaFileObject parent =
        ProcessorTestUtils.forSource(
            """
            package com.example;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder(options = @SimpleBuilder.Options(
                builderGenerationPackages = "com.other"
            ))
            public class ParentDto {
                private com.other.NestedDto nested;
                public com.other.NestedDto getNested() { return nested; }
                public void setNested(com.other.NestedDto nested) { this.nested = nested; }
            }
            """);

    Compilation compilation =
        compilerWithOptions("-Asimplebuilder.builderGenerationPackages=com.example")
            .compile(nested, parent);

    assertThat(compilation).succeededWithoutWarnings();
    ProcessorAsserts.assertNoBuilderGenerated(
        compilation, "NestedDto", "NestedDtoBuilder should not be generated");
    String parentCode = ProcessorTestUtils.loadGeneratedSource(compilation, "ParentDtoBuilder");
    ProcessorAsserts.assertNotContaining(
        parentCode, "Consumer<NestedDtoBuilder>", "new NestedDtoBuilder");
    ProcessorAsserts.assertContaining(
        parentCode, "public ParentDtoBuilder nested(NestedDto nested)");
  }

  private static Compiler compilerWithOptions(String... options) {
    return ProcessorTestUtils.createCompiler().withOptions(options);
  }
}
