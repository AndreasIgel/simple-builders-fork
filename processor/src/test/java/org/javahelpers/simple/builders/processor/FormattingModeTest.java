/*
 * MIT License
 *
 * Copyright (c) 2026 Andreas Igel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation (the "Software"), to deal
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
import static org.javahelpers.simple.builders.processor.testing.ProcessorTestUtils.loadGeneratedSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.testing.compile.Compilation;
import javax.tools.JavaFileObject;
import org.javahelpers.simple.builders.processor.testing.ProcessorTestUtils;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@code formattingMode} compiler option.
 *
 * <p>Verifies:
 *
 * <ul>
 *   <li>{@code lightweight} mode: generated code compiles, tab indentation is converted to 2-space,
 *       javadoc body lines have proper {@code " * "} prefixes, imports are not concatenated with
 *       javadoc, consecutive blank lines are collapsed
 *   <li>Default (JDT) mode: generated code matches Eclipse-formatted output
 * </ul>
 */
class FormattingModeTest {

  @Test
  void lightweightFormatting_producesValidCodeWithLightweightFormatting() {
    JavaFileObject sourceFile =
        ProcessorTestUtils.forSource(
            """
            package test;

            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public class FormatTestDto {
              private String name;
              private int count;

              public FormatTestDto(String name, int count) {
                this.name = name;
                this.count = count;
              }

              public String getName() {
                return name;
              }

              public int getCount() {
                return count;
              }
            }
            """);

    Compilation compilation =
        ProcessorTestUtils.createCompiler()
            .withOptions(
                "-Asimplebuilder.formattingMode=lightweight",
                "-Asimplebuilder.generateMapperHelpers=DISABLED")
            .compile(sourceFile);

    assertThat(compilation).succeeded();

    String generatedCode = loadGeneratedSource(compilation, "FormatTestDtoBuilder");

    // Normalize trailing whitespace per line for text block comparison (lightweight formatter
    // may leave trailing spaces on blank javadoc lines, but text blocks strip them)
    generatedCode =
        java.util.Arrays.stream(generatedCode.split("\n", -1))
            .map(String::stripTrailing)
            .collect(java.util.stream.Collectors.joining("\n"));

    // Full-file comparison using a text block.
    // The 0-indented lines (package, imports, class declaration) anchor the common prefix
    // so that all relative indentation (2-space, 4-space, etc.) is preserved correctly.
    String expectedCode =
        """
        package test;

        import static org.javahelpers.simple.builders.core.util.TrackedValue.changedValue;
        import static org.javahelpers.simple.builders.core.util.TrackedValue.initialValue;
        import static org.javahelpers.simple.builders.core.util.TrackedValue.unsetValue;
        import java.util.function.BooleanSupplier;
        import java.util.function.Consumer;
        import java.util.function.Supplier;
        import javax.annotation.processing.Generated;
        import org.apache.commons.lang3.builder.ToStringBuilder;
        import org.javahelpers.simple.builders.core.annotations.BuilderImplementation;
        import org.javahelpers.simple.builders.core.interfaces.IBuilderBase;
        import org.javahelpers.simple.builders.core.util.BuilderToStringStyle;
        import org.javahelpers.simple.builders.core.util.TrackedValue;
        /**
         * Builder for {@code test.FormatTestDto}.
         * <p>
         * This builder provides a fluent API for creating instances of test.FormatTestDto with
         * method chaining and validation. Use the static {@code create()} method
         * to obtain a new builder instance, configure the desired properties using
         * the setter methods, and then call {@code build()} to create the final DTO.
         *
         * <h4>Example:</h4><pre>{@code
         * FormatTestDto result = FormatTestDtoBuilder.create()
         *     .name("example value")
         *     .name("Hello %s", "World")
         *     .name(() -> "example value")
         *     .name(sb -> sb.append("text"))
         *     .count(42)
         *     .count(() -> 42)
         *     .build();
         * }</pre>
         */
        @Generated("Generated by org.javahelpers.simple.builders.processor.BuilderProcessor")
        @BuilderImplementation(forClass = FormatTestDto.class)
        public class FormatTestDtoBuilder implements IBuilderBase<FormatTestDto> {

          /**
           * Tracked value for <code>name</code>: name.
           */
          private TrackedValue<String> name = unsetValue();
          /**
           * Tracked value for <code>count</code>: count.
           */
          private TrackedValue<Integer> count = unsetValue();

          /**
           * Empty constructor of builder for {@code test.FormatTestDto}.
           */
          public FormatTestDtoBuilder() {
          }

          /**
           * Initialisation of builder for {@code test.FormatTestDto} by a instance.
           * @param instance object instance for initialisiation
           */
          public FormatTestDtoBuilder(FormatTestDto instance) {
            this.name = initialValue(instance.getName());
            this.count = initialValue(instance.getCount());
          }

          /**
           * Creating a new builder for {@code test.FormatTestDto}.
           *
           * <h4>Example:</h4><pre>{@code
           * FormatTestDtoBuilder builder = FormatTestDtoBuilder.create();
           * }</pre>
           * @return builder for {@code test.FormatTestDto}
           */
          public static FormatTestDtoBuilder create() {
            return new FormatTestDtoBuilder();
          }

          /**
           * Sets the value for <code>count</code>.
           * <p>Generated from parameter in constructor {@link FormatTestDto#FormatTestDto(String, int) FormatTestDto(String name, int count)}
           *
           * <h4>Example:</h4><pre>{@code
           * builder.count(42);
           * }</pre>
           * @param count count
           * @return current instance of builder
           */
          public FormatTestDtoBuilder count(int count) {
            this.count = changedValue(count);
            return this;
          }

          /**
           * Sets the value for <code>count</code> by invoking the provided supplier.
           * <p>Generated from parameter in constructor {@link FormatTestDto#FormatTestDto(String, int) FormatTestDto(String name, int count)}
           *
           * <h4>Example:</h4><pre>{@code
           * builder.count(() -> 42);
           * }</pre>
           * @param countSupplier supplier for count
           * @return current instance of builder
           */
          public FormatTestDtoBuilder count(Supplier<Integer> countSupplier) {
            this.count = changedValue(countSupplier.get());
            return this;
          }

          /**
           * Sets the value for <code>name</code>.
           * <p>Generated from parameter in constructor {@link FormatTestDto#FormatTestDto(String, int) FormatTestDto(String name, int count)}
           *
           * <h4>Example:</h4><pre>{@code
           * builder.name("example value");
           * }</pre>
           * @param name name
           * @return current instance of builder
           */
          public FormatTestDtoBuilder name(String name) {
            this.name = changedValue(name);
            return this;
          }

          /**
           * Sets the value for <code>name</code> by executing the provided consumer.
           * <p>Generated from parameter in constructor {@link FormatTestDto#FormatTestDto(String, int) FormatTestDto(String name, int count)}
           *
           * <h4>Example:</h4><pre>{@code
           * builder.name(sb -> sb.append("text"));
           * }</pre>
           * @param nameStringBuilderConsumer consumer providing an instance of name
           * @return current instance of builder
           */
          public FormatTestDtoBuilder name(Consumer<StringBuilder> nameStringBuilderConsumer) {
            StringBuilder builder = new StringBuilder();
            nameStringBuilderConsumer.accept(builder);
            this.name = changedValue(builder.toString());
            return this;
          }

          /**
           * Sets the value for <code>name</code> by invoking the provided supplier.
           * <p>Generated from parameter in constructor {@link FormatTestDto#FormatTestDto(String, int) FormatTestDto(String name, int count)}
           *
           * <h4>Example:</h4><pre>{@code
           * builder.name(() -> "example value");
           * }</pre>
           * @param nameSupplier supplier for name
           * @return current instance of builder
           */
          public FormatTestDtoBuilder name(Supplier<String> nameSupplier) {
            this.name = changedValue(nameSupplier.get());
            return this;
          }

          /**
           * Sets the String value for <code>name</code> by using String.format(format, args).
           * See {@link String#format(String, Object...)} for details.
           * <p>Generated from parameter in constructor {@link FormatTestDto#FormatTestDto(String, int) FormatTestDto(String name, int count)}
           *
           * <h4>Example:</h4><pre>{@code
           * builder.name("Hello %s", "World");
           * }</pre>
           * @param format A format string
           * @param args Arguments referenced by the format specifiers in the format string.
           * @return current instance of builder
           */
          public FormatTestDtoBuilder name(String format, Object... args) {
            this.name = changedValue(String.format(format, args));
            return this;
          }

          /**
           * Conditionally applies builder modifications if the condition is true.
           * @param condition the condition to evaluate
           * @param yesCondition the consumer to apply if condition is true
           * @return this builder instance
           */
          public FormatTestDtoBuilder conditional(BooleanSupplier condition, Consumer<FormatTestDtoBuilder> yesCondition) {
            return conditional(condition, yesCondition, null);
          }

          /**
           * Conditionally applies builder modifications based on a condition evaluation.
           * @param condition the condition to evaluate
           * @param trueCase the consumer to apply if condition is true
           * @param falseCase the consumer to apply if condition is false (can be null)
           * @return this builder instance
           */
          public FormatTestDtoBuilder conditional(BooleanSupplier condition, Consumer<FormatTestDtoBuilder> trueCase,
              Consumer<FormatTestDtoBuilder> falseCase) {
            if (condition.getAsBoolean()) {
              trueCase.accept(this);
            } else if (falseCase != null) {
              falseCase.accept(this);
            }
            return this;
          }

          /**
           * Builds the configured DTO instance.
           *
           * <h4>Example:</h4><pre>{@code
           * FormatTestDto result = builder.build();
           * }</pre>
           */
          @Override
          public FormatTestDto build() {
            if (!this.count.isSet()) {
              throw new IllegalStateException("Required field 'count' must be set before calling build()");
            }
            if (this.count.value() == null) {
              throw new IllegalStateException("Field 'count' is marked as non-null but null value was provided");
            }
            FormatTestDto result = new FormatTestDto(this.name.value(), this.count.value());
            return result;
          }

          /**
           * Returns a string representation of this builder, including only fields that have been set.
           * @return string representation of the builder
           */
          @Override
          public String toString() {
            return new ToStringBuilder(this, BuilderToStringStyle.INSTANCE).append("name", this.name)
                .append("count", this.count).toString();
          }

          /**
           * Interface that can be implemented by the DTO to provide fluent modification methods.
           */
          public interface With {
            /**
            * Initializes a builder from an instance of this class, using methods of this builder to change values and returns the new built object.
            * @param b the consumer to apply modifications
            * @return the modified instance
            */
            default FormatTestDto with(Consumer<FormatTestDtoBuilder> b) {
              FormatTestDtoBuilder builder;
              try {
                builder = new FormatTestDtoBuilder(FormatTestDto.class.cast(this));
              } catch (ClassCastException ex) {
                throw new IllegalArgumentException(
                    "The interface 'FormatTestDtoBuilder.With' should only be implemented by classes, which could be casted to 'FormatTestDto'",
                    ex);
              }
              b.accept(builder);
              return builder.build();
            }

            /**
            * Creates a builder initialized from this instance.
            * @return a builder initialized with this instance's values
            */
            default FormatTestDtoBuilder with() {
              try {
                return new FormatTestDtoBuilder(FormatTestDto.class.cast(this));
              } catch (ClassCastException ex) {
                throw new IllegalArgumentException(
                    "The interface 'FormatTestDtoBuilder.With' should only be implemented by classes, which could be casted to 'FormatTestDto'",
                    ex);
              }
            }
          }
        }""";
    assertEquals(
        expectedCode,
        generatedCode,
        "Generated code with lightweight formatting should match the expected lightweight-formatted output");
  }

