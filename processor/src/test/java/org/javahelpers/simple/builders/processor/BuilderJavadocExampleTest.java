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

import static org.javahelpers.simple.builders.processor.testing.ProcessorAsserts.assertGenerationSucceeded;
import static org.javahelpers.simple.builders.processor.testing.ProcessorTestUtils.loadGeneratedSource;

import com.google.testing.compile.Compilation;
import java.util.stream.Stream;
import javax.tools.JavaFileObject;
import org.javahelpers.simple.builders.processor.testing.ProcessorAsserts;
import org.javahelpers.simple.builders.processor.testing.ProcessorTestUtils;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests that verify the field-specific code examples added to the generated builder javadoc.
 *
 * <p>Two levels are covered:
 *
 * <ul>
 *   <li>Class-level javadoc: a single "kitchen-sink" chain that invokes every generated method
 *       (basic setter + supplier + collection helpers + list consumer + ...) on sample values so
 *       users see the full feature catalogue at a glance.
 *   <li>Method-level javadoc: each generated method has its own, method-specific example block
 *       showing exactly how to invoke that single method.
 * </ul>
 *
 * <p>Assertions use exact javadoc text blocks (whitespace-normalized) so both the presence and the
 * relative ordering of lines is verified in a single substring match.
 */
class BuilderJavadocExampleTest {

  protected Compilation compile(JavaFileObject... sourceFiles) {
    return ProcessorTestUtils.createCompiler()
        .withOptions("-Asimplebuilder.generateMapperHelpers=DISABLED")
        .compile(sourceFiles);
  }

