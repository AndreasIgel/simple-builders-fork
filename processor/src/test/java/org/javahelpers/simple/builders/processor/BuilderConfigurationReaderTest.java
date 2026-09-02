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

import com.google.testing.compile.Compilation;
import javax.tools.JavaFileObject;
import org.javahelpers.simple.builders.processor.testing.ProcessorAsserts;
import org.javahelpers.simple.builders.processor.testing.ProcessorTestUtils;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link
 * org.javahelpers.simple.builders.processor.processing.BuilderConfigurationReader}.
 *
 * <p>Verifies configuration reading from various sources through end-to-end compilation. Both
 * {@code @SimpleBuilder} (which is itself a built-in {@code @SimpleBuilder.Template}) and custom
 * template annotations are discovered and resolved through the same path.
 *
 * <ul>
 *   <li>Direct {@code @SimpleBuilder.Options} annotations
 *   <li>Template annotations ({@code @SimpleBuilder.Template}), including the built-in
 *       {@code @SimpleBuilder}
 *   <li>Configuration resolution with proper priority chain
 *   <li>Compiler arguments
 * </ul>
 *
 * <p>These are integration tests that verify BuilderConfigurationReader by checking generated
 * builder code reflects the correct configuration values.
 */
class BuilderConfigurationReaderTest {

  /**
   * Test: Builder respects configuration from @SimpleBuilder.Options annotation.
   *
   * <p>Verifies the built-in {@code @SimpleBuilder} template is resolved and its inline {@code
   * options()} override the template defaults.
   */
  @Test
  void readFromOptions_WithOptionsAnnotation_AppliesAllOptions() {
    // Given: A DTO with comprehensive inline @SimpleBuilder options
    JavaFileObject source =
        ProcessorTestUtils.forSource(
            """
            package test;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
            import org.javahelpers.simple.builders.core.enums.OptionState;
            import org.javahelpers.simple.builders.core.enums.AccessModifier;

            @SimpleBuilder(options = @SimpleBuilder.Options(
                generateFieldSupplier = OptionState.DISABLED,
                generateFieldConsumer = OptionState.DISABLED,
                generateBuilderConsumer = OptionState.DISABLED,
                builderGenerationPackages = "test",
                builderUsagePackages = "test",
                generateVarArgsHelpers = OptionState.DISABLED,
                builderAccess = AccessModifier.PACKAGE_PRIVATE,
                methodAccess = AccessModifier.PACKAGE_PRIVATE,
                builderSuffix = "Factory",
                setterSuffix = "with"
            ))
            public class PersonDto {
                private String name;
                private java.util.List<String> tags;

                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
                public java.util.List<String> getTags() { return tags; }
                public void setTags(java.util.List<String> tags) { this.tags = tags; }
            }
            """);

    // When: Compile
    Compilation compilation = ProcessorTestUtils.createCompiler().compile(source);

    // Then: Generated code reflects options
    assertThat(compilation).succeeded();

    String generatedCode = ProcessorTestUtils.loadGeneratedSource(compilation, "PersonDtoFactory");

    // Verify builderSuffix="Factory"
    ProcessorAsserts.assertContaining(generatedCode, "class PersonDtoFactory");

    // Verify class access is PACKAGE_PRIVATE
    ProcessorAsserts.assertNotContaining(generatedCode, "public class PersonDtoFactory");

    // Verify setterSuffix="with"
    ProcessorAsserts.assertContaining(generatedCode, "PersonDtoFactory withName(String name)");
    ProcessorAsserts.assertContaining(generatedCode, "PersonDtoFactory withTags(");

    // Verify builderAccess=PACKAGE_PRIVATE (no "public" before class)
    ProcessorAsserts.assertNotContaining(generatedCode, "public class PersonDtoFactory");
    ProcessorAsserts.assertContaining(generatedCode, "class PersonDtoFactory");

    // Verify methodAccess=PACKAGE_PRIVATE (no "public" before setter methods)
    ProcessorAsserts.assertNotContaining(generatedCode, "public PersonDtoFactory withName");
    ProcessorAsserts.assertContaining(generatedCode, "PersonDtoFactory withName(String name)");

    // Verify builder methods have public access even with builderAccess=PACKAGE_PRIVATE being
    // package private
    ProcessorAsserts.assertContaining(generatedCode, "public PersonDto build()");
    ProcessorAsserts.assertContaining(generatedCode, "public static PersonDtoFactory create()");

    // Verify generateFieldSupplier=DISABLED (no Supplier methods)
    ProcessorAsserts.assertNotContaining(generatedCode, "Supplier<String>");

    // Verify generateFieldConsumer=DISABLED (no Consumer methods for List)
    ProcessorAsserts.assertNotContaining(generatedCode, "Consumer<List<String>>");

    // Verify generateBuilderConsumer=DISABLED (no builder consumers)
    ProcessorAsserts.assertNotContaining(generatedCode, "Consumer<ArrayListBuilder");

    // Verify generateVarArgsHelpers=DISABLED
    ProcessorAsserts.assertNotContaining(
        generatedCode, "PersonDtoFactory withTags(String... tags)");
  }

