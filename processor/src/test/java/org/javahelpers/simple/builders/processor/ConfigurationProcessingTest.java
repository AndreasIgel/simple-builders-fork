package org.javahelpers.simple.builders.processor;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.testing.compile.Compilation;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.tools.JavaFileObject;
import org.javahelpers.simple.builders.core.enums.AccessModifier;
import org.javahelpers.simple.builders.core.enums.FormattingMode;
import org.javahelpers.simple.builders.core.enums.OptionState;
import org.javahelpers.simple.builders.processor.model.core.BuilderConfiguration;
import org.javahelpers.simple.builders.processor.testing.ProcessorAsserts;
import org.javahelpers.simple.builders.processor.testing.ProcessorTestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests ensuring all configuration options are properly handled.
 *
 * <p>These tests are designed to fail at compile-time if:
 *
 * <ul>
 *   <li>A new configuration option is added but not included in the builder
 *   <li>A new option is added but not handled in merge logic
 *   <li>A new option is added but not included in toString
 * </ul>
 *
 * <p>This ensures that when extending the configuration, all three places must be updated:
 *
 * <ol>
 *   <li>BuilderConfiguration record parameters
 *   <li>BuilderConfiguration.Builder
 *   <li>BuilderConfiguration.merge() method
 * </ol>
 */
class ConfigurationProcessingTest {

  /**
   * Completeness test: All configuration options must be settable via builder.
   *
   * <p>If you add a new configuration option and this test doesn't compile, you need to:
   *
   * <ol>
   *   <li>Add the parameter to BuilderConfiguration record
   *   <li>Add builder methods in BuilderConfiguration.Builder
   *   <li>Update DEFAULT configuration
   *   <li>Update this test to include the new option
   * </ol>
   */
  @Test
  void allConfigurationOptions_MustBeSettableViaBuilder() {
    // This test will fail to compile if any builder method is missing
    BuilderConfiguration config =
        BuilderConfiguration.builder()
            // Field setter generation options
            .generateSupplier(OptionState.ENABLED)
            .generateConsumer(OptionState.ENABLED)
            .generateBuilderConsumer(OptionState.ENABLED)
            // Conditional logic
            .generateConditionalLogic(OptionState.ENABLED)
            // Access control
            .builderAccess(AccessModifier.PACKAGE_PRIVATE)
            .builderConstructorAccess(AccessModifier.PRIVATE)
            .methodAccess(AccessModifier.PACKAGE_PRIVATE)
            // Helper method generation
            .generateVarArgsHelpers(OptionState.ENABLED)
            .generateStringFormatHelpers(OptionState.ENABLED)
            .generateUnboxedOptional(OptionState.ENABLED)
            .copyTypeAnnotations(OptionState.ENABLED)
            // Collection builder options
            .usingArrayListBuilder(OptionState.ENABLED)
            .usingArrayListBuilderWithElementBuilders(OptionState.ENABLED)
            .usingHashSetBuilder(OptionState.ENABLED)
            .usingHashSetBuilderWithElementBuilders(OptionState.ENABLED)
            .usingHashMapBuilder(OptionState.ENABLED)
            // Annotations
            .usingGeneratedAnnotation(OptionState.ENABLED)
            .usingBuilderImplementationAnnotation(OptionState.ENABLED)
            // Integration
            .implementsBuilderBase(OptionState.ENABLED)
            .generateWithInterface(OptionState.ENABLED)
            .usingJacksonDeserializerAnnotation(OptionState.ENABLED)
            .generateJacksonModule(OptionState.ENABLED)
            // Documentation
            .generateJavaDoc(OptionState.ENABLED)
            // Naming
            .builderSuffix("Builder")
            .setterSuffix("")
            // Formatting
            .formattingMode("lightweight")
            .build();

    // Verify all options are accessible (this will fail to compile if accessors are missing)
    assertNotNull(config);
    assertEquals(OptionState.ENABLED, config.generateFieldSupplier());
    assertEquals(OptionState.ENABLED, config.generateFieldConsumer());
    assertEquals(OptionState.ENABLED, config.generateBuilderConsumer());
    assertEquals(OptionState.ENABLED, config.generateConditionalHelper());
    assertEquals(AccessModifier.PACKAGE_PRIVATE, config.getBuilderAccess());
    assertEquals(AccessModifier.PRIVATE, config.getBuilderConstructorAccess());
    assertEquals(AccessModifier.PACKAGE_PRIVATE, config.getMethodAccess());
    assertEquals(OptionState.ENABLED, config.generateVarArgsHelpers());
    assertEquals(OptionState.ENABLED, config.generateStringFormatHelpers());
    assertEquals(OptionState.ENABLED, config.generateUnboxedOptional());
    assertEquals(OptionState.ENABLED, config.copyTypeAnnotations());
    assertEquals(OptionState.ENABLED, config.usingArrayListBuilder());
    assertEquals(OptionState.ENABLED, config.usingArrayListBuilderWithElementBuilders());
    assertEquals(OptionState.ENABLED, config.usingHashSetBuilder());
    assertEquals(OptionState.ENABLED, config.usingHashSetBuilderWithElementBuilders());
    assertEquals(OptionState.ENABLED, config.usingHashMapBuilder());
    assertEquals(OptionState.ENABLED, config.usingGeneratedAnnotation());
    assertEquals(OptionState.ENABLED, config.usingBuilderImplementationAnnotation());
    assertEquals(OptionState.ENABLED, config.implementsBuilderBase());
    assertEquals(OptionState.ENABLED, config.generateWithInterface());
    assertEquals(OptionState.ENABLED, config.usingJacksonDeserializerAnnotation());
    assertEquals(OptionState.ENABLED, config.generateJacksonModule());
    assertEquals(OptionState.ENABLED, config.generateJavaDoc());
    assertEquals("Builder", config.getBuilderSuffix());
    assertEquals("", config.getSetterSuffix());
    assertEquals("lightweight", config.formattingMode());
  }