  @Test
  void shouldGenerateClassJavadocExampleWithKitchenSinkChain() {
    // Given: a DTO with fields covering basic setter, supplier and list helpers
    String packageName = "test";
    String className = "BookDto";
    String builderClassName = className + "Builder";

    JavaFileObject dto =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private String title;
                private int pages;
                private java.util.List<String> tags;

                public String getTitle() { return title; }
                public void setTitle(String title) { this.title = title; }
                public int getPages() { return pages; }
                public void setPages(int pages) { this.pages = pages; }
                public java.util.List<String> getTags() { return tags; }
                public void setTags(java.util.List<String> tags) { this.tags = tags; }
            """);

    // When
    Compilation compilation = compile(dto);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // The generated class javadoc must contain the full kitchen-sink chain,
    // with fields in alphabetical order (pages, tags, title) and within each
    // field the generator lines in priority order.
    ProcessorAsserts.assertContaining(
        generatedCode,
        """
        * <pre>{@code
        * BookDto result = BookDtoBuilder.create()
        *     .pages(42)
        *     .pages(() -> 42)
        *     .tags(List.of("example value"))
        *     .tags(() -> List.of("example value"))
        *     .tags(t -> t.add("example value"))
        *     .tags("example value", "example value")
        *     .add2Tags("example value")
        *     .title("example value")
        *     .title("Hello %s", "World")
        *     .title(() -> "example value")
        *     .title(sb -> sb.append("text"))
        *     .build();
        * }</pre>
        """);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("methodJavadocExampleCases")
  void shouldGenerateMethodJavadocExample(String className, String source, String expectedJavadoc) {
    String builderClassName = className + "Builder";
    JavaFileObject dto = ProcessorTestUtils.simpleBuilderClass("test", className, source);

    Compilation compilation = compile(dto);
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    ProcessorAsserts.assertContaining(generatedCode, expectedJavadoc);
  }

  private static Stream<Arguments> methodJavadocExampleCases() {
    return Stream.of(
        Arguments.of(
            Named.of("basic string setter", "Person"),
            """
                private String teamname;
                public String getTeamname() { return teamname; }
                public void setTeamname(String teamname) { this.teamname = teamname; }
            """,
            """
            * Sets the value for <code>teamname</code>.
            * <p>
            * Generated from setter {@link Person#setTeamname(String) setTeamname(String teamname)}
            *
            * <h4>Example:</h4>
            *
            * <pre>{@code
            * builder.teamname("example value");
            * }</pre>
            *
            * @param teamname teamname
            * @return current instance of builder
            */
            public PersonBuilder teamname(String teamname)
            """),
        Arguments.of(
            Named.of("primitive setter", "Counter"),
            """
                private int amount;
                public int getAmount() { return amount; }
                public void setAmount(int amount) { this.amount = amount; }
            """,
            """
            * Sets the value for <code>amount</code>.
            * <p>
            * Generated from setter {@link Counter#setAmount(int) setAmount(int amount)}
            *
            * <h4>Example:</h4>
            *
            * <pre>{@code
            * builder.amount(42);
            * }</pre>
            *
            * @param amount amount
            * @return current instance of builder
            */
            public CounterBuilder amount(int amount)
            """),
        Arguments.of(
            Named.of("supplier", "SupplierDto"),
            """
                private String title;
                public String getTitle() { return title; }
                public void setTitle(String title) { this.title = title; }
            """,
            """
            * <h4>Example:</h4>
            *
            * <pre>{@code
            * builder.title(() -> "example value");
            * }</pre>
            """),
        Arguments.of(
            Named.of("add to collection", "TagsDto"),
            """
                private java.util.List<String> tags;
                public java.util.List<String> getTags() { return tags; }
                public void setTags(java.util.List<String> tags) { this.tags = tags; }
            """,
            """
            * Adds a single element to <code>tags</code>.
            * <p>
            * Generated from setter {@link TagsDto#setTags(List) setTags(List<String> tags)}
            *
            * <h4>Example:</h4>
            *
            * <pre>{@code
            * builder.add2Tags("example value");
            * }</pre>
            *
            * @param element the element to add
            * @return current instance of builder
            */
            public TagsDtoBuilder add2Tags(String element)
            """),
        Arguments.of(
            Named.of("list consumer", "ListDto"),
            """
                private java.util.List<String> tags;
                public java.util.List<String> getTags() { return tags; }
                public void setTags(java.util.List<String> tags) { this.tags = tags; }
            """,
            """
            * <h4>Example:</h4>
            *
            * <pre>{@code
            * builder.tags(t -> t.add("example value"));
            * }</pre>
            """),
        Arguments.of(
            Named.of("create", "CreateDto"),
            """
                private String name;
                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
            """,
            """
            * <h4>Example:</h4>
            *
            * <pre>{@code
            * CreateDtoBuilder builder = CreateDtoBuilder.create();
            * }</pre>
            """),
        Arguments.of(
            Named.of("build", "BuildDto"),
            """
                private String name;
                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
            """,
            """
            * <h4>Example:</h4>
            *
            * <pre>{@code
            * BuildDto result = builder.build();
            * }</pre>
            """),
        Arguments.of(
            Named.of("map setter", "MapDto"),
            """
                private java.util.Map<String, Integer> values;
                public java.util.Map<String, Integer> getValues() { return values; }
                public void setValues(java.util.Map<String, Integer> values) { this.values = values; }
            """,
            """
            * <h4>Example:</h4>
            *
            * <pre>{@code
            * builder.values(Map.of("example value", 42));
            * }</pre>
            """),
        Arguments.of(
            Named.of("integer-key map setter", "IntegerKeyMapDto"),
            """
                private java.util.Map<Integer, String> values;
                public java.util.Map<Integer, String> getValues() { return values; }
                public void setValues(java.util.Map<Integer, String> values) { this.values = values; }
            """,
            """
            * <h4>Example:</h4>
            *
            * <pre>{@code
            * builder.values(Map.of(42, "example value"));
            * }</pre>
            """),
        Arguments.of(
            Named.of("optional setter", "OptionalDto"),
            """
                private java.util.Optional<String> subtitle;
                public java.util.Optional<String> getSubtitle() { return subtitle; }
                public void setSubtitle(java.util.Optional<String> subtitle) { this.subtitle = subtitle; }
            """,
            """
            * <h4>Example:</h4>
            *
            * <pre>{@code
            * builder.subtitle(Optional.of("example value"));
            * }</pre>
            """));
  }

  // ---------------------------------------------------------------------------
  // Negative cases: no example emitted when placeholder value is unresolvable
  // and empty code blocks must not be rendered at all.
  // ---------------------------------------------------------------------------

  @Test
  void shouldOmitExampleBlockWhenFieldTypeHasNoDefaultValue() {
    // Given: field with a reference type that has no entry in JavadocExampleValues
    // and no @SimpleBuilder on the helper type -> no example value available.
    String packageName = "test";
    String className = "UnknownTypeDto";
    String builderClassName = className + "Builder";

    JavaFileObject dto =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private HelperPlain helper;
                public HelperPlain getHelper() { return helper; }
                public void setHelper(HelperPlain helper) { this.helper = helper; }
            """);

    JavaFileObject helper =
        ProcessorTestUtils.forSource(
            """
                package test;
                public class HelperPlain { public HelperPlain() {} }
            """);

    Compilation compilation = compile(dto, helper);
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // The basic setter must still be generated,
    // but the method javadoc must NOT contain any example block:
    // neither a bogus "builder.helper(null)" line
    // nor an empty "<pre>{@code ... }</pre>" block.
    ProcessorAsserts.assertContaining(generatedCode, "public UnknownTypeDtoBuilder helper(");
    ProcessorAsserts.assertNotContaining(
        generatedCode,
        // no example line falling back to `null`
        "builder.helper(null)",
        // no empty example block either
        "<pre>{@code\n}</pre>",
        "<pre>{@code }</pre>",
        "<pre>{@code}</pre>");
  }

  @Test
  void shouldOmitClassExampleLineForUnresolvableField() {
    // Given: a DTO with both a resolvable field and an unresolvable field.
    // The class-level kitchen-sink chain must include ONLY the resolvable field.
    String packageName = "test";
    String className = "MixedDto";
    String builderClassName = className + "Builder";

    JavaFileObject dto =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private String title;
                private HelperPlain helper;
                public String getTitle() { return title; }
                public void setTitle(String title) { this.title = title; }
                public HelperPlain getHelper() { return helper; }
                public void setHelper(HelperPlain helper) { this.helper = helper; }
            """);

    JavaFileObject helper =
        ProcessorTestUtils.forSource(
            """
                package test;
                public class HelperPlain { public HelperPlain(String arg) {} }
            """);

    Compilation compilation = compile(dto, helper);
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // The class-level kitchen-sink chain includes ONLY the resolvable field (title).
    // The helper field (HelperPlain) has no example value and must be omitted.
    // HelperPlain has only a parameterized constructor (no empty constructor) and no builder.
    ProcessorAsserts.assertContaining(
        generatedCode,
        """
        * <pre>{@code
        * MixedDto result = MixedDtoBuilder.create()
        *     .title("example value")
        *     .title("Hello %s", "World")
        *     .title(() -> "example value")
        *     .title(sb -> sb.append("text"))
        *     .build();
        * }</pre>
        """);

    // No `.helper(...)` call in the class example chain
    ProcessorAsserts.assertNotContaining(generatedCode, ".helper(");
  }

  @Test
  void shouldNotRenderEmptyJavadocExampleBlocks() {
    // Sanity guard: no generated method should ever contain an empty example block.
    String packageName = "test";
    String className = "NoEmptyDto";
    String builderClassName = className + "Builder";

    JavaFileObject dto =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private String name;
                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
            """);

    Compilation compilation = compile(dto);
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // Degenerate empty blocks must never appear.
    ProcessorAsserts.assertNotContaining(
        generatedCode, "<pre>{@code\n}</pre>", "<pre>{@code }</pre>", "<pre>{@code}</pre>");
  }
}