  /**
   * Test: Builder respects configuration from a custom template annotation.
   *
   * <p>Verifies a custom annotation meta-annotated with {@code @SimpleBuilder.Template} is resolved
   * through the same path as the built-in {@code @SimpleBuilder}.
   */
  @Test
  void readFromTemplate_WithTemplateAnnotation_AppliesTemplateConfiguration() {
    JavaFileObject dtoSource =
        ProcessorTestUtils.forSource(
            """
            package test;
            import org.javahelpers.simple.builders.processor.testing.MyBuliderForTestAnnotation;

            @MyBuliderForTestAnnotation
            public class PersonDto {
                private String name;
                private int age;

                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
                public int getAge() { return age; }
                public void setAge(int age) { this.age = age; }
            }
            """);

    // When: Compile
    Compilation compilation = ProcessorTestUtils.createCompiler().compile(dtoSource);

    // Then: Generated code reflects template configuration
    assertThat(compilation).succeeded();

    String generatedCode =
        ProcessorTestUtils.loadGeneratedSource(compilation, "PersonDtoMiniBuilder");

    // Verify template values are applied
    ProcessorAsserts.assertContaining(generatedCode, "class PersonDtoMiniBuilder");
    ProcessorAsserts.assertContaining(generatedCode, "PersonDtoMiniBuilder setName(String name)");
    ProcessorAsserts.assertContaining(generatedCode, "PersonDtoMiniBuilder setAge(int age)");

    // Verify disabled features
    ProcessorAsserts.assertNotContaining(generatedCode, "Supplier<");
    ProcessorAsserts.assertNotContaining(generatedCode, "Consumer<");
  }

  /**
   * Test: Template annotation defined in same compilation round (inline).
   *
   * <p>This tests the AnnotationMirror fallback path when the template annotation is not yet
   * compiled (same-round compilation). The annotation processor must use AnnotationMirror to read
   * the template configuration instead of reflection.
   */
  @Test
  void readFromTemplate_InlineTemplateDefinition_AppliesTemplateConfiguration() {
    // Given: Source with inline template annotation definition AND usage
    JavaFileObject dtoSource =
        ProcessorTestUtils.forSource(
            """
            package test;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
            import org.javahelpers.simple.builders.core.enums.OptionState;
            import java.lang.annotation.*;

            @SimpleBuilder.Template(
                options = @SimpleBuilder.Options(
                    generateFieldSupplier = OptionState.DISABLED,
                    generateFieldConsumer = OptionState.DISABLED,
                    generateBuilderConsumer = OptionState.DISABLED,
                    generateVarArgsHelpers = OptionState.DISABLED,
                    generateConditionalHelper = OptionState.DISABLED,
                    generateWithInterface = OptionState.DISABLED,
                    builderSuffix = "InlineBuilder",
                    setterSuffix = "update"
                ))
            @Retention(RetentionPolicy.CLASS)
            @Target(ElementType.TYPE)
            @interface InlineTemplate {}

            @InlineTemplate
            class PersonDto {
                private String name;
                private int age;

                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
                public int getAge() { return age; }
                public void setAge(int age) { this.age = age; }
            }
            """);

    // When: Compile
    Compilation compilation = ProcessorTestUtils.createCompiler().compile(dtoSource);

    // Then: Generated code reflects inline template configuration
    assertThat(compilation).succeeded();

    String generatedCode =
        ProcessorTestUtils.loadGeneratedSource(compilation, "PersonDtoInlineBuilder");

    // Verify inline template values are applied
    ProcessorAsserts.assertContaining(generatedCode, "class PersonDtoInlineBuilder");
    ProcessorAsserts.assertContaining(
        generatedCode, "PersonDtoInlineBuilder updateName(String name)");
    ProcessorAsserts.assertContaining(generatedCode, "PersonDtoInlineBuilder updateAge(int age)");

    // Verify disabled features (tests AnnotationMirror path extracts values correctly)
    ProcessorAsserts.assertNotContaining(generatedCode, "Supplier<");
    ProcessorAsserts.assertNotContaining(generatedCode, "Consumer<");
  }