  @Test
  void compilerArguments_FormatterProfile_UsesCustomProfile(@TempDir Path tempDir)
      throws IOException {
    String profile =
        new String(
                ConfigurationProcessingTest.class
                    .getClassLoader()
                    .getResourceAsStream("eclipse-java-format.xml")
                    .readAllBytes(),
                StandardCharsets.UTF_8)
            .replace(
                "<setting id=\"org.eclipse.jdt.core.formatter.tabulation.char\" value=\"space\"/>",
                "<setting id=\"org.eclipse.jdt.core.formatter.tabulation.char\" value=\"tab\"/>");
    Path profilePath = tempDir.resolve("custom-eclipse-profile.xml");
    Files.writeString(profilePath, profile);

    JavaFileObject source =
        ProcessorTestUtils.simpleBuilderClass(
            "test",
            "FormatterProfileDto",
            """
            private String name;
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            """);
    Compilation compilation =
        ProcessorTestUtils.createCompiler()
            .withOptions("-Asimplebuilder.formatterProfile=" + profilePath)
            .compile(source);

    assertThat(compilation).succeeded();
    String generated =
        ProcessorTestUtils.loadGeneratedSource(compilation, "FormatterProfileDtoBuilder");
    assertTrue(generated.contains("\t"), "Custom formatter profile should produce tab indentation");
  }

