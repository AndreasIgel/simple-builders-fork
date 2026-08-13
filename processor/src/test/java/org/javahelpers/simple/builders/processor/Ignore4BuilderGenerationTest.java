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
import static org.javahelpers.simple.builders.processor.testing.ProcessorAsserts.assertContaining;
import static org.javahelpers.simple.builders.processor.testing.ProcessorAsserts.assertGenerationSucceeded;
import static org.javahelpers.simple.builders.processor.testing.ProcessorAsserts.contains;
import static org.javahelpers.simple.builders.processor.testing.ProcessorAsserts.notContains;
import static org.javahelpers.simple.builders.processor.testing.ProcessorTestUtils.loadGeneratedSource;

import com.google.testing.compile.Compilation;
import javax.tools.JavaFileObject;
import org.javahelpers.simple.builders.processor.testing.ProcessorAsserts;
import org.javahelpers.simple.builders.processor.testing.ProcessorTestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Tests for the {@code @Ignore4BuilderGeneration} opt-out annotation. */
class Ignore4BuilderGenerationTest {

  private Compilation compile(JavaFileObject... sourceFiles) {
    return ProcessorTestUtils.createCompiler().compile(sourceFiles);
  }

  /**
   * Asserts that no generated source file for the given class builder exists.
   *
   * @param compilation the compilation result
   * @param className the simple class name for which no builder should have been generated
   * @param message the failure message
   */
  private void assertNoBuilderGenerated(Compilation compilation, String className, String message) {
    Assertions.assertTrue(
        compilation.generatedSourceFiles().stream()
            .noneMatch(f -> f.getName().endsWith(className + "Builder.java")),
        message);
  }

  /**
   * A plain {@code @SimpleBuilder} annotation on a parent is <b>not</b> inherited by an unannotated
   * child class. The processor only discovers inherited annotations through the compiler's
   * {@code @Inherited} mechanism (which {@code @SimpleBuilder} does not have).
   */
  @Test
  void simpleBuilderAnnotationIsNotInherited() {
    JavaFileObject parentSource =
        ProcessorTestUtils.forSource(
            """
            package test;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public class ParentWithSimpleBuilder {
                private String name;
                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
            }
            """);

    JavaFileObject childSource =
        ProcessorTestUtils.forSource(
            """
            package test;
            public class ChildOfSimpleBuilder extends ParentWithSimpleBuilder { }
            """);

    Compilation compilation = compile(parentSource, childSource);

    assertThat(compilation).succeededWithoutWarnings();

    String parentBuilder = loadGeneratedSource(compilation, "ParentWithSimpleBuilderBuilder");
    assertGenerationSucceeded(compilation, "ParentWithSimpleBuilderBuilder", parentBuilder);

    assertNoBuilderGenerated(
        compilation,
        "ChildOfSimpleBuilder",
        "A plain @SimpleBuilder annotation on a parent must not be inherited by subclasses");
  }

  /**
   * A custom annotation that is meta-annotated with {@code @SimpleBuilder.Template} and
   * {@code @Inherited} <b>is</b> inherited by an unannotated child class, because the annotation
   * processor discovers it through {@code RoundEnvironment#getElementsAnnotatedWith}.
   */
  @Test
  void templateAnnotationIsInherited() {
    JavaFileObject templateAnnotation =
        ProcessorTestUtils.forSource(
            """
            package test;

            import java.lang.annotation.ElementType;
            import java.lang.annotation.Inherited;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder.Template(options = @SimpleBuilder.Options())
            @Inherited
            @Retention(RetentionPolicy.CLASS)
            @Target(ElementType.TYPE)
            public @interface InheritedTemplateCheck {}
            """);

    JavaFileObject parentSource =
        ProcessorTestUtils.forSource(
            """
            package test;
            @InheritedTemplateCheck
            public class ParentWithTemplate {
                private String name;
                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
            }
            """);

    JavaFileObject childSource =
        ProcessorTestUtils.forSource(
            """
            package test;
            public class ChildOfTemplate extends ParentWithTemplate { }
            """);

    Compilation compilation = compile(templateAnnotation, parentSource, childSource);

    assertThat(compilation).succeededWithoutWarnings();

    String parentBuilder = loadGeneratedSource(compilation, "ParentWithTemplateBuilder");
    assertGenerationSucceeded(compilation, "ParentWithTemplateBuilder", parentBuilder);

    String childBuilder = loadGeneratedSource(compilation, "ChildOfTemplateBuilder");
    assertGenerationSucceeded(compilation, "ChildOfTemplateBuilder", childBuilder);
  }