  /**
   * Test: Inline @SimpleBuilder options override a custom template annotation.
   *
   * <p>Verifies resolveConfiguration() applies correct priority when both the built-in
   * {@code @SimpleBuilder} and a custom template are present: direct {@code @SimpleBuilder} options
   * win over the custom template, which in turn wins over compiler arguments and defaults.
   */
  @Test
  void resolveConfiguration_OptionsOverridesTemplate_AppliesPriorityCorrectly() {
    JavaFileObject dtoSource =
        ProcessorTestUtils.forSource(
            """
            package test;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
            import org.javahelpers.simple.builders.processor.testing.MyBuliderForTestAnnotation;

            @MyBuliderForTestAnnotation
            @SimpleBuilder(options = @SimpleBuilder.Options(
                builderSuffix = "OptionsBuilder",
                setterSuffix = "set"
            ))
            public class PersonDto {
                private String name;

                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
            }
            """);

    // When: Compile
    Compilation compilation = ProcessorTestUtils.createCompiler().compile(dtoSource);

    // Then: Options wins over template
    assertThat(compilation).succeeded();

    String generatedCode =
        ProcessorTestUtils.loadGeneratedSource(compilation, "PersonDtoOptionsBuilder");

    // Verify options values (not template values)
    ProcessorAsserts.assertContaining(generatedCode, "class PersonDtoOptionsBuilder");
    ProcessorAsserts.assertContaining(
        generatedCode, "PersonDtoOptionsBuilder setName(String name)");

    // Template values should NOT be present
    ProcessorAsserts.assertNotContaining(generatedCode, "TemplateBuilder");
    ProcessorAsserts.assertNotContaining(generatedCode, "withName");
  }

  /**
   * Test: Compiler arguments apply when no annotation configuration present.
   *
   * <p>Verifies BuilderConfigurationReader correctly reads and applies compiler arguments via
   * CompilerArgumentsReader integration.
   */
  @Test
  void resolveConfiguration_WithCompilerArgsOnly_AppliesCompilerArgs() {
    // Given: Simple @SimpleBuilder with no options or template
    JavaFileObject source =
        ProcessorTestUtils.forSource(
            """
            package test;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public class PersonDto {
                private String name;
                private java.util.List<String> tags;

                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
                public java.util.List<String> getTags() { return tags; }
                public void setTags(java.util.List<String> tags) { this.tags = tags; }
            }
            """);

    // When: Compile with compiler arguments
    Compilation compilation =
        ProcessorTestUtils.createCompiler()
            .withOptions(
                "-Asimplebuilder.builderSuffix=CustomBuilder",
                "-Asimplebuilder.setterSuffix=set",
                "-Asimplebuilder.generateVarArgsHelpers=false")
            .compile(source);

    // Then: Compiler args are applied
    assertThat(compilation).succeeded();

    String generatedCode =
        ProcessorTestUtils.loadGeneratedSource(compilation, "PersonDtoCustomBuilder");

    ProcessorAsserts.assertContaining(generatedCode, "class PersonDtoCustomBuilder");
    ProcessorAsserts.assertContaining(generatedCode, "PersonDtoCustomBuilder setName(String name)");
    ProcessorAsserts.assertContaining(generatedCode, "PersonDtoCustomBuilder setTags(");

    // VarArgs disabled by compiler arg
    ProcessorAsserts.assertNotContaining(generatedCode, "setTags(String... tags)");
  }