  /**
   * Compiler arguments integration test: Verify generated builder with all options disabled.
   *
   * <p>This test documents the current state of generated builder code when all compiler arguments
   * are set to false. It ensures that disabling features actually removes them from generated code.
   *
   * <p>If generated code format changes, update the expected text block to reflect current state.
   */
  @Test
  void compilerArguments_AllDisabled_ShouldGenerateMinimalBuilder() {
    // Given: DTO with various property types including nested DTO with builder and Address without
    // builder
    JavaFileObject nestedDto =
        ProcessorTestUtils.simpleBuilderClass(
            "test",
            "NestedDto",
            """
            private String value;
            public String getValue() { return value; }
            public void setValue(String value) { this.value = value; }
            """);

    JavaFileObject addressDto =
        ProcessorTestUtils.forSource(
            """
            package test;

            public class Address {
              private String street;
              private String city;

              public Address() {}

              public String getStreet() { return street; }
              public void setStreet(String street) { this.street = street; }

              public String getCity() { return city; }
              public void setCity(String city) { this.city = city; }
            }
            """);

    JavaFileObject source =
        ProcessorTestUtils.simpleBuilderClass(
            "test",
            "MinimalDto",
            """
            private String name;
            private java.util.List<String> items;
            private java.util.Map<String, Integer> properties;
            private java.util.Optional<String> description;
            private java.util.Set<String> tags;
            private NestedDto nested;
            private Address address;

            public String getName() { return name; }
            public void setName(String name) { this.name = name; }

            public java.util.List<String> getItems() { return items; }
            public void setItems(java.util.List<String> items) { this.items = items; }

            public java.util.Map<String, Integer> getProperties() { return properties; }
            public void setProperties(java.util.Map<String, Integer> properties) { this.properties = properties; }

            public java.util.Optional<String> getDescription() { return description; }
            public void setDescription(java.util.Optional<String> description) { this.description = description; }

            public java.util.Set<String> getTags() { return tags; }
            public void setTags(java.util.Set<String> tags) { this.tags = tags; }

            public NestedDto getNested() { return nested; }
            public void setNested(NestedDto nested) { this.nested = nested; }

            public Address getAddress() { return address; }
            public void setAddress(Address address) { this.address = address; }
            """);

    // When: Compile with ALL compiler arguments disabled and custom builder suffix
    Compilation compilation =
        ProcessorTestUtils.createCompiler()
            .withOptions(
                "-Asimplebuilder.generateFieldSupplier=false",
                "-Asimplebuilder.generateFieldConsumer=false",
                "-Asimplebuilder.generateBuilderConsumer=false",
                "-Asimplebuilder.generateConditionalHelper=false",
                "-Asimplebuilder.builderAccess=PACKAGE_PRIVATE",
                "-Asimplebuilder.builderConstructorAccess=PRIVATE",
                "-Asimplebuilder.methodAccess=PACKAGE_PRIVATE",
                "-Asimplebuilder.generateVarArgsHelpers=false",
                "-Asimplebuilder.generateStringFormatHelpers=false",
                "-Asimplebuilder.generateUnboxedOptional=false",
                "-Asimplebuilder.copyTypeAnnotations=false",
                "-Asimplebuilder.usingArrayListBuilder=false",
                "-Asimplebuilder.usingArrayListBuilderWithElementBuilders=false",
                "-Asimplebuilder.usingHashSetBuilder=false",
                "-Asimplebuilder.usingHashSetBuilderWithElementBuilders=false",
                "-Asimplebuilder.usingHashMapBuilder=false",
                "-Asimplebuilder.usingGeneratedAnnotation=false",
                "-Asimplebuilder.usingBuilderImplementationAnnotation=false",
                "-Asimplebuilder.implementsBuilderBase=false",
                "-Asimplebuilder.generateWithInterface=false",
                "-Asimplebuilder.usingJacksonDeserializerAnnotation=false",
                "-Asimplebuilder.generateJavaDoc=false",
                "-Asimplebuilder.builderSuffix=CustomBuilder",
                "-Asimplebuilder.setterSuffix=with")
            .compile(nestedDto, addressDto, source);

    // Then: Compilation should succeed
    assertThat(compilation).succeeded();

    // And: Generated builder should contain only basic functionality
    String generatedCode =
        ProcessorTestUtils.loadGeneratedSource(compilation, "MinimalDtoCustomBuilder");

    // With generateFieldSupplier=false, NO supplier methods should be generated
    ProcessorAsserts.assertNotContaining(
        generatedCode,
        "public MinimalDtoCustomBuilder withName(Supplier<String> nameSupplier)",
        "public MinimalDtoCustomBuilder withItems(Supplier<List<String>> itemsSupplier)",
        "public MinimalDtoCustomBuilder withProperties(Supplier<Map<String, Integer>> propertiesSupplier)",
        "public MinimalDtoCustomBuilder withDescription(Supplier<Optional<String>> descriptionSupplier)",
        "public MinimalDtoCustomBuilder withTags(Supplier<Set<String>> tagsSupplier)",
        "public MinimalDtoCustomBuilder withNested(Supplier<NestedDto> nestedSupplier)",
        "public MinimalDtoCustomBuilder withAddress(Supplier<Address> addressSupplier)");

    // With generateFieldConsumer=false, NO field consumer methods should be generated
    // Field consumer = Consumer<T> where T is a custom type with empty constructor
    ProcessorAsserts.assertNotContaining(
        generatedCode,
        "public MinimalDtoCustomBuilder withAddress(Consumer<Address> addressConsumer)",
        "public MinimalDtoCustomBuilder withNested(Consumer<NestedDto> nestedConsumer)",
        "public MinimalDtoCustomBuilder withItems(Consumer<List<String>> itemsConsumer)",
        "public MinimalDtoCustomBuilder withTags(Consumer<Set<String>> tagsConsumer)",
        "public MinimalDtoCustomBuilder withProperties(Consumer<Map<String, Integer>> propertiesConsumer)");

    // With generateBuilderConsumer=false, NO builder consumer methods should be generated
    // Builder consumers include: StringBuilder, collection builders, nested DTO builders
    ProcessorAsserts.assertNotContaining(
        generatedCode,
        "public MinimalDtoCustomBuilder withNested(Consumer<NestedDtoBuilder> nestedBuilderConsumer)",
        "public MinimalDtoCustomBuilder withName(Consumer<StringBuilder> nameStringBuilderConsumer)",
        "public MinimalDtoCustomBuilder withDescription(Consumer<StringBuilder> descriptionStringBuilderConsumer)");

    // With generateConditionalHelper=false, NO conditional methods
    ProcessorAsserts.assertNotContaining(
        generatedCode, "public MinimalDtoCustomBuilder conditional(BooleanSupplier condition");

    // With generateVarArgsHelpers=false, NO VarArgs helpers should be generated
    ProcessorAsserts.assertNotContaining(
        generatedCode,
        "public MinimalDtoCustomBuilder withItems(String... items)",
        "public MinimalDtoCustomBuilder withProperties(Map.Entry<String, Integer>... properties)",
        "public MinimalDtoCustomBuilder withTags(String... tags)");

    // With generateWithInterface=false, NO With interface
    ProcessorAsserts.assertNotContaining(generatedCode, "public interface With");

    // With generateUnboxedOptional=false, NO unboxed optional methods should be generated
    ProcessorAsserts.assertNotContaining(
        generatedCode, "public MinimalDtoCustomBuilder withDescription(String description)");

    // With generateStringFormatHelpers=false, NO String format methods should be generated
    ProcessorAsserts.assertNotContaining(
        generatedCode,
        "public MinimalDtoCustomBuilder withName(String format, Object... args)",
        "public MinimalDtoCustomBuilder withDescription(String format, Object... args)");

    // With usingGeneratedAnnotation=false, NO @Generated annotation should be used
    ProcessorAsserts.assertNotContaining(generatedCode, "@Generated(");

    // With usingBuilderImplementationAnnotation=false, NO @BuilderImplementation annotation should
    // be used
    ProcessorAsserts.assertNotContaining(generatedCode, "@BuilderImplementation");

    // With implementsBuilderBase=false, NO IBuilderBase interface should be implemented
    ProcessorAsserts.assertNotContaining(
        generatedCode, "implements IBuilderBase", "@Override public MinimalDto build()");

    // With usingJacksonDeserializerAnnotation=false, NO @JsonPOJOBuilder annotation should be used
    ProcessorAsserts.assertNotContaining(generatedCode, "@JsonPOJOBuilder");

    // With generateJavaDoc=false, NO Javadoc comments should be generated
    ProcessorAsserts.assertNotContaining(generatedCode, "/**");

    // With builderAccess=PACKAGE_PRIVATE, builder class should NOT have public modifier
    ProcessorAsserts.assertNotContaining(generatedCode, "public class MinimalDtoCustomBuilder");

    // But package-private class should exist
    ProcessorAsserts.assertContaining(generatedCode, "class MinimalDtoCustomBuilder");

    // With methodAccess=PACKAGE_PRIVATE, methods should NOT have public modifier
    ProcessorAsserts.assertNotContaining(
        generatedCode, "public MinimalDtoCustomBuilder withName(String name)");

    // But package-private methods should exist - with setterSuffix="with", methods are prefixed
    ProcessorAsserts.assertContaining(
        generatedCode,
        "MinimalDtoCustomBuilder withName(String name)",
        "MinimalDto build()",
        "static MinimalDtoCustomBuilder create()");

    // With usingArrayListBuilder=false AND generateBuilderConsumer=false, NO ArrayListBuilder
    // should be used
    ProcessorAsserts.assertNotContaining(
        generatedCode,
        "MinimalDtoCustomBuilder withItems(Consumer<ArrayListBuilder<String>> itemsBuilderConsumer)");

    // With usingHashSetBuilder=false AND generateBuilderConsumer=false, NO HashSetBuilder should be
    // used
    ProcessorAsserts.assertNotContaining(
        generatedCode,
        "MinimalDtoCustomBuilder withTags(Consumer<HashSetBuilder<String>> tagsBuilderConsumer)");

    // With usingHashMapBuilder=false AND generateBuilderConsumer=false, NO HashMapBuilder should be
    // used
    ProcessorAsserts.assertNotContaining(
        generatedCode,
        "MinimalDtoCustomBuilder withProperties(Consumer<HashMapBuilder<String, Integer>> propertiesBuilderConsumer)");

    // Still generates: basic setters and build method
    // With setterSuffix="with", all setter methods should be prefixed with "with" and capitalized
    ProcessorAsserts.assertContaining(
        generatedCode,
        "class MinimalDtoCustomBuilder",
        "private MinimalDtoCustomBuilder()",
        "private MinimalDtoCustomBuilder(MinimalDto instance)",
        "MinimalDtoCustomBuilder withName(String name)",
        "MinimalDtoCustomBuilder withItems(List<String> items)",
        "MinimalDtoCustomBuilder withProperties(Map<String, Integer> properties)",
        "MinimalDtoCustomBuilder withDescription(Optional<String> description)",
        "MinimalDtoCustomBuilder withTags(Set<String> tags)",
        "MinimalDtoCustomBuilder withNested(NestedDto nested)",
        "MinimalDtoCustomBuilder withAddress(Address address)",
        "MinimalDto build()",
        "static MinimalDtoCustomBuilder create()");
  }

