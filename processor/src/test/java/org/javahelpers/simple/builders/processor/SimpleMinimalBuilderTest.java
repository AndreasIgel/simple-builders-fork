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
import static org.javahelpers.simple.builders.processor.testing.ProcessorAsserts.assertNotContaining;
import static org.javahelpers.simple.builders.processor.testing.ProcessorTestUtils.loadGeneratedSource;

import com.google.testing.compile.Compilation;
import javax.tools.JavaFileObject;
import org.javahelpers.simple.builders.processor.testing.ProcessorAsserts;
import org.javahelpers.simple.builders.processor.testing.ProcessorTestUtils;
import org.junit.jupiter.api.Test;

/**
 * Tests for the built-in {@code @SimpleMinimalBuilder} template annotation.
 *
 * <p>Verifies that {@code @SimpleMinimalBuilder} disables all optional features and produces a
 * builder with only the basic fluent API: constructors, {@code create()}, field setters, {@code
 * build()} and {@code toString()}.
 */
class SimpleMinimalBuilderTest {

  @Test
  void simpleMinimalBuilderGeneratesMinimalBuilder() {
    JavaFileObject source =
        ProcessorTestUtils.forSource(
            """
            package test;
            import org.javahelpers.simple.builders.core.annotations.SimpleMinimalBuilder;

            @SimpleMinimalBuilder
            public class PersonDto {
                private String name;
                private java.util.List<String> tags;

                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
                public java.util.List<String> getTags() { return tags; }
                public void setTags(java.util.List<String> tags) { this.tags = tags; }
            }
            """);

    Compilation compilation = ProcessorTestUtils.createCompiler().compile(source);

    assertThat(compilation).succeededWithoutWarnings();

    String generatedCode = loadGeneratedSource(compilation, "PersonDtoBuilder");
    assertGenerationSucceeded(compilation, "PersonDtoBuilder", generatedCode);

    String expectedCode =
        """
        package test;

        import static org.javahelpers.simple.builders.core.util.TrackedValue.changedValue;
        import static org.javahelpers.simple.builders.core.util.TrackedValue.initialValue;
        import static org.javahelpers.simple.builders.core.util.TrackedValue.unsetValue;
        import java.util.List;
        import org.apache.commons.lang3.builder.ToStringBuilder;
        import org.javahelpers.simple.builders.core.util.BuilderToStringStyle;
        import org.javahelpers.simple.builders.core.util.TrackedValue;

        public class PersonDtoBuilder {

          private TrackedValue<String> name = unsetValue();

          private TrackedValue<List<String>> tags = unsetValue();

          public PersonDtoBuilder() {
          }

          public PersonDtoBuilder(PersonDto instance) {
            this.name = initialValue(instance.getName());
            this.tags = initialValue(instance.getTags());
          }

          public static PersonDtoBuilder create() {
            return new PersonDtoBuilder();
          }

          public PersonDtoBuilder name(String name) {
            this.name = changedValue(name);
            return this;
          }

          public PersonDtoBuilder tags(List<String> tags) {
            this.tags = changedValue(tags);
            return this;
          }

          public PersonDto build() {
            PersonDto result = new PersonDto();
            this.name.ifSet(result::setName);
            this.tags.ifSet(result::setTags);
            return result;
          }

          @Override
          public String toString() {
            return new ToStringBuilder(this, BuilderToStringStyle.INSTANCE).append("name", this.name)
                .append("tags", this.tags)
                .toString();
          }
        }
        """;

    ProcessorAsserts.assertNormalizedEquals(
        expectedCode,
        generatedCode,
        "Generated minimal builder does not match expected. All optional features should be disabled.");
  }

  @Test
  void inheritedSimpleMinimalBuilderPropagatesToSubclass() {
    JavaFileObject parentSource =
        ProcessorTestUtils.forSource(
            """
            package test;
            import org.javahelpers.simple.builders.core.annotations.SimpleMinimalBuilder;

            @SimpleMinimalBuilder
            public class PersonDto {
                private String name;

                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
            }
            """);

    JavaFileObject childSource =
        ProcessorTestUtils.forSource(
            """
            package test;

            public class ChildDto extends PersonDto {
                private int age;

                public int getAge() { return age; }
                public void setAge(int age) { this.age = age; }
            }
            """);

    Compilation compilation =
        ProcessorTestUtils.createCompiler().compile(parentSource, childSource);

    assertThat(compilation).succeededWithoutWarnings();

    String parentBuilder = loadGeneratedSource(compilation, "PersonDtoBuilder");
    assertGenerationSucceeded(compilation, "PersonDtoBuilder", parentBuilder);

    String childBuilder = loadGeneratedSource(compilation, "ChildDtoBuilder");
    assertGenerationSucceeded(compilation, "ChildDtoBuilder", childBuilder);

    assertContaining(childBuilder, "public ChildDtoBuilder name(String name)");
    assertContaining(childBuilder, "public ChildDtoBuilder age(int age)");

    assertNotContaining(
        childBuilder,
        "Supplier<",
        "Consumer<",
        "WithChildDto",
        "@Generated",
        "@BuilderImplementation",
        "IBuilderBase",
        "@JsonPOJOBuilder",
        "ChildDtoBuilder name(String... name)",
        "ChildDtoBuilder tags(String... tags)");
  }
}