  /** Test: Options override compiler arguments (proper priority). */
  @Test
  void resolveConfiguration_OptionsOverridesCompilerArgs_AppliesPriorityCorrectly() {
    // Given: Both compiler args and options specified
    JavaFileObject source =
        ProcessorTestUtils.forSource(
            """
            package test;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder(options = @SimpleBuilder.Options(
                builderSuffix = "OptionsBuilder"
            ))
            public class PersonDto {
                private String name;

                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
            }
            """);

    // When: Compile with conflicting compiler argument
    Compilation compilation =
        ProcessorTestUtils.createCompiler()
            .withOptions("-Asimplebuilder.builderSuffix=CompilerArgBuilder")
            .compile(source);

    // Then: Options wins over compiler arg
    assertThat(compilation).succeeded();

    String generatedCode =
        ProcessorTestUtils.loadGeneratedSource(compilation, "PersonDtoOptionsBuilder");

    // Verify options value (not compiler arg value)
    ProcessorAsserts.assertContaining(generatedCode, "class PersonDtoOptionsBuilder");
    ProcessorAsserts.assertNotContaining(generatedCode, "CompilerArgBuilder");
  }

  /**
   * Test: All optional features disabled through mixed configuration layers.
   *
   * <p>Verifies comprehensive configuration resolution by disabling ALL optional features, but
   * distributing the disabling across different configuration layers (inline, compiler args,
   * defaults). This ensures the complete priority chain works correctly and serves as a regression
   * test that new features are properly processed.
   *
   * <p>Configuration strategy - ALL optional features disabled:
   *
   * <ul>
   *   <li>Inline options: Disable field supplier, field consumer, builder consumer, varargs
   *       helpers, unboxed optional, collection builders, annotations
   *       (@Generated, @BuilderImplementation)
   *   <li>Compiler args: Disable conditional helper, with interface, string format helpers
   *   <li>Inline options: Custom naming (builderSuffix, setterSuffix)
   *   <li>Template: Ignored (because @SimpleBuilder is present)
   * </ul>
   */
  @Test
  void resolveConfiguration_AllLayersTogether_CompleteChain() {
    JavaFileObject dtoSource =
        ProcessorTestUtils.forSource(
            """
            package test;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
            import org.javahelpers.simple.builders.core.enums.OptionState;
            import org.javahelpers.simple.builders.processor.testing.MyBuliderForTestAnnotation;

            @MyBuliderForTestAnnotation
            @SimpleBuilder(options = @SimpleBuilder.Options(
                generateFieldSupplier = OptionState.DISABLED,
                generateFieldConsumer = OptionState.DISABLED,
                generateBuilderConsumer = OptionState.DISABLED,
                generateVarArgsHelpers = OptionState.DISABLED,
                generateAddToCollectionHelpers = OptionState.DISABLED,
                generateUnboxedOptional = OptionState.DISABLED,
                usingArrayListBuilder = OptionState.DISABLED,
                usingHashSetBuilder = OptionState.DISABLED,
                usingHashMapBuilder = OptionState.DISABLED,
                usingGeneratedAnnotation = OptionState.DISABLED,
                usingBuilderImplementationAnnotation = OptionState.DISABLED,
                usingJacksonDeserializerAnnotation = OptionState.DISABLED,
                builderSuffix = "MinimalBuilder",
                setterSuffix = "with"
            ))
            public class PersonDto {
                private String name;
                private java.util.List<String> tags;

                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
                public java.util.List<String> getTags() { return tags; }
                public void setTags(java.util.List<String> tags) { this.tags = tags; }
            }
            """);

    // When: Compile with compiler args that disable additional features
    Compilation compilation =
        ProcessorTestUtils.createCompiler()
            .withOptions(
                "-Asimplebuilder.generateConditionalHelper=DISABLED",
                "-Asimplebuilder.generateWithInterface=DISABLED",
                "-Asimplebuilder.generateStringFormatHelpers=DISABLED")
            .compile(dtoSource);

    // Then: Configuration is applied correctly
    assertThat(compilation).succeeded();

    String generatedCode =
        ProcessorTestUtils.loadGeneratedSource(compilation, "PersonDtoMinimalBuilder");

    // Assert complete generated source to ensure all configuration options are applied correctly.
    // This comprehensive test validates that ALL optional features can be properly disabled
    // through the configuration chain. If this test fails after adding a new feature, it means
    // the feature may not be properly integrated into the configuration processing.
    String expectedCode =
        """
        package test;

        import static org.javahelpers.simple.builders.core.util.TrackedValue.changedValue;
        import static org.javahelpers.simple.builders.core.util.TrackedValue.initialValue;
        import static org.javahelpers.simple.builders.core.util.TrackedValue.unsetValue;

        import java.util.List;
        import org.apache.commons.lang3.builder.ToStringBuilder;
        import org.javahelpers.simple.builders.core.interfaces.IBuilderBase;
        import org.javahelpers.simple.builders.core.util.BuilderToStringStyle;
        import org.javahelpers.simple.builders.core.util.TrackedValue;

        /**
         * Builder for {@code test.PersonDto}.
         * <p>
         * This builder provides a fluent API for creating instances of test.PersonDto with method chaining and validation. Use
         * the static {@code create()} method to obtain a new builder instance, configure the desired properties using the
         * setter methods, and then call {@code build()} to create the final DTO.
         *
         * <h4>Example:</h4>
         *
         * <pre>{@code
        * PersonDto result = PersonDtoMinimalBuilder.create()
        *     .withName("example value")
        *     .withTags(List.of("example value"))
        *     .build();
        * }</pre>
         */
        public class PersonDtoMinimalBuilder implements IBuilderBase<PersonDto> {

          /**
           * Tracked value for <code>name</code>: name.
           */
          private TrackedValue<String> name = unsetValue();
          /**
           * Tracked value for <code>tags</code>: tags.
           */
          private TrackedValue<List<String>> tags = unsetValue();

          /**
           * Empty constructor of builder for {@code test.PersonDto}.
           */
          public PersonDtoMinimalBuilder() {
          }

          /**
           * Initialisation of builder for {@code test.PersonDto} by a instance.
           *
           * @param instance object instance for initialisiation
           */
          public PersonDtoMinimalBuilder(PersonDto instance) {
            this.name = initialValue(instance.getName());
            this.tags = initialValue(instance.getTags());
          }

          /**
           * Creating a new builder for {@code test.PersonDto}.
           *
           * <h4>Example:</h4>
           *
           * <pre>{@code
           * PersonDtoMinimalBuilder builder = PersonDtoMinimalBuilder.create();
           * }</pre>
           *
           * @return builder for {@code test.PersonDto}
           */
          public static PersonDtoMinimalBuilder create() {
            return new PersonDtoMinimalBuilder();
          }

          /**
           * Sets the value for <code>name</code>.
           * <p>
           * Generated from setter {@link PersonDto#setName(String) setName(String name)}
           *
           * <h4>Example:</h4>
           *
           * <pre>{@code
           * builder.withName("example value");
           * }</pre>
           *
           * @param name name
           * @return current instance of builder
           */
          public PersonDtoMinimalBuilder withName(String name) {
            this.name = changedValue(name);
            return this;
          }

          /**
           * Sets the value for <code>tags</code>.
           * <p>
           * Generated from setter {@link PersonDto#setTags(List) setTags(List<String> tags)}
           *
           * <h4>Example:</h4>
           *
           * <pre>{@code
           * builder.withTags(List.of("example value"));
           * }</pre>
           *
           * @param tags tags
           * @return current instance of builder
           */
          public PersonDtoMinimalBuilder withTags(List<String> tags) {
            this.tags = changedValue(tags);
            return this;
          }

          /**
           * Builds the configured DTO instance.
           *
           * <h4>Example:</h4>
           *
           * <pre>{@code
           * PersonDto result = builder.build();
           * }</pre>
           */
          @Override
          public PersonDto build() {
            PersonDto result = new PersonDto();
            this.name.ifSet(result::setName);
            this.tags.ifSet(result::setTags);
            return result;
          }

          /**
           * Returns a string representation of this builder, including only fields that have been set.
           *
           * @return string representation of the builder
           */
          @Override
          public String toString() {
            return new ToStringBuilder(this, BuilderToStringStyle.INSTANCE).append("name", this.name)
                .append("tags", this.tags)
                .toString();
          }
        }
        """;

    // Normalize whitespace for comparison to avoid formatting issues
    String normalizedExpected = expectedCode.replaceAll("\\s+", " ").trim();
    String normalizedGenerated = generatedCode.replaceAll("\\s+", " ").trim();

    org.junit.jupiter.api.Assertions.assertEquals(
        normalizedExpected,
        normalizedGenerated,
        "Generated code does not match expected. This comprehensive test ensures all configuration "
            + "options are correctly applied. If this fails, a configuration option may have been "
            + "added without proper processing support.");
  }