  /**
   * (a) A subclass that inherits a template annotation from its parent and is annotated with
   * {@code @Ignore4BuilderGeneration} must NOT get a builder, while the parent's builder is still
   * generated.
   */
  @Test
  void subclassWithInheritedTemplateAndOptOutGeneratesParentButNotChild() {
    JavaFileObject templateAnnotation =
        ProcessorTestUtils.forSource(
            """
            package test;

            import java.lang.annotation.ElementType;
            import java.lang.annotation.Inherited;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder.Template(options = @SimpleBuilder.Options())
            @Inherited
            @Retention(RetentionPolicy.CLASS)
            @Target(ElementType.TYPE)
            public @interface InheritedTemplate {}
            """);

    JavaFileObject parentSource =
        ProcessorTestUtils.forSource(
            """
            package test;

            @InheritedTemplate
            public class ParentDto {
                private String name;

                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
            }
            """);

    JavaFileObject childSource =
        ProcessorTestUtils.forSource(
            """
            package test;

            import org.javahelpers.simple.builders.core.annotations.Ignore4BuilderGeneration;

            @Ignore4BuilderGeneration
            public class ChildDto extends ParentDto { }
            """);

    Compilation compilation = compile(templateAnnotation, parentSource, childSource);

    assertThat(compilation).succeededWithoutWarnings();

    String parentBuilder = loadGeneratedSource(compilation, "ParentDtoBuilder");
    assertGenerationSucceeded(compilation, "ParentDtoBuilder", parentBuilder);
    assertContaining(parentBuilder, "public ParentDtoBuilder name(String name)");

    assertNoBuilderGenerated(
        compilation,
        "ChildDto",
        "ChildDto builder should not have been generated due to @Ignore4BuilderGeneration");
  }

  /**
   * (b) A DTO field whose type is annotated with {@code @Ignore4BuilderGeneration} must not trigger
   * nested-builder consumer generation in the referencing DTO. The referencing builder falls back
   * to a plain setter and must not reference a non-existent builder class.
   */
  @Test
  void referencedOptedOutDtoFallsBackToPlainSetter() {
    JavaFileObject ignoredDto =
        ProcessorTestUtils.forSource(
            """
            package test;

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

    JavaFileObject containerDto =
        ProcessorTestUtils.forSource(
            """
            package test;

            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public class ContainerDto {
                private IgnoredDto ignored;

                public IgnoredDto getIgnored() { return ignored; }
                public void setIgnored(IgnoredDto ignored) { this.ignored = ignored; }
            }
            """);

    Compilation compilation = compile(ignoredDto, containerDto);

    assertThat(compilation).succeededWithoutWarnings();

    assertNoBuilderGenerated(
        compilation, "IgnoredDto", "IgnoredDto builder should not have been generated");

    String containerBuilder = loadGeneratedSource(compilation, "ContainerDtoBuilder");
    assertGenerationSucceeded(compilation, "ContainerDtoBuilder", containerBuilder);

    ProcessorAsserts.assertingResult(
        containerBuilder,
        contains("public ContainerDtoBuilder ignored(IgnoredDto ignored)"),
        notContains("IgnoredDtoBuilder"));
  }

  /**
   * (c) Regression: a normally annotated DTO without the opt-out still generates its builder and is
   * still referenced by other DTOs.
   */
  @Test
  void normalDtoStillGeneratesBuilderAndIsReferencedByOthers() {
    JavaFileObject normalDto =
        ProcessorTestUtils.simpleBuilderClass(
            "test",
            "NormalDto",
            """
            private String value;

            public String getValue() { return value; }
            public void setValue(String value) { this.value = value; }
            """);

    JavaFileObject containerDto =
        ProcessorTestUtils.forSource(
            """
            package test;

            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public class ReferenceDto {
                private NormalDto normal;

                public NormalDto getNormal() { return normal; }
                public void setNormal(NormalDto normal) { this.normal = normal; }
            }
            """);

    Compilation compilation = compile(normalDto, containerDto);

    assertThat(compilation).succeededWithoutWarnings();

    String normalBuilder = loadGeneratedSource(compilation, "NormalDtoBuilder");
    assertGenerationSucceeded(compilation, "NormalDtoBuilder", normalBuilder);

    String referenceBuilder = loadGeneratedSource(compilation, "ReferenceDtoBuilder");
    assertGenerationSucceeded(compilation, "ReferenceDtoBuilder", referenceBuilder);
    assertContaining(
        referenceBuilder, "public ReferenceDtoBuilder normal(Consumer<NormalDtoBuilder>");
  }
}