  @Test
  void jdtFormattingByDefault_usesEclipseFormatter() {
    JavaFileObject sourceFile =
        ProcessorTestUtils.forSource(
            """
            package test;

            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public class DefaultFormatDto {
              private String value;

              public DefaultFormatDto(String value) {
                this.value = value;
              }

              public String getValue() {
                return value;
              }
            }
            """);

    Compilation compilation =
        ProcessorTestUtils.createCompiler()
            .withOptions("-Asimplebuilder.generateMapperHelpers=DISABLED")
            .compile(sourceFile);

    assertThat(compilation).succeeded();

    String generatedCode = loadGeneratedSource(compilation, "DefaultFormatDtoBuilder");

    // Normalize trailing whitespace per line for text block comparison
    generatedCode =
        java.util.Arrays.stream(generatedCode.split("\n", -1))
            .map(String::stripTrailing)
            .collect(java.util.stream.Collectors.joining("\n"));

    String[] lines = generatedCode.split("\n", -1);

    // Section 1: package + imports + class-level javadoc + class declaration
    String section1 = String.join("\n", java.util.Arrays.copyOfRange(lines, 0, 36));
    String expectedSection1 =
        """
        package test;

        import static org.javahelpers.simple.builders.core.util.TrackedValue.changedValue;
        import static org.javahelpers.simple.builders.core.util.TrackedValue.initialValue;
        import static org.javahelpers.simple.builders.core.util.TrackedValue.unsetValue;
        import java.util.function.BooleanSupplier;
        import java.util.function.Consumer;
        import java.util.function.Supplier;
        import javax.annotation.processing.Generated;
        import org.apache.commons.lang3.builder.ToStringBuilder;
        import org.javahelpers.simple.builders.core.annotations.BuilderImplementation;
        import org.javahelpers.simple.builders.core.interfaces.IBuilderBase;
        import org.javahelpers.simple.builders.core.util.BuilderToStringStyle;
        import org.javahelpers.simple.builders.core.util.TrackedValue;

        /**
         * Builder for {@code test.DefaultFormatDto}.
         * <p>
         * This builder provides a fluent API for creating instances of test.DefaultFormatDto with method chaining and
         * validation. Use the static {@code create()} method to obtain a new builder instance, configure the desired properties
         * using the setter methods, and then call {@code build()} to create the final DTO.
         *
         * <h4>Example:</h4>
         *
         * <pre>{@code
         * DefaultFormatDto result = DefaultFormatDtoBuilder.create()
         *     .value("example value")
         *     .value("Hello %s", "World")
         *     .value(() -> "example value")
         *     .value(sb -> sb.append("text"))
         *     .build();
         * }</pre>
         */
        @Generated("Generated by org.javahelpers.simple.builders.processor.BuilderProcessor")
        @BuilderImplementation(forClass = DefaultFormatDto.class)
        public class DefaultFormatDtoBuilder implements IBuilderBase<DefaultFormatDto> {""";
    assertEquals(
        expectedSection1,
        section1,
        "Eclipse-formatted section 1 (package/imports/class javadoc/declaration) mismatch");

    // Section 2: class body — the closing brace at 0 indentation anchors the common prefix
    String section2 = String.join("\n", java.util.Arrays.copyOfRange(lines, 36, lines.length));
    String expectedSection2 =
        """

          /**
           * Tracked value for <code>value</code>: value.
           */
          private TrackedValue<String> value = unsetValue();

          /**
           * Empty constructor of builder for {@code test.DefaultFormatDto}.
           */
          public DefaultFormatDtoBuilder() {
          }

          /**
           * Initialisation of builder for {@code test.DefaultFormatDto} by a instance.
           *
           * @param instance object instance for initialisiation
           */
          public DefaultFormatDtoBuilder(DefaultFormatDto instance) {
            this.value = initialValue(instance.getValue());
          }

          /**
           * Creating a new builder for {@code test.DefaultFormatDto}.
           *
           * <h4>Example:</h4>
           *
           * <pre>{@code
           * DefaultFormatDtoBuilder builder = DefaultFormatDtoBuilder.create();
           * }</pre>
           *
           * @return builder for {@code test.DefaultFormatDto}
           */
          public static DefaultFormatDtoBuilder create() {
            return new DefaultFormatDtoBuilder();
          }

          /**
           * Sets the value for <code>value</code>.
           * <p>
           * Generated from parameter in constructor {@link DefaultFormatDto#DefaultFormatDto(String) DefaultFormatDto(String
           * value)}
           *
           * <h4>Example:</h4>
           *
           * <pre>{@code
           * builder.value("example value");
           * }</pre>
           *
           * @param value value
           * @return current instance of builder
           */
          public DefaultFormatDtoBuilder value(String value) {
            this.value = changedValue(value);
            return this;
          }

          /**
           * Sets the value for <code>value</code> by executing the provided consumer.
           * <p>
           * Generated from parameter in constructor {@link DefaultFormatDto#DefaultFormatDto(String) DefaultFormatDto(String
           * value)}
           *
           * <h4>Example:</h4>
           *
           * <pre>{@code
           * builder.value(sb -> sb.append("text"));
           * }</pre>
           *
           * @param valueStringBuilderConsumer consumer providing an instance of value
           * @return current instance of builder
           */
          public DefaultFormatDtoBuilder value(Consumer<StringBuilder> valueStringBuilderConsumer) {
            StringBuilder builder = new StringBuilder();
            valueStringBuilderConsumer.accept(builder);
            this.value = changedValue(builder.toString());
            return this;
          }

          /**
           * Sets the value for <code>value</code> by invoking the provided supplier.
           * <p>
           * Generated from parameter in constructor {@link DefaultFormatDto#DefaultFormatDto(String) DefaultFormatDto(String
           * value)}
           *
           * <h4>Example:</h4>
           *
           * <pre>{@code
           * builder.value(() -> "example value");
           * }</pre>
           *
           * @param valueSupplier supplier for value
           * @return current instance of builder
           */
          public DefaultFormatDtoBuilder value(Supplier<String> valueSupplier) {
            this.value = changedValue(valueSupplier.get());
            return this;
          }

          /**
           * Sets the String value for <code>value</code> by using String.format(format, args). See
           * {@link String#format(String, Object...)} for details.
           * <p>
           * Generated from parameter in constructor {@link DefaultFormatDto#DefaultFormatDto(String) DefaultFormatDto(String
           * value)}
           *
           * <h4>Example:</h4>
           *
           * <pre>{@code
           * builder.value("Hello %s", "World");
           * }</pre>
           *
           * @param format A format string
           * @param args Arguments referenced by the format specifiers in the format string.
           * @return current instance of builder
           */
          public DefaultFormatDtoBuilder value(String format, Object... args) {
            this.value = changedValue(String.format(format, args));
            return this;
          }

          /**
           * Conditionally applies builder modifications if the condition is true.
           *
           * @param condition the condition to evaluate
           * @param yesCondition the consumer to apply if condition is true
           * @return this builder instance
           */
          public DefaultFormatDtoBuilder conditional(BooleanSupplier condition,
              Consumer<DefaultFormatDtoBuilder> yesCondition) {
            return conditional(condition, yesCondition, null);
          }

          /**
           * Conditionally applies builder modifications based on a condition evaluation.
           *
           * @param condition the condition to evaluate
           * @param trueCase the consumer to apply if condition is true
           * @param falseCase the consumer to apply if condition is false (can be null)
           * @return this builder instance
           */
          public DefaultFormatDtoBuilder conditional(BooleanSupplier condition, Consumer<DefaultFormatDtoBuilder> trueCase,
              Consumer<DefaultFormatDtoBuilder> falseCase) {
            if (condition.getAsBoolean()) {
              trueCase.accept(this);
            } else if (falseCase != null) {
              falseCase.accept(this);
            }
            return this;
          }

          /**
           * Builds the configured DTO instance.
           *
           * <h4>Example:</h4>
           *
           * <pre>{@code
           * DefaultFormatDto result = builder.build();
           * }</pre>
           */
          @Override
          public DefaultFormatDto build() {
            DefaultFormatDto result = new DefaultFormatDto(this.value.value());
            return result;
          }

          /**
           * Returns a string representation of this builder, including only fields that have been set.
           *
           * @return string representation of the builder
           */
          @Override
          public String toString() {
            return new ToStringBuilder(this, BuilderToStringStyle.INSTANCE).append("value", this.value).toString();
          }

          /**
           * Interface that can be implemented by the DTO to provide fluent modification methods.
           */
          public interface With {
            /**
             * Initializes a builder from an instance of this class, using methods of this builder to change values and returns
             * the new built object.
             *
             * @param b the consumer to apply modifications
             * @return the modified instance
             */
            default DefaultFormatDto with(Consumer<DefaultFormatDtoBuilder> b) {
              DefaultFormatDtoBuilder builder;
              try {
                builder = new DefaultFormatDtoBuilder(DefaultFormatDto.class.cast(this));
              } catch (ClassCastException ex) {
                throw new IllegalArgumentException(
                    "The interface 'DefaultFormatDtoBuilder.With' should only be implemented by classes, which could be casted to 'DefaultFormatDto'",
                    ex);
              }
              b.accept(builder);
              return builder.build();
            }

            /**
             * Creates a builder initialized from this instance.
             *
             * @return a builder initialized with this instance's values
             */
            default DefaultFormatDtoBuilder with() {
              try {
                return new DefaultFormatDtoBuilder(DefaultFormatDto.class.cast(this));
              } catch (ClassCastException ex) {
                throw new IllegalArgumentException(
                    "The interface 'DefaultFormatDtoBuilder.With' should only be implemented by classes, which could be casted to 'DefaultFormatDto'",
                    ex);
              }
            }
          }
        }""";
    assertEquals(expectedSection2, section2, "Eclipse-formatted section 2 (class body) mismatch");
  }
}