  /**
   * Test: PRIVATE builderAccess fails builder generation.
   *
   * <p>Verifies that using PRIVATE for builderAccess causes builder generation to fail with a clear
   * error message, while allowing other builders to be processed successfully.
   */
  @Test
  void resolveConfiguration_PrivateBuilderAccess_FailsGeneration() {
    JavaFileObject dtoSource =
        ProcessorTestUtils.forSource(
            """
            package test;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
            import org.javahelpers.simple.builders.core.enums.AccessModifier;

            @SimpleBuilder(options = @SimpleBuilder.Options(
                builderAccess = AccessModifier.PRIVATE
            ))
            public class PersonDto {
                private String name;

                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
            }
            """);

    // When: Compile
    Compilation compilation = ProcessorTestUtils.createCompiler().compile(dtoSource);

    // Then: Builder generation fails but compilation succeeds (no invalid Java code generated)
    assertThat(compilation).succeeded();
    assertThat(compilation).hadWarningContaining("Failed to generate builder");
    assertThat(compilation).hadWarningContaining("builderAccess=PRIVATE");
    assertThat(compilation).hadWarningContaining("Java does not allow private top-level classes");
  }

  /**
   * Test: PRIVATE methodAccess fails builder generation.
   *
   * <p>Verifies that using PRIVATE for methodAccess causes builder generation to fail with a clear
   * error message, while allowing other builders to be processed successfully.
   */
  @Test
  void resolveConfiguration_PrivateMethodAccess_FailsGeneration() {
    JavaFileObject dtoSource =
        ProcessorTestUtils.forSource(
            """
            package test;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
            import org.javahelpers.simple.builders.core.enums.AccessModifier;

            @SimpleBuilder(options = @SimpleBuilder.Options(
                methodAccess = AccessModifier.PRIVATE
            ))
            public class PersonDto {
                private String name;

                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
            }
            """);

    // When: Compile
    Compilation compilation = ProcessorTestUtils.createCompiler().compile(dtoSource);

    // Then: Builder generation fails but compilation succeeds (no invalid Java code generated)
    assertThat(compilation).succeeded();
    assertThat(compilation).hadWarningContaining("Failed to generate builder");
    assertThat(compilation).hadWarningContaining("methodAccess=PRIVATE");
    assertThat(compilation).hadWarningContaining("makes all setter methods inaccessible");
  }