  /**
   * Inline annotation test: Disabling Javadoc via {@code @SimpleBuilder.Options} removes Javadoc
   * blocks while keeping the builder API intact.
   */
  @Test
  void inlineOptions_generateJavaDocDisabled_ShouldNotGenerateJavadoc() {
    JavaFileObject source =
        ProcessorTestUtils.forSource(
            """
            package test;

            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
            import org.javahelpers.simple.builders.core.enums.OptionState;

            @SimpleBuilder(options = @SimpleBuilder.Options(generateJavaDoc = OptionState.DISABLED))
            public class PersonDto {
                private String name;

                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
            }
            """);

    Compilation compilation = ProcessorTestUtils.createCompiler().compile(source);

    assertThat(compilation).succeeded();

    String generatedCode = ProcessorTestUtils.loadGeneratedSource(compilation, "PersonDtoBuilder");

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

        @Generated("Generated by org.javahelpers.simple.builders.processor.BuilderProcessor")
        @BuilderImplementation(forClass = PersonDto.class)
        public class PersonDtoBuilder implements IBuilderBase<PersonDto> {

          private TrackedValue<String> name = unsetValue();

          public PersonDtoBuilder() {
          }

          public PersonDtoBuilder(PersonDto instance) {
            this.name = initialValue(instance.getName());
          }

          public static PersonDtoBuilder create() {
            return new PersonDtoBuilder();
          }

          public PersonDtoBuilder name(String name) {
            this.name = changedValue(name);
            return this;
          }

          public PersonDtoBuilder name(Consumer<StringBuilder> nameStringBuilderConsumer) {
            StringBuilder builder = new StringBuilder();
            nameStringBuilderConsumer.accept(builder);
            this.name = changedValue(builder.toString());
            return this;
          }

          public PersonDtoBuilder name(Supplier<String> nameSupplier) {
            this.name = changedValue(nameSupplier.get());
            return this;
          }

          public PersonDtoBuilder name(String format, Object... args) {
            this.name = changedValue(String.format(format, args));
            return this;
          }

          public PersonDtoBuilder conditional(BooleanSupplier condition, Consumer<PersonDtoBuilder> yesCondition) {
            return conditional(condition, yesCondition, null);
          }

          public PersonDtoBuilder conditional(BooleanSupplier condition, Consumer<PersonDtoBuilder> trueCase,
              Consumer<PersonDtoBuilder> falseCase) {
            if (condition.getAsBoolean()) {
              trueCase.accept(this);
            } else if (falseCase != null) {
              falseCase.accept(this);
            }
            return this;
          }

          @Override
          public PersonDto build() {
            PersonDto result = new PersonDto();
            this.name.ifSet(result::setName);
            return result;
          }

          @Override
          public String toString() {
            return new ToStringBuilder(this, BuilderToStringStyle.INSTANCE).append("name", this.name).toString();
          }

          public interface With {
            default PersonDto with(Consumer<PersonDtoBuilder> b) {
              PersonDtoBuilder builder;
              try {
                builder = new PersonDtoBuilder(PersonDto.class.cast(this));
              } catch (ClassCastException ex) {
                throw new IllegalArgumentException(
                    "The interface 'PersonDtoBuilder.With' should only be implemented by classes, which could be casted to 'PersonDto'",
                    ex);
              }
              b.accept(builder);
              return builder.build();
            }

            default PersonDtoBuilder with() {
              try {
                return new PersonDtoBuilder(PersonDto.class.cast(this));
              } catch (ClassCastException ex) {
                throw new IllegalArgumentException(
                    "The interface 'PersonDtoBuilder.With' should only be implemented by classes, which could be casted to 'PersonDto'",
                    ex);
              }
            }
          }
        }""";

    assertEquals(expectedCode, generatedCode);
  }

  /**
   * Merge logic test: Configuration merging must respect priority correctly.
   *
   * <p>Priority: other > this (for non-UNSET/DEFAULT values)
   */
  @Test
  void configurationMerge_MustRespectPriority() {
    // Given: Base configuration with some values
    BuilderConfiguration base =
        BuilderConfiguration.builder()
            .generateSupplier(OptionState.ENABLED)
            .generateConsumer(OptionState.ENABLED)
            .builderAccess(AccessModifier.PUBLIC)
            .setterSuffix("")
            .build();

    // When: Merge with override configuration
    BuilderConfiguration override =
        BuilderConfiguration.builder()
            .generateSupplier(OptionState.DISABLED) // Override
            .generateBuilderConsumer(OptionState.DISABLED) // New value
            .setterSuffix("with") // Override setterSuffix
            // generateConsumer not set, should keep base value
            .build();

    BuilderConfiguration merged = base.merge(override);

    // Then: Override values win, base values kept for unset
    assertEquals(
        OptionState.DISABLED,
        merged.generateFieldSupplier(),
        "Override should win for generateFieldSupplier");
    assertEquals(
        OptionState.ENABLED,
        merged.generateFieldConsumer(),
        "Base value should be kept when override is UNSET");
    assertEquals(
        OptionState.DISABLED, merged.generateBuilderConsumer(), "Override should set new value");
    assertEquals(
        AccessModifier.PUBLIC,
        merged.getBuilderAccess(),
        "Base value should be kept when override is DEFAULT");
    assertEquals("with", merged.getSetterSuffix(), "Override should win for setterSuffix");
  }

  /** Merge logic test for formattingMode: Annotation value must override compiler arg default. */
  @Test
  void configurationMerge_FormattingMode_MustRespectPriority() {
    // Given: Base config with no formattingMode (inherit from compiler arg)
    BuilderConfiguration base = BuilderConfiguration.builder().build();

    // When: Merge with override that sets formattingMode
    BuilderConfiguration override = BuilderConfiguration.builder().formattingMode("none").build();

    BuilderConfiguration merged = base.merge(override);

    // Then: Override should win
    assertEquals("none", merged.formattingMode(), "Override should win for formattingMode");

    // And: formattingModeEnum should return the override value
    assertEquals(
        FormattingMode.NONE,
        merged.formattingModeEnum(),
        "Annotation formattingMode should override compiler arg fallback");
  }

  /** formattingModeEnum test: Null/blank formattingMode should default to JDT. */
  @Test
  void formattingModeEnum_WhenUnset_ShouldDefaultToJdt() {
    BuilderConfiguration config = BuilderConfiguration.builder().build();

    assertEquals(
        FormattingMode.JDT,
        config.formattingModeEnum(),
        "Null formattingMode should default to JDT");
  }

  /** formattingModeEnum test: Set formattingMode should be resolved. */
  @Test
  void formattingModeEnum_WhenSet_ShouldReturnSetValue() {
    BuilderConfiguration config =
        BuilderConfiguration.builder().formattingMode("lightweight").build();

    assertEquals(
        FormattingMode.LIGHTWEIGHT,
        config.formattingModeEnum(),
        "Set formattingMode should be resolved correctly");
  }

  /**
   * toString test: Configuration must produce human-readable output.
   *
   * <p>This ensures debugging and logging shows meaningful information.
   */
  @Test
  void configurationToString_MustBeHumanReadable() {
    BuilderConfiguration config =
        BuilderConfiguration.builder()
            .generateSupplier(OptionState.DISABLED)
            .generateConsumer(OptionState.ENABLED)
            .builderAccess(AccessModifier.PRIVATE)
            .methodAccess(AccessModifier.PACKAGE_PRIVATE)
            .setterSuffix("with")
            .build();

    String configString = config.toString();

    // Verify it contains field names and values
    assertNotNull(configString, "toString should not return null");
    assertTrue(
        configString.contains("generateFieldSupplier"), "toString should mention field names");
    assertTrue(configString.contains("DISABLED"), "toString should show enum values");
    assertTrue(configString.contains("ENABLED"), "toString should show enum values");
    assertTrue(configString.contains("PRIVATE"), "toString should show AccessModifier values");
    assertTrue(
        configString.contains("PACKAGE_PRIVATE"), "toString should show AccessModifier values");
    assertTrue(
        configString.contains("setterSuffix"),
        "toString should mention setterSuffix when non-default");
    assertTrue(configString.contains("with"), "toString should show setterSuffix value");

    // Verify it's not just a hash code
    assertTrue(configString.length() > 100, "toString should be detailed, not just class@hashcode");
  }

  /** Null-safe merge test: Merging with null should return this. */
  @Test
  void configurationMerge_WithNull_ShouldReturnThis() {
    BuilderConfiguration config =
        BuilderConfiguration.builder().generateSupplier(OptionState.DISABLED).build();

    BuilderConfiguration merged = config.merge(null);

    assertEquals(config, merged, "Merging with null should return the same configuration");
  }

  /**
   * Chain merge test: Multiple merges should apply in order.
   *
   * <p>This simulates: Defaults -> Compiler Args -> Template -> Options
   */
  @Test
  void configurationMerge_Chain_ShouldApplyInOrder() {
    // Layer 1: Defaults
    BuilderConfiguration defaults = BuilderConfiguration.DEFAULT;

    // Layer 2: Compiler arguments (override some defaults)
    BuilderConfiguration compilerArgs =
        BuilderConfiguration.builder()
            .generateSupplier(OptionState.DISABLED)
            .builderAccess(AccessModifier.PACKAGE_PRIVATE)
            .build();

    // Layer 3: Template (override some compiler args)
    BuilderConfiguration template =
        BuilderConfiguration.builder().generateSupplier(OptionState.ENABLED).build();

    // Layer 4: Direct options (highest priority)
    BuilderConfiguration options =
        BuilderConfiguration.builder().generateConsumer(OptionState.DISABLED).build();

    // Apply chain: defaults -> compiler -> template -> options
    BuilderConfiguration finalConfig = defaults.merge(compilerArgs).merge(template).merge(options);

    // Verify final values
    assertEquals(
        OptionState.ENABLED,
        finalConfig.generateFieldSupplier(),
        "Template should override compiler args");
    assertEquals(
        OptionState.DISABLED,
        finalConfig.generateFieldConsumer(),
        "Options should override all others");
    assertEquals(
        AccessModifier.PACKAGE_PRIVATE,
        finalConfig.getBuilderAccess(),
        "Compiler args should override defaults");
    assertEquals(
        OptionState.ENABLED,
        finalConfig.generateConditionalHelper(),
        "Default should remain when not overridden");
  }

  /**
   * Custom builder suffix with nested DTOs test.
   *
   * <p>Verifies that when using a custom builderSuffix, the builder correctly recognizes nested DTO
   * builders with the same custom suffix.
   *
   * <p>For example, if PersonDto has an AddressDto field, and both use suffix "Factory", then
   * PersonDtoFactory should have a method accepting {@code Consumer<AddressDtoFactory>}.
   */
  @Test
  void builderSuffix_WithNestedDto_ShouldRecognizeNestedBuilder() {
    // Given: A nested DTO with @SimpleBuilder annotation
    JavaFileObject nestedDto =
        ProcessorTestUtils.simpleBuilderClass(
            "com.example",
            "AddressDto",
            """
            private String street;
            private String city;

            public String getStreet() { return street; }
            public void setStreet(String street) { this.street = street; }

            public String getCity() { return city; }
            public void setCity(String city) { this.city = city; }
            """);

    // And: A parent DTO with @SimpleBuilder annotation that has the nested DTO as a field
    JavaFileObject parentDto =
        ProcessorTestUtils.simpleBuilderClass(
            "com.example",
            "PersonDto",
            """
            private String name;
            private AddressDto address;

            public String getName() { return name; }
            public void setName(String name) { this.name = name; }

            public AddressDto getAddress() { return address; }
            public void setAddress(AddressDto address) { this.address = address; }
            """);

    // When: Compile with custom builderSuffix
    Compilation compilation =
        ProcessorTestUtils.createCompiler()
            .withOptions("-Asimplebuilder.builderSuffix=Factory")
            .compile(nestedDto, parentDto);

    // Then: Compilation should succeed
    assertThat(compilation).succeeded();

    // And: Parent builder should be generated with custom suffix
    String parentBuilderCode =
        ProcessorTestUtils.loadGeneratedSource(compilation, "PersonDtoFactory");
    assertNotNull(parentBuilderCode, "PersonDtoFactory should be generated");

    // And: Nested builder should be generated with custom suffix
    String nestedBuilderCode =
        ProcessorTestUtils.loadGeneratedSource(compilation, "AddressDtoFactory");
    assertNotNull(nestedBuilderCode, "AddressDtoFactory should be generated");

    // And: Parent builder should reference nested builder with custom suffix
    ProcessorAsserts.assertContaining(
        parentBuilderCode,
        "class PersonDtoFactory",
        "PersonDtoFactory address(AddressDto address)",
        "PersonDtoFactory address(Consumer<AddressDtoFactory> addressBuilderConsumer)");

    // And: Parent builder's create method should use custom suffix
    ProcessorAsserts.assertContaining(parentBuilderCode, "static PersonDtoFactory create()");

    // And: Nested builder should also use custom suffix in its methods
    ProcessorAsserts.assertContaining(
        nestedBuilderCode,
        "class AddressDtoFactory",
        "AddressDtoFactory street(String street)",
        "AddressDtoFactory city(String city)",
        "static AddressDtoFactory create()");
  }
}