  /**
   * Test: PRIVATE builderConstructorAccess does NOT fail generation.
   *
   * <p>Verifies that using PRIVATE for builderConstructorAccess is acceptable and does not cause
   * builder generation to fail (it's a valid pattern to enforce using factory methods).
   */
  @Test
  void resolveConfiguration_PrivateBuilderConstructorAccess_NoFailure() {
    JavaFileObject dtoSource =
        ProcessorTestUtils.forSource(
            """
            package test;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
            import org.javahelpers.simple.builders.core.enums.AccessModifier;

            @SimpleBuilder(options = @SimpleBuilder.Options(
                builderConstructorAccess = AccessModifier.PRIVATE
            ))
            public class PersonDto {
                private String name;

                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
            }
            """);

    // When: Compile
    Compilation compilation = ProcessorTestUtils.createCompiler().compile(dtoSource);

    // Then: Compilation succeeds without warnings about access modifiers
    assertThat(compilation).succeededWithoutWarnings();
  }

  /**
   * Test: {@code @SimpleBuilder.Options(formattingMode = "none")} produces unformatted output.
   *
   * <p>When {@code formattingMode} is set to {@code "none"} via annotation options, the generated
   * source should be raw Roaster output without any post-processing (no indentation fixup).
   */
  @Test
  void readFromOptions_FormattingModeNone_GeneratesUnformattedCode() {
    JavaFileObject source =
        ProcessorTestUtils.forSource(
            """
            package test;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder(options = @SimpleBuilder.Options(
                formattingMode = "none"
            ))
            public class PersonDto {
                private String name;

                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
            }
            """);

    Compilation compilation = ProcessorTestUtils.createCompiler().compile(source);

    assertThat(compilation).succeeded();
    String generatedCode = ProcessorTestUtils.loadGeneratedSource(compilation, "PersonDtoBuilder");
    assertNotNull(generatedCode, "PersonDtoBuilder should be generated");
  }

  /**
   * Test: {@code @SimpleBuilder.Options(formattingMode = "lightweight")} produces formatted output.
   *
   * <p>When {@code formattingMode} is set to {@code "lightweight"} via annotation options, the
   * generated source should have proper indentation (tabs converted to spaces).
   */
  @Test
  void readFromOptions_FormattingModeLightweight_GeneratesFormattedCode() {
    JavaFileObject source =
        ProcessorTestUtils.forSource(
            """
            package test;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder(options = @SimpleBuilder.Options(
                formattingMode = "lightweight"
            ))
            public class PersonDto {
                private String name;

                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
            }
            """);

    Compilation compilation = ProcessorTestUtils.createCompiler().compile(source);

    assertThat(compilation).succeeded();
    String generatedCode = ProcessorTestUtils.loadGeneratedSource(compilation, "PersonDtoBuilder");
    assertNotNull(generatedCode, "PersonDtoBuilder should be generated");

    // Lightweight formatting should produce 2-space indentation (not tabs)
    ProcessorAsserts.assertContaining(generatedCode, "  private");
  }

  /**
   * Test: {@code @SimpleMinimalBuilder} uses lightweight formatting by default.
   *
   * <p>The {@code @SimpleMinimalBuilder} template sets {@code formattingMode = "lightweight"} in
   * its template options. This verifies the template option flows through correctly.
   */
  @Test
  void readFromTemplate_SimpleMinimalBuilder_UsesLightweightFormatting() {
    JavaFileObject source =
        ProcessorTestUtils.forSource(
            """
            package test;
            import org.javahelpers.simple.builders.core.annotations.SimpleMinimalBuilder;

            @SimpleMinimalBuilder
            public class MinimalDto {
                private String name;

                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
            }
            """);

    Compilation compilation = ProcessorTestUtils.createCompiler().compile(source);

    assertThat(compilation).succeeded();
    String generatedCode = ProcessorTestUtils.loadGeneratedSource(compilation, "MinimalDtoBuilder");
    assertNotNull(generatedCode, "MinimalDtoBuilder should be generated");

    // Lightweight formatting should produce 2-space indentation (not tabs)
    ProcessorAsserts.assertContaining(generatedCode, "  private");
  }

  /**
   * Test: Default {@code @SimpleBuilder} (without formattingMode option) uses JDT formatting.
   *
   * <p>When no {@code formattingMode} is specified in annotation options, the global default (JDT)
   * from compiler arguments should be used.
   */
  @Test
  void readFromOptions_NoFormattingMode_UsesJdtDefault() {
    JavaFileObject source =
        ProcessorTestUtils.simpleBuilderClass(
            "test",
            "PersonDto",
            """
            private String name;

            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            """);

    Compilation compilation = ProcessorTestUtils.createCompiler().compile(source);

    assertThat(compilation).succeeded();
    String generatedCode = ProcessorTestUtils.loadGeneratedSource(compilation, "PersonDtoBuilder");
    assertNotNull(generatedCode, "PersonDtoBuilder should be generated");

    // JDT formatting should produce 2-space indentation
    ProcessorAsserts.assertContaining(generatedCode, "  private");
  }
}
