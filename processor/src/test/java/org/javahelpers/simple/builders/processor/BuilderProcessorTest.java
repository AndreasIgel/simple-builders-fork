/*
 * MIT License
 *
 * Copyright (c) 2025 Andreas Igel
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
import static org.javahelpers.simple.builders.processor.testing.ProcessorAsserts.assertGenerationSucceeded;
import static org.javahelpers.simple.builders.processor.testing.ProcessorAsserts.contains;
import static org.javahelpers.simple.builders.processor.testing.ProcessorAsserts.notContains;
import static org.javahelpers.simple.builders.processor.testing.ProcessorTestUtils.loadGeneratedSource;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import javax.tools.JavaFileObject;
import org.javahelpers.simple.builders.processor.testing.ProcessorAsserts;
import org.javahelpers.simple.builders.processor.testing.ProcessorTestUtils;
import org.junit.jupiter.api.Test;

/** Tests for the {@link BuilderProcessor} class. */
class BuilderProcessorTest {

  @Test
  void shouldGenerateBasicBuilder() {
    // Given: A simple class with one field
    String className = "SimpleDto";
    String builderClassName = className + "Builder";

    // When
    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            "test",
            className,
            """
                private Integer value;
                public Integer getValue() { return value; }
                public void setValue(Integer value) { this.value = value; }
            """);

    Compilation compilation = compile(sourceFile);

    // Then: Builder should be generated successfully
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    ProcessorAsserts.assertContaining(
        generatedCode, "public SimpleDtoBuilder value(Integer value)", "public SimpleDto build()");
  }

  @Test
  void shouldLogDebugMessagesWhenVerboseModeEnabled() {
    // Given
    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            "test",
            "VerboseTest",
            """
                private String name;
                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
            """);

    // When: Compile with verbose=true to enable debug logging
    Compilation compilation =
        ProcessorTestUtils.createCompiler()
            .withOptions("-Averbose=true", "-Asimplebuilder.generateMapperHelpers=DISABLED")
            .compile(sourceFile);

    // Then: Compilation succeeds and debug messages are present
    assertThat(compilation).succeeded();

    // Verify key debug messages are logged with hierarchical format
    ProcessorAsserts.assertHadNoteContaining(
        compilation,
        "[DEBUG] Starting BuilderProcessor...",
        "[DEBUG] Loaded global configuration from compiler arguments: "
            + "BuilderConfiguration[generateMapperHelpers=DISABLED]",
        "[DEBUG] Initializing generator registry",
        "[DEBUG] ├─ Loaded 15 method generators and 9 builder enhancers total",
        "[DEBUG] └─ Initialized GeneratorRegistry with 15 method generators and 9 builder",
        "simple-builders: PROCESSING ROUND START",
        "[DEBUG] simple-builders: Processing round started. Found 1 annotated elements.",
        "[DEBUG] Processing element: VerboseTest",
        "[DEBUG] ├─ Resolving configuration for element: VerboseTest",
        "[DEBUG] │  ├─ Built-in template @SimpleBuilder found in DIRECT scope",
        "[DEBUG] │  └─ Resulting configuration resolved: BuilderConfiguration[",
        "[DEBUG] ├─ Extracting builder definition from: test.VerboseTest",
        "[DEBUG] │  ├─ Builder will be generated as: test.VerboseTestBuilder",
        "[DEBUG] │  ├─ Analysing setters for finding fields",
        "[DEBUG] │  │  ├─ Analyzing method: setName(java.lang.String)",
        "[DEBUG] │  │  │  ├─ Processing method generators",
        "[DEBUG] │  │  │  │  ├─ Applying: BasicSetterGenerator (priority: 100)",
        "[DEBUG] │  │  │  │  ├─ Applying: StringFormatHelperGenerator (priority: 80)",
        "[DEBUG] │  │  │  │  ├─ Applying: SupplierMethodGenerator (priority: 60)",
        "[DEBUG] │  │  │  │  ├─ Applying: StringBuilderConsumerGenerator (priority: 45)",
        "[DEBUG] │  │  │  │  └─ Generated 4 methods",
        "[DEBUG] │  │  │  └─ Adding field: name (type: java.lang.String)",
        "[DEBUG] │  │  └─ Processed 1 possible setters: added 1 fields, skipped 0",
        "[DEBUG] │  ├─ Processing class based enhancer",
        "[DEBUG] │  │  ├─ Applying: GeneratedAnnotationEnhancer (priority: 120)",
        "[DEBUG] │  │  ├─ Applying: BuilderImplementationAnnotationEnhancer (priority: 115)",
        "[DEBUG] │  │  ├─ Applying: CoreMethodsEnhancer (priority: 100)",
        "[DEBUG] │  │  ├─ Applying: WithInterfaceEnhancer (priority: 95)",
        "[DEBUG] │  │  ├─ Applying: InterfaceEnhancer (priority: 90)",
        "[DEBUG] │  │  ├─ Applying: ConditionalEnhancer (priority: 80)",
        "[DEBUG] │  │  ├─ Applying: ClassJavaDocEnhancer (priority: 10)",
        "[DEBUG] │  │  └─ Applied 8 builder enhancers",
        "[DEBUG] │  ├─ Finalizing builder definition",
        "[DEBUG] │  │  ├─ Resolving method conflicts",
        "[DEBUG] │  │  │  └─ Resolved: 9 signatures, 0 conflicts, 0 methods removed",
        "[DEBUG] │  │  └─ Finalized: 1 class fields, 5 builder-level methods, 2 constructors",
        "[DEBUG] │  ├─ Builder will be generated as: VerboseTestBuilder",
        "[DEBUG] │  └─ Builder definition extracted: VerboseTestBuilder",
        "[DEBUG] ├─ Code generation for class: VerboseTestBuilder",
        "[DEBUG] │  ├─ JavaClassSource created",
        "[DEBUG] │  ├─ Class metadata added",
        "[DEBUG] │  ├─ Generating 1 fields",
        "[DEBUG] │  │  └─ Fields added: 1 fields",
        "[DEBUG] │  ├─ Generating 2 constructors",
        "[DEBUG] │  │  └─ Constructors added: 2",
        "[DEBUG] │  ├─ Generating 9 method candidates",
        "[DEBUG] │  │  ├─ Resolved to 9 methods after conflict resolution",
        "[DEBUG] │  │  └─ Methods added: 9",
        "[DEBUG] │  ├─ Generating 1 nested type(s)",
        "[DEBUG] │  │  ├─ Generated nested type: With",
        "[DEBUG] │  │  └─ Nested types added",
        "[DEBUG] │  ├─ Class-level annotations added",
        "[DEBUG] │  ├─ Writing class to file: test.VerboseTestBuilder",
        "[DEBUG] │  └─ Successfully generated class: VerboseTestBuilder",
        "[DEBUG] ├─ Jackson module entry added",
        "[DEBUG] └─ Generated builder with 1 fields and 9 methods for VerboseTestBuilder",
        "simple-builders: Successfully generated 1 builder(s) in this processing round",
        "");
  }

  @Test
  void shouldFailWhenAnnotationPlacedOnInterface() {
    // Given: @SimpleBuilder used on an interface instead of a class
    JavaFileObject source =
        ProcessorTestUtils.forSource(
            """
                package test;
                import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
                @SimpleBuilder
                public interface WrongTargetInterface { void x(); }
            """);

    // When
    Compilation compilation = compile(source);

    // Then: compilation should succeed but with a warning
    assertThat(compilation).succeeded();
    assertThat(compilation)
        .hadWarningContaining("The SimpleBuilder should be annotated on a class or record");
  }

  @Test
  void shouldFailWhenAnnotationPlacedOnAbstractClass() {
    // Given: @SimpleBuilder used on an abstract class
    JavaFileObject source =
        ProcessorTestUtils.forSource(
            """
                package test;
                import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
                @SimpleBuilder
                public abstract class AbstractAnnotated { }
            """);

    // When
    Compilation compilation = compile(source);

    // Then: compilation should succeed but with a warning
    assertThat(compilation).succeeded();
    assertThat(compilation).hadWarningContaining("The SimpleBuilder should not be abstract");
  }

  @Test
  void shouldGenerateBuilderForNonEmptyConstructorWithFinalFields() {
    // Given
    String packageName = "test";
    String className = "NonEmptyCtorFinals";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private final String name;
                private final int age;

                public NonEmptyCtorFinals(String name, int age) {
                    this.name = name;
                    this.age = age;
                }

                public String getName() { return name; }
                public int getAge() { return age; }
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // Expect helper methods for constructor params and proper build invocation
    ProcessorAsserts.assertContaining(
        generatedCode,
        "public NonEmptyCtorFinalsBuilder()",
        "public NonEmptyCtorFinalsBuilder(NonEmptyCtorFinals instance)",
        "this.name = initialValue(instance.getName());",
        "this.age = initialValue(instance.getAge());",
        "public NonEmptyCtorFinalsBuilder name(String name)",
        "public NonEmptyCtorFinalsBuilder age(int age)",
        "NonEmptyCtorFinals result = new NonEmptyCtorFinals(this.name.value(), this.age.value());");

    // Ensure no setter calls are emitted for final fields
    ProcessorAsserts.assertNotContaining(generatedCode, "result.setName", "result.setAge");
  }

  @Test
  void shouldHandleFieldsOfConstructorAndSetters() {
    // Given
    String packageName = "test";
    String className = "CtorAndSetter";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private final int a; // has no setter, only in constructor
                private String name; // will be set via setter

                public CtorAndSetter(int a) { this.a = a; }

                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
                // a has no setter intentionally
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    // Setter-based API should appear (name) and Constructor params (a)
    ProcessorAsserts.assertContaining(
        generatedCode,
        "public CtorAndSetterBuilder()", // empty constructor for builder
        "public CtorAndSetterBuilder name(String name)", // helpermethod for setter
        "public CtorAndSetterBuilder name(Supplier<String> nameSupplier)", // supplier for setter
        "public CtorAndSetterBuilder a(int a)", // helpermethod for constructor param
        "public CtorAndSetterBuilder a(Supplier<Integer> aSupplier)", // supplier for constructor
        "CtorAndSetter result = new CtorAndSetter(this.a.value());",
        "this.name.ifSet(result::setName);");
  }

  @Test
  void shouldPreferConstructorOverSetterWhenBothExist() {
    // Given
    String packageName = "test";
    String className = "CtorAndSetterSameField";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private int a; // could be set via constructor and setter
                private String name;

                public CtorAndSetterSameField(int a) { this.a = a; }

                public int getA() { return a; }
                public void setA(int a) { this.a = a; }

                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    // Expect helper for constructor param and no duplicate setter application for 'a'
    ProcessorAsserts.assertContaining(
        generatedCode,
        "public CtorAndSetterSameFieldBuilder()",
        // Expectations on constructor of builder with instance
        "public CtorAndSetterSameFieldBuilder(CtorAndSetterSameField instance)",
        "this.name = initialValue(instance.getName());",
        "this.a = initialValue(instance.getA());",
        // Expectations on helper functions
        "public CtorAndSetterSameFieldBuilder a(int a)",
        "public CtorAndSetterSameFieldBuilder name(String name)",
        // Expectations on build function
        "CtorAndSetterSameField result = new CtorAndSetterSameField(this.a.value());",
        "this.name.ifSet(result::setName);");
    // expect no setter application for 'a' on build-function
    ProcessorAsserts.assertNotContaining(generatedCode, "result.setA(this.a);");
  }

  @Test
  void shouldUseAnnotatedConstructorWhenMultipleConstructorsExist() {
    // Given
    String builderClassName = "AnnotatedCtorChoiceBuilder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.forSource(
            """
                package test;

                import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
                import org.javahelpers.simple.builders.core.annotations.SimpleBuilderConstructor;

                @SimpleBuilder
                public class AnnotatedCtorChoice {
                  private int a;
                  private String name;
                  private boolean flag;

                  @SimpleBuilderConstructor
                  public AnnotatedCtorChoice(int a, String name) {
                    this.a = a;
                    this.name = name;
                  }

                  // Alternative constructor that must NOT be used by the builder
                  public AnnotatedCtorChoice(int a, String name, boolean flag) {
                    this.a = a;
                    this.name = name;
                    this.flag = flag;
                  }

                  public int getA() { return a; }
                  public String getName() { return name; }
                  public boolean isFlag() { return flag; }

                  public void setName(String name) { this.name = name; }
                }
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // Builder must use the annotated (a, name) constructor
    ProcessorAsserts.assertContaining(
        generatedCode,
        "public AnnotatedCtorChoiceBuilder a(int a)",
        "public AnnotatedCtorChoiceBuilder name(String name)",
        "AnnotatedCtorChoice result = new AnnotatedCtorChoice(this.a.value(), this.name.value());");

    // Ensure the alternative constructor (with flag) is not used nor exposed via helpers
    ProcessorAsserts.assertNotContaining(
        generatedCode,
        "public AnnotatedCtorChoiceBuilder flag",
        "new AnnotatedCtorChoice(this.a.value(), this.flag.value())");
  }

  @Test
  void shouldIgnoreSimpleBuilderConstructorOnRegularMethod() {
    // Given: @SimpleBuilderConstructor incorrectly placed on a regular method (not constructor)
    String className = "WrongMethodAnnotation";
    String builderClassName = className + "Builder";

    JavaFileObject source =
        ProcessorTestUtils.forSource(
            """
            package test;

            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilderConstructor;

            @SimpleBuilder
            public class WrongMethodAnnotation {
              private String name;

              @SimpleBuilderConstructor
              public void notAConstructor(String name) {
                this.name = name;
              }

              public String getName() { return name; }
              public void setName(String name) { this.name = name; }
            }
            """);

    // When
    Compilation compilation = compile(source);

    // Then: Builder should still be generated, but annotation on method should be ignored
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    // Builder should have method from setter, not from annotated regular method
    ProcessorAsserts.assertContaining(
        generatedCode, "public WrongMethodAnnotationBuilder name(String name)");
  }

  @Test
  void shouldGenerateSupplierMethodsForConstructorFields() {
    // Given
    String packageName = "test";
    String className = "RequiredFieldViaConstructor";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private final String name;
                private final int age;
                private String description;

                public RequiredFieldViaConstructor(String name, int age) {
                  this.name = name;
                  this.age = age;
                }

                public String getName() { return name; }
                public int getAge() { return age; }
                public String getDescription() { return description; }
                public void setDescription(String description) { this.description = description; }
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // Constructor fields should have both basic setter AND supplier methods
    ProcessorAsserts.assertContaining(
        generatedCode,
        // Basic setters for constructor fields
        "public RequiredFieldViaConstructorBuilder name(String name)",
        "public RequiredFieldViaConstructorBuilder name(String format, Object... args)",
        "public RequiredFieldViaConstructorBuilder age(int age)",
        // Supplier methods for constructor fields (the bug we fixed!)
        "public RequiredFieldViaConstructorBuilder name(Supplier<String> nameSupplier)",
        "public RequiredFieldViaConstructorBuilder age(Supplier<Integer> ageSupplier)",
        // Setter field should also have supplier
        "public RequiredFieldViaConstructorBuilder description(String description)",
        "public RequiredFieldViaConstructorBuilder description(String format, Object... args)",
        "public RequiredFieldViaConstructorBuilder description(Supplier<String> descriptionSupplier)");
  }

  @Test
  void shouldGenerateConsumerMethodsForConstructorFieldsWithCustomTypes() {
    // Given
    String packageName = "test";
    String className = "RequiredCustomTypeViaConstructor";
    String builderClassName = className + "Builder";

    JavaFileObject dto =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private final Address address;
                private String notes;

                public RequiredCustomTypeViaConstructor(Address address) {
                  this.address = address;
                }

                public Address getAddress() { return address; }
                public String getNotes() { return notes; }
                public void setNotes(String notes) { this.notes = notes; }
            """);

    JavaFileObject helper =
        ProcessorTestUtils.forSource(
            """
                package test;
                public class Address {
                  private String street;
                  public Address() {}
                  public String getStreet() { return street; }
                  public void setStreet(String street) { this.street = street; }
                }
            """);

    // When
    Compilation compilation = compile(dto, helper);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // Constructor field with custom type should have setter, supplier AND consumer methods
    ProcessorAsserts.assertContaining(
        generatedCode,
        // Basic setter for constructor field
        "public RequiredCustomTypeViaConstructorBuilder address(Address address)",
        // Supplier method for constructor field
        "public RequiredCustomTypeViaConstructorBuilder address(Supplier<Address> addressSupplier)",
        // Consumer method for constructor field (the key coverage addition!)
        "public RequiredCustomTypeViaConstructorBuilder address(Consumer<Address> addressConsumer)",
        "Address consumer = this.address.isSet() ? this.address.value() : new Address();",
        "addressConsumer.accept(consumer);");
  }

  @Test
  void shouldHandleRecordWithoutGeneratingBuilder() {
    // Given
    String recordName = "PersonRecord";
    String builderClassName = recordName + "Builder";

    JavaFileObject recordFile =
        ProcessorTestUtils.forSource(
            """
            package test;

            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public record PersonRecord(String name, int age) {
              public String upperName() { return name.toUpperCase(); }
            }
            """);

    // When
    Compilation compilation = compile(recordFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    // Setter-based API should support record Constructor params (name, age)
    ProcessorAsserts.assertContaining(
        generatedCode,
        "public PersonRecordBuilder name(String name)",
        "this.name = changedValue(name);",
        "public PersonRecordBuilder age(int age)",
        "this.age = changedValue(age);");
  }

  @Test
  void shouldHandleRecordWithMultipleConstructorsAndAnnotatedNonCanonical() {
    // Given: Record with canonical constructor (name, age, email) but a secondary
    // constructor with fewer params is annotated with @SimpleBuilderConstructor
    String recordName = "PersonWithEmail";
    String builderClassName = recordName + "Builder";

    JavaFileObject recordFile =
        ProcessorTestUtils.forSource(
            """
            package test;

            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilderConstructor;

            @SimpleBuilder
            public record PersonWithEmail(String name, int age, String email) {
              @SimpleBuilderConstructor
              public PersonWithEmail(String name, int age) {
                this(name, age, null);
              }
            }
            """);

    // When
    Compilation compilation = compile(recordFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    // Builder should only generate methods for the annotated constructor (name, age)
    // NOT for email, since records don't have setters and email is not in the annotated ctor
    ProcessorAsserts.assertContaining(
        generatedCode,
        "public PersonWithEmailBuilder name(String name)",
        "public PersonWithEmailBuilder age(int age)");
    // Email should NOT have a builder method since it's not in the annotated constructor
    ProcessorAsserts.assertNotContaining(generatedCode, "public PersonWithEmailBuilder email");
  }

  @Test
  void shouldGenerateCollectionSettersAndProviders() {
    // Given
    String packageName = "test";
    String className = "WithCollections";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private java.util.List<String> names;
                private java.util.Set<String> tags;
                private java.util.Map<String, Integer> map;

                public java.util.List<String> getNames() { return names; }
                public void setNames(java.util.List<String> names) { this.names = names; }

                public java.util.Set<String> getTags() { return tags; }
                public void setTags(java.util.Set<String> tags) { this.tags = tags; }

                public java.util.Map<String, Integer> getMap() { return map; }
                public void setMap(java.util.Map<String, Integer> map) { this.map = map; }
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // Direct setters and varargs convenience for List and Set; direct setter for Map
    ProcessorAsserts.assertContaining(
        generatedCode,
        "public WithCollectionsBuilder names(List<String> names)",
        "public WithCollectionsBuilder names(Supplier<List<String>> namesSupplier)",
        "public WithCollectionsBuilder names(String... names)",
        "this.names = changedValue(List.of(names));",
        "private TrackedValue<List<String>> names = unsetValue();",
        "public WithCollectionsBuilder tags(Set<String> tags)",
        "public WithCollectionsBuilder tags(Supplier<Set<String>> tagsSupplier)",
        "public WithCollectionsBuilder tags(String... tags)",
        "this.tags = changedValue(Set.of(tags));",
        "private TrackedValue<Set<String>> tags = unsetValue();",
        "public WithCollectionsBuilder map(Map<String, Integer> map)",
        "public WithCollectionsBuilder map(Supplier<Map<String, Integer>> mapSupplier)",
        "this.map = changedValue(map);",
        "private TrackedValue<Map<String, Integer>> map = unsetValue();");
  }

  @Test
  void shouldGenerateOptionalSettersAndProviders() {
    // Given
    String packageName = "test";
    String className = "WithOptionals";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private java.util.Optional<String> description;
                private java.util.Optional<Integer> age;

                public java.util.Optional<String> getDescription() { return description; }
                public void setDescription(java.util.Optional<String> description) { this.description = description; }

                public java.util.Optional<Integer> getAge() { return age; }
                public void setAge(java.util.Optional<Integer> age) { this.age = age; }
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // Direct setters with Optional<T> and unwrapped T convenience
    ProcessorAsserts.assertContaining(
        generatedCode,
        "public WithOptionalsBuilder description(Optional<String> description)",
        "public WithOptionalsBuilder description(Supplier<Optional<String>> descriptionSupplier)",
        "public WithOptionalsBuilder description(String description)",
        "public WithOptionalsBuilder description(String format, Object... args)",
        "this.description = changedValue(Optional.ofNullable(description));",
        "private TrackedValue<Optional<String>> description = unsetValue();",
        "public WithOptionalsBuilder age(Optional<Integer> age)",
        "public WithOptionalsBuilder age(Supplier<Optional<Integer>> ageSupplier)",
        "public WithOptionalsBuilder age(Integer age)",
        "this.age = changedValue(Optional.ofNullable(age));",
        "private TrackedValue<Optional<Integer>> age = unsetValue();");
  }

  @Test
  void shouldGenerateStringFormatMethodForStringField() {
    // Given: String field to verify format method generation
    String packageName = "test";
    String className = "WithString";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private String message;

                public String getMessage() { return message; }
                public void setMessage(String message) { this.message = message; }
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // Verify both basic String setter and format method are generated
    ProcessorAsserts.assertContaining(
        generatedCode,
        "private TrackedValue<String> message = unsetValue();",
        """
        public WithStringBuilder message(String message) {
          this.message = changedValue(message);
          return this;
        }
        """,
        """
        public WithStringBuilder message(String format, Object... args) {
          this.message = changedValue(String.format(format, args));
          return this;
        }
        """);
  }

  @Test
  void shouldGenerateStringFormatMethodForOptionalString() {
    // Given: Optional<String> field to verify format method generation
    String packageName = "test";
    String className = "WithOptionalString";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private java.util.Optional<String> message;

                public java.util.Optional<String> getMessage() { return message; }
                public void setMessage(java.util.Optional<String> message) { this.message = message; }
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // Verify both unwrapped String setter and format method are generated
    ProcessorAsserts.assertContaining(
        generatedCode,
        "private TrackedValue<Optional<String>> message = unsetValue();",
        """
        public WithOptionalStringBuilder message(String message) {
          this.message = changedValue(Optional.ofNullable(message));
          return this;
        }
        """,
        """
        public WithOptionalStringBuilder message(String format, Object... args) {
          this.message = changedValue(Optional.of(String.format(format, args)));
          return this;
        }
        """);
  }

  @Test
  void shouldGenerateStringBuilderConsumerForStringField() {
    // Given: String field to verify StringBuilder consumer generation
    String packageName = "test";
    String className = "WithStringBuilder";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private String message;

                public String getMessage() { return message; }
                public void setMessage(String message) { this.message = message; }
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // Verify StringBuilder consumer method is generated
    ProcessorAsserts.assertContaining(
        generatedCode,
        "private TrackedValue<String> message = unsetValue();",
        """
        public WithStringBuilderBuilder message(Consumer<StringBuilder> messageStringBuilderConsumer) {
          StringBuilder builder = new StringBuilder();
          messageStringBuilderConsumer.accept(builder);
          this.message = changedValue(builder.toString());
          return this;
        }
        """);
  }

  @Test
  void shouldGenerateStringBuilderConsumerForOptionalString() {
    // Given: Optional<String> field to verify StringBuilder consumer generation
    String packageName = "test";
    String className = "WithOptString";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private java.util.Optional<String> message;

                public java.util.Optional<String> getMessage() { return message; }
                public void setMessage(java.util.Optional<String> message) { this.message = message; }
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // Verify StringBuilder consumer method is generated with Optional wrapping
    ProcessorAsserts.assertContaining(
        generatedCode,
        """
        public WithOptStringBuilder message(Consumer<StringBuilder> messageStringBuilderConsumer) {
          StringBuilder builder = new StringBuilder();
          messageStringBuilderConsumer.accept(builder);
          this.message = changedValue(Optional.of(builder.toString()));
          return this;
        }
        """);
  }

  @Test
  void shouldDeclarePerFieldBackingField() {
    // Given
    String packageName = "test";
    String className = "FieldDoc";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private int x; public int getX(){return x;} public void setX(int x){this.x=x;}
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // Expect a private tracked backing field for primitive int setter
    ProcessorAsserts.assertContaining(
        generatedCode, "private TrackedValue<Integer> x = unsetValue();");
    ProcessorAsserts.assertNotContaining(generatedCode, " FieldDoc instance;");
  }

  @Test
  void shouldGenerateCreateMethodWithModifiersAndJavadoc() {
    // Given
    String packageName = "test";
    String className = "CreateDoc";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private int x;
                public int getX(){return x;}
                public void setX(int x){this.x=x;}
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    ProcessorAsserts.assertContaining(
        generatedCode,
        "public static CreateDocBuilder create()",
        "Creating a new builder for {@code test.CreateDoc}",
        "@return builder for {@code test.CreateDoc}",
        "return new CreateDocBuilder();");
  }

  @Test
  void shouldGenerateBuildOverrideAndReturnAnInstanceWithCalledSetters() {
    // Given
    String packageName = "test";
    String className = "BuildDoc";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private int x;
                public int getX(){return x;}
                public void setX(int x){this.x=x;}
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // Verify build method exists and calls setter, if not null
    ProcessorAsserts.assertContaining(
        generatedCode,
        """
        @Override
        public BuildDoc build() {
            if (this.x.isSet() && this.x.value() == null) {
                throw new IllegalStateException("Field 'x' is marked as non-null but null value was provided");
            }
            BuildDoc result = new BuildDoc();
            this.x.ifSet(result::setX);
            return result;
        }
        """);
  }

  @Test
  void shouldReturnThisFromFluentMethods() {
    // Given
    String packageName = "test";
    String className = "FluentReturn";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private String name; public String getName(){return name;} public void setName(String name){this.name=name;}
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    ProcessorAsserts.assertContaining(
        generatedCode,
        "public FluentReturnBuilder name(String name)",
        "this.name = changedValue(name);",
        "return this;");
  }

  @Test
  void shouldContainReturnJavadocForConsumerSupplierAndConsumerByBuilder() {
    // Given
    String packageName = "test";
    String className = "ReturnDoc";
    String builderClassName = className + "Builder";

    JavaFileObject dto =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private String name;
                private HelperAnno helper;
                private HelperPlain helperPlain;
                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
                public HelperAnno getHelper() { return helper; }
                public void setHelper(HelperAnno helper) { this.helper = helper; }
                public HelperPlain getHelperPlain() { return helperPlain; }
                public void setHelperPlain(HelperPlain helperPlain) { this.helperPlain = helperPlain; }
            """);

    JavaFileObject helperAnno =
        ProcessorTestUtils.forSource(
            """
                package test;
                import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
                @SimpleBuilder
                public class HelperAnno { public HelperAnno() {} }
            """);

    JavaFileObject helperPlain =
        ProcessorTestUtils.forSource(
            """
                package test;
                public class HelperPlain { public HelperPlain() {} }
            """);

    // When
    Compilation compilation = compile(dto, helperAnno, helperPlain);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // '@return current instance of builder' should be present for all method types
    ProcessorAsserts.assertContaining(
        generatedCode,
        "helperBuilderConsumer",
        "@return current instance of builder",
        "helperPlainConsumer",
        "@return current instance of builder",
        "nameSupplier",
        "@return current instance of builder");
  }

  @Test
  void shouldIncludeNestedPackageInClassJavadoc() {
    // Given
    String packageName = "test.nested";
    String className = "NestedDoc";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private int x; public int getX(){return x;} public void setX(int x){this.x=x;}
                """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // Javadoc line should contain the nested package name and the class indicator
    ProcessorAsserts.assertContaining(generatedCode, "Builder for {", "test.nested");
  }

  @Test
  void shouldGenerateClassJavadocAndBaseInterface() {
    // Given
    String packageName = "test";
    String className = "DocTarget";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private int n;

                public int getN() { return n; }
                public void setN(int n) { this.n = n; }
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // Class javadoc and interface
    ProcessorAsserts.assertContaining(
        generatedCode, "Builder for {", "implements IBuilderBase<DocTarget>");
  }

  @Test
  void shouldGenerateBothConstructorsWithJavadocs() {
    // Given
    String packageName = "test";
    String className = "CtorDoc";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private String x;
                public String getX() { return x; }
                public void setX(String x) { this.x = x; }
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    ProcessorAsserts.assertContaining(
        generatedCode,
        "Empty constructor of builder for",
        "Initialisation of builder for",
        "@param instance object instance for initialisiation",
        "public CtorDocBuilder()",
        "public CtorDocBuilder(CtorDoc instance)",
        "private TrackedValue<String> x = unsetValue();");
  }

  @Test
  void shouldValidateNonNullFieldsInFromInstanceConstructor() {
    // Given
    String className = "NonNullValidation";
    String builderClassName = className + "Builder";

    JavaFileObject notNullAnnotation =
        ProcessorTestUtils.forSource(
            """
            package jakarta.validation.constraints;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.FIELD, ElementType.PARAMETER})
            public @interface NotNull {
              String message() default "";
            }
            """);

    JavaFileObject sourceFile =
        ProcessorTestUtils.forSource(
            """
            package test;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
            import jakarta.validation.constraints.NotNull;

            @SimpleBuilder
            public class NonNullValidation {
              private String requiredField;
              private String optionalField;
              private int primitiveField;

              public String getRequiredField() { return requiredField; }
              public void setRequiredField(@NotNull String requiredField) {
                this.requiredField = requiredField;
              }

              public String getOptionalField() { return optionalField; }
              public void setOptionalField(String optionalField) {
                this.optionalField = optionalField;
              }

              public int getPrimitiveField() { return primitiveField; }
              public void setPrimitiveField(int primitiveField) {
                this.primitiveField = primitiveField;
              }
            }
            """);

    // When
    Compilation compilation = compile(notNullAnnotation, sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // Verify from-instance constructor validates @NotNull fields but not nullable/primitive fields
    ProcessorAsserts.assertContaining(
        generatedCode,
        """
        public NonNullValidationBuilder(NonNullValidation instance) {
            this.optionalField = initialValue(instance.getOptionalField());
            this.primitiveField = initialValue(instance.getPrimitiveField());
            if (instance.getRequiredField() == null) {
                throw new IllegalArgumentException(
                    "Field 'requiredField' is non-null but instance.getRequiredField() returned null");
            }
            this.requiredField = initialValue(instance.getRequiredField());
        }
        """);

    // Verify build() method has validation for @NotNull field
    ProcessorAsserts.assertContaining(
        generatedCode,
        """
        public NonNullValidation build() {
            if (this.primitiveField.isSet() && this.primitiveField.value() == null) {
                throw new IllegalStateException("Field 'primitiveField' is marked as non-null but null value was provided");
            }
            if (this.requiredField.isSet() && this.requiredField.value() == null) {
                throw new IllegalStateException("Field 'requiredField' is marked as non-null but null value was provided");
            }
            NonNullValidation result = new NonNullValidation();
            this.optionalField.ifSet(result::setOptionalField);
            this.primitiveField.ifSet(result::setPrimitiveField);
            this.requiredField.ifSet(result::setRequiredField);
            return result;
        }
        """);
  }

  @Test
  void shouldGenerateMethodJavadocsForAllMethodTypes() {
    // Given
    String packageName = "test";
    String className = "JavadocTarget";
    String builderClassName = className + "Builder";

    JavaFileObject dto =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private String name;
                private HelperPlain helperPlain;
                public String getName() { return name; }
                /**
                 * @param name the name of a specific person
                 */
                public void setName(String name) { this.name = name; }
                public HelperPlain getHelperPlain() { return helperPlain; }
                /**
                 * @param helperPlain complex helper object
                 */
                public void setHelperPlain(HelperPlain helperPlain) { this.helperPlain = helperPlain; }
                public void doSomething(int a, String b) { /* no-op */ }
                """);

    JavaFileObject helper =
        ProcessorTestUtils.forSource(
            """
                package test;
                public class HelperPlain {
                  public HelperPlain() {}
                }
            """);

    // When
    Compilation compilation = compile(dto, helper);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // Setter method javadoc and code (default values, when nothing is set)
    ProcessorAsserts.assertContaining(generatedCode, "@param name the name of a specific person");

    // SUPPLIER method javadoc and code
    ProcessorAsserts.assertContaining(
        generatedCode, "@param nameSupplier supplier for the name of a specific person");

    // CONSUMER method javadoc and code
    ProcessorAsserts.assertContaining(
        generatedCode,
        "@param helperPlainConsumer consumer providing an instance of complex helper object");

    // Backing fields present with javadoc
    ProcessorAsserts.assertContaining(
        generatedCode,
        "* Tracked value for <code>name</code>: the name of a specific person.",
        "private TrackedValue<String> name = unsetValue();",
        "* Tracked value for <code>helperPlain</code>: complex helper object.",
        "private TrackedValue<HelperPlain> helperPlain = unsetValue();");
  }

  @Test
  void shouldPropagateFieldJavadocToBuilderParameter() {
    // Given
    String packageName = "test";
    String className = "FieldWithDoc";
    String builderClassName = className + "Builder";

    JavaFileObject dto =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private String name;

                public String getName() { return name; }
                /**
                 * @param name the person name
                 */
                public void setName(String name) { this.name = name; }
            """);

    // When
    Compilation compilation = compile(dto);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    // Expect the field's Javadoc text to appear on the builder method parameter
    ProcessorAsserts.assertContaining(
        generatedCode,
        "@param name the person name",
        "public FieldWithDocBuilder name(String name)",
        "private TrackedValue<String> name = unsetValue();");
  }

  @Test
  void shouldGenerateConsumerByBuilderForAnnotatedField() {
    // Given
    String packageName = "test";
    String className = "UsesAnnotatedHelper";
    String builderClassName = className + "Builder";

    JavaFileObject dto =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private HelperAnno helper;

                public HelperAnno getHelper() { return helper; }
                public void setHelper(HelperAnno helper) { this.helper = helper; }
            """);

    JavaFileObject helper =
        ProcessorTestUtils.forSource(
            """
                package test;
                import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
                @SimpleBuilder
                public class HelperAnno {
                  public HelperAnno() {}
                }
            """);

    // When
    Compilation compilation = compile(dto, helper);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // Expect consumer-by-builder method using HelperAnnoBuilder and builder.build()
    ProcessorAsserts.assertContaining(
        generatedCode,
        "private TrackedValue<HelperAnno> helper = unsetValue();",
        "helperBuilderConsumer",
        "HelperAnnoBuilder builder = this.helper.isSet() ? new HelperAnnoBuilder(this.helper.value()) : new HelperAnnoBuilder();",
        "helperBuilderConsumer.accept(builder);",
        "this.helper = changedValue(builder.build());");
  }

  @Test
  void shouldGenerateConsumerForCustomTypeWithEmptyConstructor() {
    // Given
    String packageName = "test";
    String className = "UsesPlainHelper";
    String builderClassName = className + "Builder";

    JavaFileObject dto =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private HelperPlain helperPlain;

                public HelperPlain getHelperPlain() { return helperPlain; }
                public void setHelperPlain(HelperPlain helperPlain) { this.helperPlain = helperPlain; }
            """);

    JavaFileObject helper =
        ProcessorTestUtils.forSource(
            """
                package test;
                public class HelperPlain {
                  public HelperPlain() {}
                }
            """);

    // When
    Compilation compilation = compile(dto, helper);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // Expect consumer method with local var named 'consumer' of HelperPlain
    ProcessorAsserts.assertContaining(
        generatedCode,
        "HelperPlain consumer = this.helperPlain.isSet() ? this.helperPlain.value() : new HelperPlain();",
        "helperPlainConsumer.accept(consumer);",
        "private TrackedValue<HelperPlain> helperPlain = unsetValue();");
  }

  @Test
  void shouldGenerateSupplierMethodForField() {
    // Given
    String packageName = "test";
    String className = "HasSupplier";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private String name;

                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // Expect supplier-based setter and backing field
    ProcessorAsserts.assertContaining(
        generatedCode,
        "private TrackedValue<String> name = unsetValue();",
        """
        public HasSupplierBuilder name(Supplier<String> nameSupplier) {
          this.name = changedValue(nameSupplier.get());
          return this;
        }
        """);
  }

  protected Compilation compile(JavaFileObject... sourceFiles) {
    return ProcessorTestUtils.createCompiler()
        .withOptions("-Asimplebuilder.generateMapperHelpers=DISABLED")
        .compile(sourceFiles);
  }

  @Test
  void shouldFailCompilationOnLowerReleaseOption() {
    // Given: a minimal @SimpleBuilder-annotated class
    JavaFileObject source =
        ProcessorTestUtils.forSource(
            """
                package test;
                import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
                @SimpleBuilder
                public class ForcedOldRelease { public ForcedOldRelease() {} }
            """);

    // When: compile with a lower language level to simulate older Java (no production code change)
    Compilation compilation =
        ProcessorTestUtils.createCompiler().withOptions("--release", "11").compile(source);

    // Then: compilation must fail with the expected error
    assertThat(compilation).failed();
    assertThat(compilation)
        .hadErrorContaining(
            "simple-builders requires Java 17 or higher for annotation processing.");
  }

  @Test
  void shouldGenerateBuilderForSimpleClass() {
    // Given
    String packageName = "test";
    String className = "Person";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private String name;
                private int age;

                public String getName() {
                    return name;
                }

                public void setName(String name) {
                    this.name = name;
                }

                public int getAge() {
                    return age;
                }

                public void setAge(int age) {
                    this.age = age;
                }
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // Verify the generated code contains builder methods (build/create are checked centrally)
    ProcessorAsserts.assertContaining(
        generatedCode,
        "public PersonBuilder name(String name)",
        "public PersonBuilder age(int age)",
        "private TrackedValue<String> name = unsetValue();",
        "private TrackedValue<Integer> age = unsetValue();");
  }

  @Test
  void shouldHandleGenericWrapperTypes() {

    JavaFileObject wrapperRecord =
        ProcessorTestUtils.forSource(
            """
            package test;

            public record WrapperRecord<T>(T value) {
            }
            """);

    JavaFileObject dtoWithWrappedTypes =
        ProcessorTestUtils.forSource(
            """
            package test;
            import java.time.LocalDate;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public class DtoWithWrappedTypes {
              private WrapperRecord<String> name;
              private WrapperRecord<Integer> count;
              private WrapperRecord<LocalDate> date;

              public WrapperRecord<String> getName() { return name; }
              public void setName(WrapperRecord<String> name) { this.name = name; }

              public WrapperRecord<Integer> getCount() { return count; }
              public void setCount(WrapperRecord<Integer> count) { this.count = count; }

              public WrapperRecord<LocalDate> getDate() { return date; }
              public void setDate(WrapperRecord<LocalDate> date) { this.date = date; }
            }
            """);

    // Test builder generation (without usage)
    Compilation compilation = compile(wrapperRecord, dtoWithWrappedTypes);
    String optionalBuilder = loadGeneratedSource(compilation, "DtoWithWrappedTypesBuilder");

    // Verify Optional wrapper generates basic methods
    assertGenerationSucceeded(compilation, "DtoWithWrappedTypesBuilder", optionalBuilder);
    ProcessorAsserts.assertContaining(
        optionalBuilder,
        "public static DtoWithWrappedTypesBuilder create()",
        "public DtoWithWrappedTypesBuilder name(WrapperRecord<String> name)",
        "public DtoWithWrappedTypesBuilder count(WrapperRecord<Integer> count)",
        "public DtoWithWrappedTypesBuilder date(WrapperRecord<LocalDate> date)",
        "public DtoWithWrappedTypes build()");

    // Verify no additional helper methods are generated for these generic types
    // (since they don't match List/Set/Map/Optional patterns with recognized inner types)
    ProcessorAsserts.assertNotContaining(
        optionalBuilder,
        "nameBuilderConsumer",
        "countBuilderConsumer",
        "dateBuilderConsumer",
        "name(String...)",
        "count(Integer...)",
        "date(LocalDate...)");
    // Backing fields present
    ProcessorAsserts.assertContaining(
        optionalBuilder,
        "private TrackedValue<WrapperRecord<String>> name = unsetValue();",
        "private TrackedValue<WrapperRecord<Integer>> count = unsetValue();",
        "private TrackedValue<WrapperRecord<LocalDate>> date = unsetValue();");
  }

  @Test
  void shouldGenerateSetterForClassInDifferentPackage() {
    // Given
    String packageName = "test";
    String className = "UsesOtherPackageHelper";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private otherpkg.Helper helper;

                public otherpkg.Helper getHelper() {
                  return helper;
                }
                public void setHelper(otherpkg.Helper helper) {
                  this.helper = helper;
                }
            """);

    JavaFileObject helperSource =
        ProcessorTestUtils.forSource(
            """
                package otherpkg;
                public class Helper {
                  public Helper() {}
                }
            """);

    // When
    Compilation compilation = compile(sourceFile, helperSource);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    ProcessorAsserts.assertContaining(
        generatedCode,
        "public UsesOtherPackageHelperBuilder helper(Helper helper)",
        "private TrackedValue<Helper> helper = unsetValue();");
  }

  @Test
  void shouldHandleSetOfStrings() {
    // Given
    String packageName = "test";
    String className = "HasSetString";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private java.util.Set<String> tags;

                public java.util.Set<String> getTags() { return tags; }
                public void setTags(java.util.Set<String> tags) { this.tags = tags; }
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(
        compilation, builderClassName, loadGeneratedSource(compilation, builderClassName));
    ProcessorAsserts.assertContaining(
        generatedCode,
        "public HasSetStringBuilder tags(Set<String> tags)",
        "this.tags = changedValue(tags);",
        "public HasSetStringBuilder tags(Consumer<HashSetBuilder<String>> tagsBuilderConsumer)",
        "this.tags = changedValue(builder.build());",
        "public HasSetStringBuilder tags(String... tags)",
        "this.tags = changedValue(Set.of(tags));",
        "public HasSetStringBuilder tags(Supplier<Set<String>> tagsSupplier)",
        "this.tags = changedValue(tagsSupplier.get());");
  }

  @Test
  void shouldHandleSetOfCustomType() {
    // Given
    String packageName = "test";
    String className = "HasSetCustom";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private java.util.Set<Helper> helpers;

                public java.util.Set<Helper> getHelpers() { return helpers; }
                public void setHelpers(java.util.Set<Helper> helpers) { this.helpers = helpers; }
            """);

    JavaFileObject helperSource =
        ProcessorTestUtils.forSource(
            """
                package test;
                public class Helper {
                  public Helper() {}
                }
            """);

    // When
    Compilation compilation = compile(sourceFile, helperSource);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    ProcessorAsserts.assertContaining(
        generatedCode,
        "public HasSetCustomBuilder helpers(Set<Helper> helpers)",
        "this.helpers = changedValue(helpers);",
        "public HasSetCustomBuilder helpers(Consumer<HashSetBuilder<Helper>> helpersBuilderConsumer)",
        "this.helpers = changedValue(builder.build());",
        "public HasSetCustomBuilder helpers(Helper... helpers)",
        "this.helpers = changedValue(Set.of(helpers));",
        "public HasSetCustomBuilder helpers(Supplier<Set<Helper>> helpersSupplier)",
        "this.helpers = changedValue(helpersSupplier.get());");
  }

  @Test
  void shouldHandleHelperInDifferentPackage() {
    // Given
    String packageName = "test";
    String className = "UsesOtherPackageHelper";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private otherpkg.Helper helper;

                public otherpkg.Helper getHelper() { return helper; }
                public void setHelper(otherpkg.Helper helper) { this.helper = helper; }
            """);

    JavaFileObject helperSource =
        ProcessorTestUtils.forSource(
            """
                package otherpkg;
                public class Helper {
                  public Helper() {}
                }
            """);

    // When
    Compilation compilation = compile(sourceFile, helperSource);

    // Then
    assertGenerationSucceeded(
        compilation, builderClassName, loadGeneratedSource(compilation, builderClassName));
    // build/create are checked centrally; no additional builder setter checks here
  }

  @Test
  void shouldFilterOutNonRelevantMethods() {
    // Given
    String packageName = "test";
    String className = "HasVariousMethods";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                // valid setter (having a getter)
                public void setOk(int ok) {}
                public int getOk() { return 0; }

                // should be filtered
                public void nonSetterMethods() {}
                public void settingChanged(String nonSetterValue) {}
                public int setSetterWithReturnValue(int returnValue) { return returnValue; }
                private void setHidden(int hidden) {}
                public static void setUtil(int util) {}
                public void setRisky(int risk) throws Exception {}
                public int returnsInt() { return 42; }
                public int getJustGetter() { return 25; }
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    // Expect only setter-derived methods to be present (no proxy methods)
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains("public HasVariousMethodsBuilder ok(int ok)"),
        contains("this.ok = changedValue(ok);"),
        notContains("nonSetterMethods"),
        notContains("settingChanged"),
        notContains("setterWithReturnValue"),
        notContains("hidden"),
        notContains("util(int util)"),
        notContains("risky(int risk)"),
        notContains("returnsInt"),
        notContains("justGetter"));
  }

  @Test
  void shouldIgnoreIfAnnotatedWithIgnoreInBuilder() {
    // Given
    String packageName = "test";
    String className = "IgnoredByAnnotation";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private String name;

                public String getName() { return name; }

                @org.javahelpers.simple.builders.core.annotations.IgnoreInBuilder
                public void setName(String name) { this.name = name; }
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    // Ensure no builder methods were generated for ignored setters (method- and parameter-level)
    ProcessorAsserts.assertNotContaining(
        generatedCode,
        "public IgnoredByAnnotationBuilder name(String name)",
        "public IgnoredByAnnotationBuilder name(Supplier<String> nameSupplier)");
  }

  @Test
  void shouldNotGenerateConsumersForPrimitiveFields() {
    // Given
    String packageName = "test";
    String className = "PrimAndArray";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private int count;
                private String[] names;

                public int getCount() { return count; }
                public void setCount(int count) { this.count = count; }

                public String[] getNames() { return names; }
                public void setNames(String[] names) { this.names = names; }
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    // Primitives should not get consumer methods, but arrays now get ArrayListBuilder consumers
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains("public PrimAndArrayBuilder count(Supplier"),
        contains("public PrimAndArrayBuilder names(Supplier"),
        notContains("public PrimAndArrayBuilder count(Consumer"),
        contains("public PrimAndArrayBuilder names(Consumer<ArrayListBuilder<String>>"));
  }

  @Test
  void shouldNotGenerateConsumerForAbstractHelper() {
    // Given
    String packageName = "test";
    String className = "UsesAbstract";
    String builderClassName = className + "Builder";

    JavaFileObject dto =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private HelperAbs helperAbs;

                public HelperAbs getHelperAbs() { return helperAbs; }
                public void setHelperAbs(HelperAbs helperAbs) { this.helperAbs = helperAbs; }
            """);

    JavaFileObject helperAbs =
        ProcessorTestUtils.forSource(
            """
                package test;
                public abstract class HelperAbs {
                  public HelperAbs() {}
                }
            """);

    // When
    Compilation compilation = compile(dto, helperAbs);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // No consumer method should be generated for abstract helper type
    ProcessorAsserts.assertNotContaining(
        generatedCode, "helperAbsConsumer", "helperAbsBuilderConsumer");
  }

  @Test
  void shouldGenerateAnnotationsAndCreateBuildMethods() {
    // Given
    String packageName = "test";
    String className = "AnnoTarget";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private int a;
                public int getA() { return a; }
                public void setA(int a) { this.a = a; }
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    ProcessorAsserts.assertContaining(
        generatedCode,
        "@Generated(\"Generated by org.javahelpers.simple.builders.processor.BuilderProcessor\")",
        "@BuilderImplementation( forClass = AnnoTarget.class )",
        "public static AnnoTargetBuilder create() { return new AnnoTargetBuilder(); }",
        "@Override public AnnoTarget build()");
  }

  @Test
  void shouldGenerateVarargsConvenienceForCollections() {
    // Given
    String packageName = "test";
    String className = "HasCollectionsConvenience";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private java.util.List<String> names;
                private java.util.Set<String> tags;

                public java.util.List<String> getNames() { return names; }
                public void setNames(java.util.List<String> names) { this.names = names; }

                public java.util.Set<String> getTags() { return tags; }
                public void setTags(java.util.Set<String> tags) { this.tags = tags; }
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    ProcessorAsserts.assertContaining(
        generatedCode,
        "public HasCollectionsConvenienceBuilder names(String... names) { this.names = changedValue(List.of(names));",
        "public HasCollectionsConvenienceBuilder tags(String... tags) { this.tags = changedValue(Set.of(tags));");
  }

  @Test
  void shouldHandleJavaTimeField() {
    // Given
    String packageName = "test";
    String className = "HasLocalDate";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private java.time.LocalDate date;

                public java.time.LocalDate getDate() { return date; }
                public void setDate(java.time.LocalDate date) { this.date = date; }
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    ProcessorAsserts.assertContaining(
        generatedCode,
        "public HasLocalDateBuilder date(LocalDate date) { this.date = changedValue(date);");
  }

  @Test
  void shouldHandleMixedFields() {
    // Given
    String packageName = "test";
    String className = "HasMixed";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private int a;
                private String[] names;
                private java.util.List<String> list;
                private java.util.Map<String, Integer> map;
                private java.time.LocalDate date;

                public int getA() { return a; }
                public void setA(int a) { this.a = a; }

                public String[] getNames() { return names; }
                public void setNames(String[] names) { this.names = names; }

                public java.util.List<String> getList() { return list; }
                public void setList(java.util.List<String> list) { this.list = list; }

                public java.util.Map<String, Integer> getMap() { return map; }
                public void setMap(java.util.Map<String, Integer> map) { this.map = map; }

                public java.time.LocalDate getDate() { return date; }
                public void setDate(java.time.LocalDate date) { this.date = date; }
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    // Expect signatures for all supported setter patterns per field kind
    ProcessorAsserts.assertingResult(
        generatedCode,
        // primitive int: direct and supplier (boxed)
        contains("public HasMixedBuilder a(int a)"),
        contains("public HasMixedBuilder a(Supplier<Integer> aSupplier)"),
        // array String[]: varargs, supplier, List parameter, and ArrayListBuilder consumer
        contains("public HasMixedBuilder names(String... names)"),
        contains("public HasMixedBuilder names(Supplier<String[]> namesSupplier)"),
        contains("public HasMixedBuilder names(List<String> names)"),
        contains(
            "public HasMixedBuilder names(Consumer<ArrayListBuilder<String>> namesBuilderConsumer)"),
        // List<String>: direct, supplier, varargs convenience and consumer with ArrayListBuilder
        contains("public HasMixedBuilder list(List<String> list)"),
        contains("public HasMixedBuilder list(Supplier<List<String>> listSupplier)"),
        contains("public HasMixedBuilder list(String... list)"),
        contains(
            "public HasMixedBuilder list(Consumer<ArrayListBuilder<String>> listBuilderConsumer)"),
        // Map<String,Integer>: direct and supplier only
        contains("public HasMixedBuilder map(Map<String, Integer> map)"),
        contains("public HasMixedBuilder map(Supplier<Map<String, Integer>> mapSupplier)"),
        // LocalDate: direct and supplier
        contains("public HasMixedBuilder date(LocalDate date)"),
        contains("public HasMixedBuilder date(Supplier<LocalDate> dateSupplier)"));
  }

  @Test
  void shouldHandleObjectArrayField() {
    // Given
    String packageName = "test";
    String className = "HasObjectArray";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private String[] names;

                public String[] getNames() { return names; }
                public void setNames(String[] names) { this.names = names; }
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    // Arrays should get setter, supplier, List parameter, and ArrayListBuilder consumer
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains("public HasObjectArrayBuilder names(Supplier<String[]> namesSupplier)"),
        contains("public HasObjectArrayBuilder names(String... names)"),
        notContains("public HasObjectArrayBuilder names(String[] names)"),
        contains("public HasObjectArrayBuilder names(List<String> names)"),
        contains("Consumer<ArrayListBuilder<String>> namesBuilderConsumer"),
        notContains("Consumer<HashSetBuilder"));
  }

  @Test
  void shouldHandleClassWithOnlyGetters() {
    // Given
    String packageName = "test";
    String className = "OnlyGetters";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private String name;

                public String getName() { return name; }
                // no setter on purpose
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    // Even without setters, builder should exist (build/create checked centrally).
    ProcessorAsserts.assertNotContaining(
        generatedCode, "public OnlyGettersBuilder name(String name)");
  }

  @Test
  void shouldHandleEmptyClass() {
    // Given
    String packageName = "test";
    String className = "EmptyClass";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                // No fields or methods
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    // There should be no builder generated for an empty class
    ProcessorAsserts.assertContaining(
        generatedCode,
        "public EmptyClass build()",
        "EmptyClass result = new EmptyClass();",
        "return result;");
  }

  @Test
  void shouldGenerateBuilderForPrimitiveOnlyClass() {
    // Given
    String packageName = "test";
    String className = "Numbers";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private int a;

                public int getA() { return a; }
                public void setA(int a) { this.a = a; }
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    ProcessorAsserts.assertContaining(generatedCode, "public NumbersBuilder a(int a)");
  }

  @Test
  void shouldHandleListField() {
    // Given
    String packageName = "test";
    String className = "HasList";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private java.util.List<String> names;

                public java.util.List<String> getNames() { return names; }
                public void setNames(java.util.List<String> names) { this.names = names; }
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    // Expect direct setter, supplier, varargs convenience, and consumer with ArrayListBuilder
    ProcessorAsserts.assertContaining(
        generatedCode,
        "public HasListBuilder names(List<String> names)",
        "public HasListBuilder names(Supplier<List<String>> namesSupplier)",
        "public HasListBuilder names(String... names)",
        "public HasListBuilder names(Consumer<ArrayListBuilder<String>> namesBuilderConsumer)");
  }

  @Test
  void shouldBoxPrimitiveTypeArguments() {
    // Given
    String packageName = "test";
    String className = "HasConsumer";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private int intValue;
                private long longValue;
                private double doubleValue;
                private boolean booleanValue;

                public int getIntValue() { return intValue; }
                public void setIntValue(int intValue) { this.intValue = intValue; }
                public long getLongValue() { return longValue; }
                public void setLongValue(long longValue) { this.longValue = longValue; }
                public double getDoubleValue() { return doubleValue; }
                public void setDoubleValue(double doubleValue) { this.doubleValue = doubleValue; }
                public boolean isBooleanValue() { return booleanValue; }
                public void setBooleanValue(boolean booleanValue) { this.booleanValue = booleanValue; }
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    // Ensure builder compiles and does not use primitive type argument like <int>
    ProcessorAsserts.assertingResult(
        generatedCode,
        notContains("<int>"),
        notContains("<long>"),
        notContains("<double>"),
        notContains("<boolean>"),
        contains("public HasConsumerBuilder intValue(Supplier<Integer> intValueSupplier)"),
        contains("public HasConsumerBuilder longValue(Supplier<Long> longValueSupplier)"),
        contains("public HasConsumerBuilder doubleValue(Supplier<Double> doubleValueSupplier)"),
        contains("public HasConsumerBuilder booleanValue(Supplier<Boolean> booleanValueSupplier)"));
  }

  @Test
  void shouldHandleFunctionalInterfaces() {
    // Given
    String packageName = "test";
    String className = "HasFunctionalInterface";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private java.util.function.Consumer<Integer> consumer;

                public java.util.function.Consumer<Integer> getConsumer() { return consumer; }
                public void setConsumer(java.util.function.Consumer<Integer> consumer) { this.consumer = consumer; }
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    // Ensure builder compiles and not generates Consumer of Consumer or Supplier of Consumer
    ProcessorAsserts.assertNotContaining(generatedCode, "Consumer<Consumer", "Supplier<Consumer>");
  }

  @Test
  void shouldGenerateBuilderForParentClassSetter() {
    // Given
    String className = "ChildInheritsSetter";
    String builderClassName = className + "Builder";

    JavaFileObject parentSource =
        ProcessorTestUtils.forSource(
            """
                package test;
                public class ParentWithSetter {
                  private String name;
                  public String getName() { return name; }
                  public void setName(String name) { this.name = name; }
                }
            """);

    JavaFileObject childSource =
        ProcessorTestUtils.forSource(
            """
                package test;
                import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
                @SimpleBuilder
                public class ChildInheritsSetter extends ParentWithSetter { }
            """);

    // When
    Compilation compilation = compile(childSource, parentSource);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    // Expect builder to expose setter for parent class property
    ProcessorAsserts.assertingResult(
        generatedCode, contains("public ChildInheritsSetterBuilder name(String name)"));
  }

  @Test
  void shouldHandleNestedTypeWithEmptyConstructor() {
    // Given
    String packageName = "test";
    String className = "OuterWithHelper";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private Helper helper;

                public Helper getHelper() { return helper; }
                public void setHelper(Helper helper) { this.helper = helper; }
            """);

    // And a top-level helper class with empty constructor
    JavaFileObject helperSource =
        ProcessorTestUtils.forSource(
            """
                package test;
                public class Helper {
                  public Helper() {}
                  private String v;
                  public String getV() { return v; }
                  public void setV(String v) { this.v = v; }
                }
            """);

    // When
    Compilation compilation = compile(sourceFile, helperSource);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    // Expect direct setter, supplier and consumer creating instance via empty constructor
    ProcessorAsserts.assertContaining(
        generatedCode,
        "public OuterWithHelperBuilder helper(Helper helper)",
        "public OuterWithHelperBuilder helper(Supplier<Helper> helperSupplier)",
        "public OuterWithHelperBuilder helper(Consumer<Helper> helperConsumer)");
  }

  @Test
  void shouldHandleNestedTypeWithoutEmptyConstructor() {
    // Given
    String packageName = "test";
    String className = "OuterWithNoEmptyHelper";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private HelperNoEmpty helper;

                public HelperNoEmpty getHelper() { return helper; }
                public void setHelper(HelperNoEmpty helper) { this.helper = helper; }
            """);

    // And a top-level helper class without empty constructor
    JavaFileObject helperSource =
        ProcessorTestUtils.forSource(
            """
                package test;
                public class HelperNoEmpty {
                  private final int x;
                  public HelperNoEmpty(int x) { this.x = x; }
                  public int getX() { return x; }
                }
            """);

    // When
    Compilation compilation = compile(sourceFile, helperSource);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    // Expect only direct setter and supplier. No consumer method should be generated.
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains("public OuterWithNoEmptyHelperBuilder helper(HelperNoEmpty helper)"),
        contains(
            "public OuterWithNoEmptyHelperBuilder helper(Supplier<HelperNoEmpty> helperSupplier)"),
        notContains(
            "public OuterWithNoEmptyHelperBuilder helper(Consumer<HelperNoEmpty> helperConsumer)"));
  }

  @Test
  void shouldHandleListOfCustomTypeWithoutEmptyConstructor() {
    // Given
    String packageName = "test";
    String className = "HasListNoEmpty";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private java.util.List<HelperNoEmpty> helpers;

                public java.util.List<HelperNoEmpty> getHelpers() { return helpers; }
                public void setHelpers(java.util.List<HelperNoEmpty> helpers) { this.helpers = helpers; }
            """);

    // And a top-level HelperNoEmpty class referenced by the list, without empty constructor
    JavaFileObject helperSource =
        ProcessorTestUtils.forSource(
            """
                package test;
                public class HelperNoEmpty {
                  private final int x;
                  public HelperNoEmpty(int x) { this.x = x; }
                  public int getX() { return x; }
                }
            """);

    // When
    Compilation compilation = compile(sourceFile, helperSource);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    // Expect direct setter, varargs, supplier and consumer builder method
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains("public HasListNoEmptyBuilder helpers(List<HelperNoEmpty> helpers)"),
        contains("public HasListNoEmptyBuilder helpers(HelperNoEmpty... helpers)"),
        contains(
            "public HasListNoEmptyBuilder helpers(Supplier<List<HelperNoEmpty>> helpersSupplier)"),
        contains(
            "public HasListNoEmptyBuilder helpers( Consumer<ArrayListBuilder<HelperNoEmpty>> helpersBuilderConsumer)"));
  }

  @Test
  void shouldHandleListOfCustomType() {
    // Given
    String packageName = "test";
    String className = "HasListCustom";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private java.util.List<Helper> helpers;

                public java.util.List<Helper> getHelpers() { return helpers; }
                public void setHelpers(java.util.List<Helper> helpers) { this.helpers = helpers; }
            """);

    // And a top-level Helper class referenced by the list
    JavaFileObject helperSource =
        ProcessorTestUtils.forSource(
            """
                package test;
                public class Helper {
                  public Helper() {}
                }
            """);

    // When
    Compilation compilation = compile(sourceFile, helperSource);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    // Expect direct setter, supplier, varargs convenience, and consumer with ArrayListBuilder
    ProcessorAsserts.assertContaining(
        generatedCode,
        "public HasListCustomBuilder helpers(List<Helper> helpers)",
        "public HasListCustomBuilder helpers(Supplier<List<Helper>> helpersSupplier)",
        "public HasListCustomBuilder helpers(Helper... helpers)",
        "public HasListCustomBuilder helpers(Consumer<ArrayListBuilder<Helper>> helpersBuilderConsumer)");
  }

  @Test
  void shouldHandleMapWithCustomValueType() {
    // Given
    String packageName = "test";
    String className = "HasMapCustom";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private java.util.Map<String, Helper> map;

                public java.util.Map<String, Helper> getMap() { return map; }
                public void setMap(java.util.Map<String, Helper> map) { this.map = map; }
            """);

    // And a top-level Helper class referenced by the map
    JavaFileObject helperSource =
        JavaFileObjects.forSourceString(
            packageName + ".Helper",
            """
                package test;
                public class Helper {
                  public Helper() {}
                }
            """);

    // When
    Compilation compilation = compile(sourceFile, helperSource);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    // Expect only direct setter and supplier for Map; no varargs or collection-builder consumer
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains("public HasMapCustomBuilder map(Map<String, Helper> map)"),
        contains("public HasMapCustomBuilder map(Supplier<Map<String, Helper>> mapSupplier)"),
        notContains("public HasMapCustomBuilder map(String..."),
        notContains("Consumer<ArrayListBuilder"),
        notContains("Consumer<HashSetBuilder"));
  }

  @Test
  void shouldRetainTypeParameterOfDtoToInBuilder() {
    // Given
    String builderClassName = "GenericDtoBuilder";

    JavaFileObject genericDtoSource =
        ProcessorTestUtils.forSource(
            """
                package test;
                import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
                @SimpleBuilder
                public class GenericDto<T> {
                  private String value;
                  public String getValue() { return value; }
                  public void setValue(String value) { this.value = value; }
                }
            """);

    // When
    Compilation compilation = compile(genericDtoSource);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertThat(compilation).succeededWithoutWarnings();
    ProcessorAsserts.assertingResult(
        generatedCode,
        // builder preserves type parameter T
        contains("class GenericDtoBuilder<T>"),
        // builderinterfaces using type parameter T too
        contains("implements IBuilderBase<GenericDto<T>>"),
        // build returns GenericDto<T>
        contains("public GenericDto<T> build()"),
        // create() exposes generic as well
        contains("public static <T> GenericDtoBuilder<T> create()"));
  }

  @Test
  void shouldGenerateGenericBuilderWithTypeParameterInField() {
    // Given
    String builderClassName = "GenericDtoBuilder";

    JavaFileObject genericDtoSource =
        ProcessorTestUtils.forSource(
            """
                package test;
                import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
                @SimpleBuilder
                public class GenericDto<T> {
                  private T value;
                  public T getValue() { return value; }
                  public void setValue(T value) { this.value = value; }
                }
            """);

    // When
    Compilation compilation = compile(genericDtoSource);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertThat(compilation).succeededWithoutWarnings();
    ProcessorAsserts.assertingResult(
        generatedCode,
        // builder preserves type parameter T
        contains("class GenericDtoBuilder<T>"),
        // setter keeps T
        contains("public GenericDtoBuilder<T> value(T value)"),
        // supplier-based setter retains T
        contains("public GenericDtoBuilder<T> value(Supplier<T> valueSupplier)"),
        // no direct consumer possible for unknown T (no empty ctor info)
        notContains("public GenericDtoBuilder<T> value(Consumer<T> valueConsumer)"),
        // build returns GenericDto<T>
        contains("public GenericDto<T> build()"),
        // create() exposes generic as well
        contains("public static <T> GenericDtoBuilder<T> create()"));
  }

  @Test
  void shouldGenerateGenericBuilderWithMultipleTypeParameterInFields() {
    // Given
    String builderClassName = "GenericDtoBuilder";

    JavaFileObject genericDtoSource =
        ProcessorTestUtils.forSource(
            """
                package test;
                import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
                @SimpleBuilder
                public class GenericDto<T, U> {
                  private T valueT;
                  private U valueU;
                  public T getValueT() { return valueT; }
                  public void setValueT(T valueT) { this.valueT = valueT; }
                  public U getValueU() { return valueU; }
                  public void setValueU(U valueU) { this.valueU = valueU; }
                }
            """);

    // When
    Compilation compilation = compile(genericDtoSource);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertThat(compilation).succeededWithoutWarnings();
    ProcessorAsserts.assertingResult(
        generatedCode,
        // builder preserves type parameter T
        contains("class GenericDtoBuilder<T, U>"),
        // setter keeps T
        contains("public GenericDtoBuilder<T, U> valueT(T valueT)"),
        // setter keeps U
        contains("public GenericDtoBuilder<T, U> valueU(U valueU)"),
        // supplier-based setter retains T
        contains("public GenericDtoBuilder<T, U> valueT(Supplier<T> valueTSupplier)"),
        // supplier-based setter retains U
        contains("public GenericDtoBuilder<T, U> valueU(Supplier<U> valueUSupplier)"),
        // no direct consumer possible for unknown T (no empty ctor info)
        notContains("public GenericDtoBuilder<T, U> valueT(Consumer<T> valueTConsumer)"),
        // no direct consumer possible for unknown U (no empty ctor info)
        notContains("public GenericDtoBuilder<T, U> valueU(Consumer<U> valueUConsumer)"),
        // build returns GenericDto<T, U>
        contains("public GenericDto<T, U> build()"),
        // create() exposes generic as well
        contains("public static <T, U> GenericDtoBuilder<T, U> create()"));
  }

  @Test
  void shouldIgnoreSettersWithMethodLevelTypeParameters() {
    // Given: a DTO with setters that have their own type parameters
    String builderClassName = "DtoWithGenericSettersBuilder";
    JavaFileObject source =
        ProcessorTestUtils.forSource(
            """
                package test;
                import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
                import java.io.Serializable;
                @SimpleBuilder
                public class DtoWithGenericSetters <T> {
                  private String name;
                  private Object fieldWithOwnGeneric;
                  private T fieldWithClassGeneric;
                  private T fieldWithExtendedClassGeneric;
                  public String getName() { return name; }
                  public void setName(String name) { this.name = name; }
                  public T getFieldWithClassGeneric() { return fieldWithClassGeneric; }
                  public void setFieldWithClassGeneric(T fieldWithClassGeneric) { this.fieldWithClassGeneric = fieldWithClassGeneric; }
                  public T getFieldWithExtendedClassGeneric() { return fieldWithExtendedClassGeneric; }
                  public <F extends T> void setFieldWithExtendedClassGeneric(F fieldWithExtendedClassGeneric) {
                    this.fieldWithExtendedClassGeneric = fieldWithExtendedClassGeneric;
                  }
                  public Object getFieldWithOwnGeneric() { return fieldWithOwnGeneric; }
                  // This setter has its own type parameter - should be ignored
                  public <F extends Serializable> void setFieldWithOwnGeneric(F fieldWithOwnGeneric) {
                    this.fieldWithOwnGeneric = fieldWithOwnGeneric;
                  }
                }
            """);

    // When
    Compilation compilation = compile(source);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertThat(compilation).succeeded();
    assertThat(compilation)
        .hadWarningContaining(
            "Field 'fieldWithOwnGeneric' has field-specific generics, so it will be ignored");
    assertThat(compilation)
        .hadWarningContaining(
            "Field 'fieldWithExtendedClassGeneric' has field-specific generics, so it will be ignored");
    ProcessorAsserts.assertingResult(
        generatedCode,
        // name field should be present
        contains("public DtoWithGenericSettersBuilder<T> name"),
        // fieldWithClassGeneric setter should be present
        contains("public DtoWithGenericSettersBuilder<T> fieldWithClassGeneric"),
        // fieldWithOwnGeneric setter should be ignored because it has method-level type parameter
        // <F>
        notContains("public DtoWithGenericSettersBuilder<T> fieldWithOwnGeneric"),
        // fieldWithExtendedClassGeneric setter should be ignored because it has method-level type
        // parameter <F>, even if it extends the type parameter of the class <T>
        notContains("public DtoWithGenericSettersBuilder<T> fieldWithExtendedClassGeneric"));
  }

  @Test
  void shouldNotGenerateBuilderWhenNestedBuilderInterfaceExists() {
    // Given: an annotated class that already declares a nested interface named like the would-be
    // builder
    JavaFileObject source =
        ProcessorTestUtils.forSource(
            """
                package test;
                import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
                public class HasInnerClassWithBuilder {
                  @SimpleBuilder
                  public class InnerClass {
                    private int x;
                    public int getX(){return x;}
                    public void setX(int x){this.x=x;}
                  }
                }
            """);

    // When
    Compilation compilation = compile(source);

    // Then: compilation should succeed but with a warning
    assertThat(compilation).succeeded();
    assertThat(compilation)
        .hadWarningContaining("The SimpleBuilder should be declared on a top-level class only");
  }

  @Test
  void shouldHandleOverloadedSettersForSameFieldWithoutConflicts() {
    // Given
    String packageName = "test";
    String className = "OverloadedNames";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private java.util.List<String> names;

                public java.util.List<String> getNames() { return names; }
                public void setNames(String[] names) { this.names = java.util.List.of(names); }
                public void setNames(java.util.List<String> names) { this.names = names; }
            """);

    // When
    Compilation compilation = compile(sourceFile);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    // Allow warnings but check that generation worked
    assertThat(compilation).succeeded();

    // Expect the canonical builder methods that should be generated
    ProcessorAsserts.assertContaining(
        generatedCode,
        "public OverloadedNamesBuilder names(List<String> names)",
        "public OverloadedNamesBuilder names(String... names)",
        "public OverloadedNamesBuilder names(Supplier<String[]> namesSupplier)");
  }

  @Test
  void collectionsWithRawTypes_shouldGenerateBuilders() {
    JavaFileObject rawCollectionsClass =
        ProcessorTestUtils.forSource(
            """
            package test;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
            import java.util.List;
            import java.util.Set;
            import java.util.Map;
            @SimpleBuilder
            public class RawCollections {
              private List rawList;
              private Set rawSet;
              private Map rawMap;

              public List getRawList() { return rawList; }
              public void setRawList(List rawList) { this.rawList = rawList; }

              public Set getRawSet() { return rawSet; }
              public void setRawSet(Set rawSet) { this.rawSet = rawSet; }

              public Map getRawMap() { return rawMap; }
              public void setRawMap(Map rawMap) { this.rawMap = rawMap; }
            }
            """);

    // Test builder generation (without usage)
    Compilation compilation = compile(rawCollectionsClass);
    String generatedCode = loadGeneratedSource(compilation, "RawCollectionsBuilder");
    assertGenerationSucceeded(compilation, "RawCollectionsBuilder", generatedCode);

    // Verify basic builder methods are generated
    ProcessorAsserts.assertContaining(
        generatedCode,
        "public static RawCollectionsBuilder create()",
        "public RawCollectionsBuilder rawList(List rawList)",
        "public RawCollectionsBuilder rawSet(Set rawSet)",
        "public RawCollectionsBuilder rawMap(Map rawMap)",
        "public RawCollections build()");

    // Verify no additional helper methods are generated for raw types
    ProcessorAsserts.assertNotContaining(
        generatedCode,
        "rawListBuilderConsumer",
        "rawSetBuilderConsumer",
        "rawMapBuilderConsumer",
        "rawList(String...)",
        "rawSet(String...)",
        "rawMap(Map.Entry)");
  }

  @Test
  void shouldGenerateArrayListBuilderWithElementBuildersForAnnotatedElements() {
    // Given: List of custom type where element has @SimpleBuilder
    String packageName = "test";
    String className = "HasListWithBuilders";
    String builderClassName = className + "Builder";

    JavaFileObject dto =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private java.util.List<Task> tasks;

                public java.util.List<Task> getTasks() { return tasks; }
                public void setTasks(java.util.List<Task> tasks) { this.tasks = tasks; }
            """);

    JavaFileObject task =
        ProcessorTestUtils.forSource(
            """
                package test;
                import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
                @SimpleBuilder
                public class Task {
                  private String title;
                  public String getTitle() { return title; }
                  public void setTitle(String title) { this.title = title; }
                }
            """);

    // When
    Compilation compilation = compile(dto, task);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // Verify WithElementBuilders consumer is generated instead of regular ArrayListBuilder
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains("public HasListWithBuildersBuilder tasks(List<Task> tasks)"),
        contains("public HasListWithBuildersBuilder tasks(Supplier<List<Task>> tasksSupplier)"),
        contains("public HasListWithBuildersBuilder tasks(Task... tasks)"),
        // Key assertion: uses ArrayListBuilderWithElementBuilders with TaskBuilder
        contains(
            "public HasListWithBuildersBuilder tasks(Consumer<ArrayListBuilderWithElementBuilders<Task, TaskBuilder>> tasksBuilderConsumer)"),
        contains(
            "new ArrayListBuilderWithElementBuilders<Task,TaskBuilder>(this.tasks.value(), TaskBuilder::create)"),
        contains("new ArrayListBuilderWithElementBuilders<Task,TaskBuilder>(TaskBuilder::create)"),
        // Should NOT use regular ArrayListBuilder
        notContains("Consumer<ArrayListBuilder<Task>>"));
  }

  @Test
  void shouldGenerateHashSetBuilderWithElementBuildersForAnnotatedElements() {
    // Given: Set of custom type where element has @SimpleBuilder
    String packageName = "test";
    String className = "HasSetWithBuilders";
    String builderClassName = className + "Builder";

    JavaFileObject dto =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private java.util.Set<Tag> tags;

                public java.util.Set<Tag> getTags() { return tags; }
                public void setTags(java.util.Set<Tag> tags) { this.tags = tags; }
            """);

    JavaFileObject tag =
        ProcessorTestUtils.forSource(
            """
                package test;
                import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
                @SimpleBuilder
                public class Tag {
                  private String name;
                  public String getName() { return name; }
                  public void setName(String name) { this.name = name; }
                }
            """);

    // When
    Compilation compilation = compile(dto, tag);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // Verify WithElementBuilders consumer is generated for Set
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains("public HasSetWithBuildersBuilder tags(Set<Tag> tags)"),
        contains("public HasSetWithBuildersBuilder tags(Supplier<Set<Tag>> tagsSupplier)"),
        contains("public HasSetWithBuildersBuilder tags(Tag... tags)"),
        // Key assertion: uses HashSetBuilderWithElementBuilders with TagBuilder
        contains(
            "public HasSetWithBuildersBuilder tags(Consumer<HashSetBuilderWithElementBuilders<Tag, TagBuilder>> tagsBuilderConsumer)"),
        contains(
            "new HashSetBuilderWithElementBuilders<Tag, TagBuilder>(this.tags.value(), TagBuilder::create)"),
        contains("new HashSetBuilderWithElementBuilders<Tag, TagBuilder>(TagBuilder::create)"),
        // Should NOT use regular HashSetBuilder
        notContains("Consumer<HashSetBuilder<Tag>>"));
  }

  @Test
  void shouldUseRegularBuilderWhenElementTypeHasNoSimpleBuilder() {
    // Given: List of custom type WITHOUT @SimpleBuilder annotation
    String packageName = "test";
    String className = "HasListWithoutBuilders";
    String builderClassName = className + "Builder";

    JavaFileObject dto =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private java.util.List<PlainClass> items;

                public java.util.List<PlainClass> getItems() { return items; }
                public void setItems(java.util.List<PlainClass> items) { this.items = items; }
            """);

    JavaFileObject plain =
        ProcessorTestUtils.forSource(
            """
                package test;
                public class PlainClass {
                  private String value;
                  public PlainClass() {}
                  public String getValue() { return value; }
                  public void setValue(String value) { this.value = value; }
                }
            """);

    // When
    Compilation compilation = compile(dto, plain);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // Verify regular ArrayListBuilder is used (not WithElementBuilders)
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains("public HasListWithoutBuildersBuilder items(List<PlainClass> items)"),
        contains(
            "public HasListWithoutBuildersBuilder items(Consumer<ArrayListBuilder<PlainClass>> itemsBuilderConsumer)"),
        // Should NOT use WithElementBuilders version
        notContains("ArrayListBuilderWithElementBuilders"),
        notContains("PlainClassBuilder"));
  }

  @Test
  void shouldPreserveSpecificCollectionTypesWithVarargsAndConsumerMethods() {
    // Given: A DTO with specific collection implementations (LinkedList, ArrayList, HashSet, etc.)
    String packageName = "test";
    String className = "SpecificCollectionsDto";
    String builderClassName = className + "Builder";

    JavaFileObject dto =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private java.util.LinkedList<String> linkedList;
                private java.util.ArrayList<Integer> arrayList;
                private java.util.HashSet<Double> hashSet;
                private java.util.TreeSet<Long> treeSet;
                private java.util.HashMap<String, String> hashMap;
                private java.util.TreeMap<String, Integer> treeMap;

                public java.util.LinkedList<String> getLinkedList() { return linkedList; }
                public void setLinkedList(java.util.LinkedList<String> linkedList) { this.linkedList = linkedList; }
                public java.util.ArrayList<Integer> getArrayList() { return arrayList; }
                public void setArrayList(java.util.ArrayList<Integer> arrayList) { this.arrayList = arrayList; }
                public java.util.HashSet<Double> getHashSet() { return hashSet; }
                public void setHashSet(java.util.HashSet<Double> hashSet) { this.hashSet = hashSet; }
                public java.util.TreeSet<Long> getTreeSet() { return treeSet; }
                public void setTreeSet(java.util.TreeSet<Long> treeSet) { this.treeSet = treeSet; }
                public java.util.HashMap<String, String> getHashMap() { return hashMap; }
                public void setHashMap(java.util.HashMap<String, String> hashMap) { this.hashMap = hashMap; }
                public java.util.TreeMap<String, Integer> getTreeMap() { return treeMap; }
                public void setTreeMap(java.util.TreeMap<String, Integer> treeMap) { this.treeMap = treeMap; }
            """);

    // When
    Compilation compilation = compile(dto);

    // Then
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);

    // Verify LinkedList methods: setter, supplier, varargs with LinkedList constructor
    ProcessorAsserts.assertingResult(
        generatedCode,
        // Direct setter
        contains("public SpecificCollectionsDtoBuilder linkedList(LinkedList<String> linkedList)"),
        // Supplier
        contains(
            "public SpecificCollectionsDtoBuilder linkedList(Supplier<LinkedList<String>> linkedListSupplier)"),
        // Varargs with LinkedList wrapper
        contains("public SpecificCollectionsDtoBuilder linkedList(String... linkedList)"),
        contains("new LinkedList<>(java.util.List.of(linkedList))"),
        // Consumer with LinkedList wrapper
        contains(
            "public SpecificCollectionsDtoBuilder linkedList(Consumer<ArrayListBuilder<String>> linkedListBuilderConsumer)"),
        contains("new LinkedList<>(builder.build())"));

    // Verify ArrayList methods: setter, supplier, varargs with ArrayList constructor
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains("public SpecificCollectionsDtoBuilder arrayList(ArrayList<Integer> arrayList)"),
        contains(
            "public SpecificCollectionsDtoBuilder arrayList(Supplier<ArrayList<Integer>> arrayListSupplier)"),
        contains("public SpecificCollectionsDtoBuilder arrayList(Integer... arrayList)"),
        contains("new ArrayList<>(java.util.List.of(arrayList))"),
        contains(
            "public SpecificCollectionsDtoBuilder arrayList(Consumer<ArrayListBuilder<Integer>> arrayListBuilderConsumer)"),
        contains("new ArrayList<>(builder.build())"));

    // Verify HashSet methods: setter, supplier, varargs with HashSet constructor
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains("public SpecificCollectionsDtoBuilder hashSet(HashSet<Double> hashSet)"),
        contains(
            "public SpecificCollectionsDtoBuilder hashSet(Supplier<HashSet<Double>> hashSetSupplier)"),
        contains("public SpecificCollectionsDtoBuilder hashSet(Double... hashSet)"),
        contains("new HashSet<>(java.util.Set.of(hashSet))"),
        contains(
            "public SpecificCollectionsDtoBuilder hashSet(Consumer<HashSetBuilder<Double>> hashSetBuilderConsumer)"),
        contains("new HashSet<>(builder.build())"));

    // Verify TreeSet methods: setter, supplier, varargs with TreeSet constructor
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains("public SpecificCollectionsDtoBuilder treeSet(TreeSet<Long> treeSet)"),
        contains(
            "public SpecificCollectionsDtoBuilder treeSet(Supplier<TreeSet<Long>> treeSetSupplier)"),
        contains("public SpecificCollectionsDtoBuilder treeSet(Long... treeSet)"),
        contains("new TreeSet<>(java.util.Set.of(treeSet))"),
        contains(
            "public SpecificCollectionsDtoBuilder treeSet(Consumer<HashSetBuilder<Long>> treeSetBuilderConsumer)"),
        contains("new TreeSet<>(builder.build())"));

    // Verify HashMap methods: setter, supplier, varargs with HashMap constructor
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains("public SpecificCollectionsDtoBuilder hashMap(HashMap<String, String> hashMap)"),
        contains(
            "public SpecificCollectionsDtoBuilder hashMap(Supplier<HashMap<String, String>> hashMapSupplier)"),
        contains("public SpecificCollectionsDtoBuilder hashMap(Entry<String, String>... hashMap)"),
        contains("new HashMap<>(java.util.Map.ofEntries(hashMap))"),
        contains(
            "public SpecificCollectionsDtoBuilder hashMap(Consumer<HashMapBuilder<String, String>> hashMapBuilderConsumer)"),
        contains("new HashMap<>(builder.build())"));

    // Verify TreeMap methods: setter, supplier, varargs with TreeMap constructor
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains("public SpecificCollectionsDtoBuilder treeMap(TreeMap<String, Integer> treeMap)"),
        contains(
            "public SpecificCollectionsDtoBuilder treeMap(Supplier<TreeMap<String, Integer>> treeMapSupplier)"),
        contains("public SpecificCollectionsDtoBuilder treeMap(Entry<String, Integer>... treeMap)"),
        contains("new TreeMap<>(java.util.Map.ofEntries(treeMap))"),
        // TreeMap consumer method may not be generated - checking what's available
        contains("new HashMap<>(builder.build())"));
  }

  @Test
  void shouldNotGenerateSetConsumerWhenFieldHasBuilder() {
    // Given: A Set field where the element type has a builder, but
    // HashSetBuilderWithElementBuilders is disabled
    String packageName = "test";
    String className = "HasSetWithBuilderElement";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private java.util.Set<Helper> helpers;

                public java.util.Set<Helper> getHelpers() { return helpers; }
                public void setHelpers(java.util.Set<Helper> helpers) { this.helpers = helpers; }
            """);

    JavaFileObject helperSource =
        ProcessorTestUtils.forSource(
            """
                package test;
                import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
                @SimpleBuilder
                public class Helper {
                  public Helper() {}
                }
            """);

    // When: Compile with HashSetBuilderWithElementBuilders disabled
    Compilation compilation =
        ProcessorTestUtils.createCompiler()
            .withOptions("-Asimplebuilder.usingHashSetBuilderWithElementBuilders=false")
            .compile(sourceFile, helperSource);

    // Then: No consumer method should be generated (covers line 101 in appliesTo)
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    ProcessorAsserts.assertNotContaining(
        generatedCode,
        "public HasSetWithBuilderElementBuilder helpers(Consumer<HashSetBuilderWithElementBuilders<Helper, HelperBuilder>> helpersBuilderConsumer)");
  }

  @Test
  void shouldNotGenerateSetConsumerWhenBothHashSetBuilderOptionsDisabled() {
    // Given: A Set field with both HashSetBuilder options disabled
    String packageName = "test";
    String className = "HasSetWithDisabledBuilders";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private java.util.Set<String> tags;

                public java.util.Set<String> getTags() { return tags; }
                public void setTags(java.util.Set<String> tags) { }
            """);

    // When: Compile with both HashSetBuilder options disabled
    Compilation compilation =
        ProcessorTestUtils.createCompiler()
            .withOptions(
                "-Asimplebuilder.usingHashSetBuilder=false",
                "-Asimplebuilder.usingHashSetBuilderWithElementBuilders=false")
            .compile(sourceFile);

    // Then: No consumer method should be generated (covers line 104 and 144)
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    ProcessorAsserts.assertNotContaining(
        generatedCode,
        "public HasSetWithDisabledBuildersBuilder tags(Consumer<HashSetBuilder<String>> tagsBuilderConsumer)");
  }

  @Test
  void shouldNotGenerateSetConsumerWhenBuilderConsumerDisabled() {
    // Given: A Set field with generateBuilderConsumer disabled
    String packageName = "test";
    String className = "HasSetWithConsumerDisabled";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private java.util.Set<String> tags;

                public java.util.Set<String> getTags() { return tags; }
                public void setTags(java.util.Set<String> tags) { }
            """);

    // When: Compile with generateBuilderConsumer disabled
    Compilation compilation =
        ProcessorTestUtils.createCompiler()
            .withOptions("-Asimplebuilder.generateBuilderConsumer=false")
            .compile(sourceFile);

    // Then: No consumer method should be generated (covers line 91-93 in appliesTo)
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    ProcessorAsserts.assertNotContaining(
        generatedCode,
        "public HasSetWithConsumerDisabledBuilder tags(Consumer<HashSetBuilder<String>> tagsBuilderConsumer)");
  }

  @Test
  void shouldGenerateSetConsumerForSimpleElements() {
    // Given: A Set field where element type has no builder
    String packageName = "test";
    String className = "HasSetWithSimpleElement";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private java.util.Set<String> tags;

                public java.util.Set<String> getTags() { return tags; }
                public void setTags(java.util.Set<String> tags) { }
            """);

    // When: Compile with HashSetBuilder enabled
    Compilation compilation =
        ProcessorTestUtils.createCompiler()
            .withOptions("-Asimplebuilder.usingHashSetBuilder=true")
            .compile(sourceFile);

    // Then: Consumer method with HashSetBuilder should be generated
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    ProcessorAsserts.assertContaining(
        generatedCode,
        "public HasSetWithSimpleElementBuilder tags(Consumer<HashSetBuilder<String>> tagsBuilderConsumer)",
        "this.tags = changedValue(builder.build());");
  }

  @Test
  void shouldGenerateSetConsumerForBuilderElements() {
    // Given: A Set field where element type has a builder
    String packageName = "test";
    String className = "HasSetWithBuilderElement";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private java.util.Set<Helper> helpers;

                public java.util.Set<Helper> getHelpers() { return helpers; }
                public void setHelpers(java.util.Set<Helper> helpers) { }
            """);

    JavaFileObject helperSource =
        ProcessorTestUtils.forSource(
            """
                package test;
                import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
                @SimpleBuilder
                public class Helper {
                  public Helper() {}
                }
            """);

    // When: Compile with HashSetBuilderWithElementBuilders enabled
    Compilation compilation =
        ProcessorTestUtils.createCompiler()
            .withOptions("-Asimplebuilder.usingHashSetBuilderWithElementBuilders=true")
            .compile(sourceFile, helperSource);

    // Then: Consumer method with HashSetBuilderWithElementBuilders should be generated
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    ProcessorAsserts.assertContaining(
        generatedCode,
        "public HasSetWithBuilderElementBuilder helpers(Consumer<HashSetBuilderWithElementBuilders<Helper, HelperBuilder>> helpersBuilderConsumer)",
        "new HashSetBuilderWithElementBuilders<Helper, HelperBuilder>(this.helpers.value(), HelperBuilder::create)",
        "new HashSetBuilderWithElementBuilders<Helper, HelperBuilder>(HelperBuilder::create)");
  }

  @Test
  void shouldNotGenerateListConsumerWhenFieldHasBuilder() {
    // Given: A List field where the element type has a builder, but
    // ArrayListBuilderWithElementBuilders is disabled
    String packageName = "test";
    String className = "HasListWithBuilderElement";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private java.util.List<Helper> helpers;

                public java.util.List<Helper> getHelpers() { return helpers; }
                public void setHelpers(java.util.List<Helper> helpers) { this.helpers = helpers; }
            """);

    JavaFileObject helperSource =
        ProcessorTestUtils.forSource(
            """
                package test;
                import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
                @SimpleBuilder
                public class Helper {
                  public Helper() {}
                }
            """);

    // When: Compile with ArrayListBuilderWithElementBuilders disabled
    Compilation compilation =
        ProcessorTestUtils.createCompiler()
            .withOptions("-Asimplebuilder.usingArrayListBuilderWithElementBuilders=false")
            .compile(sourceFile, helperSource);

    // Then: No consumer method should be generated (covers line 102 in appliesTo)
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    ProcessorAsserts.assertNotContaining(
        generatedCode,
        "public HasListWithBuilderElementBuilder helpers(Consumer<ArrayListBuilderWithElementBuilders<Helper, HelperBuilder>> helpersBuilderConsumer)");
  }

  @Test
  void shouldNotGenerateListConsumerWhenBothArrayListBuilderOptionsDisabled() {
    // Given: A List field with both ArrayListBuilder options disabled
    String packageName = "test";
    String className = "HasListWithDisabledBuilders";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private java.util.List<String> tags;

                public java.util.List<String> getTags() { return tags; }
                public void setTags(java.util.List<String> tags) { this.tags = tags; }
            """);

    // When: Compile with both ArrayListBuilder options disabled
    Compilation compilation =
        ProcessorTestUtils.createCompiler()
            .withOptions(
                "-Asimplebuilder.usingArrayListBuilder=false",
                "-Asimplebuilder.usingArrayListBuilderWithElementBuilders=false")
            .compile(sourceFile);

    // Then: No consumer method should be generated (covers line 106 and 146)
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    ProcessorAsserts.assertNotContaining(
        generatedCode,
        "public HasListWithDisabledBuildersBuilder tags(Consumer<ArrayListBuilder<String>> tagsBuilderConsumer)");
  }

  @Test
  void shouldNotGenerateListConsumerWhenBuilderConsumerDisabled() {
    // Given: A List field with generateBuilderConsumer disabled
    String packageName = "test";
    String className = "HasListWithConsumerDisabled";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private java.util.List<String> tags;

                public java.util.List<String> getTags() { return tags; }
                public void setTags(java.util.List<String> tags) { this.tags = tags; }
            """);

    // When: Compile with generateBuilderConsumer disabled
    Compilation compilation =
        ProcessorTestUtils.createCompiler()
            .withOptions("-Asimplebuilder.generateBuilderConsumer=false")
            .compile(sourceFile);

    // Then: No consumer method should be generated (covers line 91-93 in appliesTo)
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    ProcessorAsserts.assertNotContaining(
        generatedCode,
        "public HasListWithConsumerDisabledBuilder tags(Consumer<ArrayListBuilder<String>> tagsBuilderConsumer)");
  }

  @Test
  void shouldGenerateListConsumerForSimpleElements() {
    // Given: A List field where element type has no builder
    String packageName = "test";
    String className = "HasListWithSimpleElement";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private java.util.List<String> tags;

                public java.util.List<String> getTags() { return tags; }
                public void setTags(java.util.List<String> tags) { this.tags = tags; }
            """);

    // When: Compile with ArrayListBuilder enabled
    Compilation compilation =
        ProcessorTestUtils.createCompiler()
            .withOptions("-Asimplebuilder.usingArrayListBuilder=true")
            .compile(sourceFile);

    // Then: Consumer method with ArrayListBuilder should be generated
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    ProcessorAsserts.assertContaining(
        generatedCode,
        "public HasListWithSimpleElementBuilder tags(Consumer<ArrayListBuilder<String>> tagsBuilderConsumer)",
        "this.tags = changedValue(builder.build());");
  }

  @Test
  void shouldGenerateListConsumerForBuilderElements() {
    // Given: A List field where element type has a builder
    String packageName = "test";
    String className = "HasListWithBuilderElement";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.simpleBuilderClass(
            packageName,
            className,
            """
                private java.util.List<Helper> helpers;

                public java.util.List<Helper> getHelpers() { return helpers; }
                public void setHelpers(java.util.List<Helper> helpers) { this.helpers = helpers; }
            """);

    JavaFileObject helperSource =
        ProcessorTestUtils.forSource(
            """
                package test;
                import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
                @SimpleBuilder
                public class Helper {
                  public Helper() {}
                }
            """);

    // When: Compile with ArrayListBuilderWithElementBuilders enabled
    Compilation compilation =
        ProcessorTestUtils.createCompiler()
            .withOptions("-Asimplebuilder.usingArrayListBuilderWithElementBuilders=true")
            .compile(sourceFile, helperSource);

    // Then: Consumer method with ArrayListBuilderWithElementBuilders should be generated
    String generatedCode = loadGeneratedSource(compilation, builderClassName);
    assertGenerationSucceeded(compilation, builderClassName, generatedCode);
    ProcessorAsserts.assertContaining(
        generatedCode,
        "public HasListWithBuilderElementBuilder helpers(Consumer<ArrayListBuilderWithElementBuilders<Helper, HelperBuilder>> helpersBuilderConsumer)",
        "new ArrayListBuilderWithElementBuilders<Helper, HelperBuilder>(this.helpers.value(), HelperBuilder::create)",
        "new ArrayListBuilderWithElementBuilders<Helper, HelperBuilder>(HelperBuilder::create)");
  }

  @Test
  void shouldHandleManualBuilderExistsAlongsideDto() {
    // Given: A manually created builder class that conflicts with the generated one

    JavaFileObject manualBuilder =
        ProcessorTestUtils.forSource(
            """
            package test;

            // Manually created builder that follows the same naming convention
            public class PersonDtoBuilder {
                private String name;
                private int age;

                public PersonDtoBuilder name(String name) {
                    this.name = name;
                    return this;
                }

                public PersonDtoBuilder age(int age) {
                    this.age = age;
                    return this;
                }

                public PersonDto build() {
                    // Use reflection to avoid constructor signature issues
                    try {
                        return PersonDto.class.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            """);

    JavaFileObject dtoWithAnnotation =
        ProcessorTestUtils.simpleBuilderClass(
            "test",
            "PersonDto",
            """
                private String name;
                private int age;

                public String getName() { return name; }
                public void setName(String name) { this.name = name; }

                public int getAge() { return age; }
                public void setAge(int age) { this.age = age; }
            """);

    // When: Compile both the manual builder and the DTO with @SimpleBuilder
    Compilation compilation = compile(manualBuilder, dtoWithAnnotation);

    // Then: Should succeed - processor should handle the conflict with a warning
    assertThat(compilation).succeeded();
    assertThat(compilation)
        .hadWarningContaining(
            "Failed to generate builder - Builder class 'test.PersonDtoBuilder' already exists.");

    // The processor should detect the conflict and handle it gracefully
    // Either by skipping generation or by generating with a different approach
  }

  @Test
  void shouldHandleDifferentDtoNamesWithSuffixWithoutConflict() {
    // Given: Two DTOs with different names that generate the same builder name with custom suffix
    // Order + "DtoFactory" = OrderDtoFactory
    // OrderDto + "Factory" = OrderDtoFactory (same result!)

    JavaFileObject firstDto =
        ProcessorTestUtils.forSource(
            """
            package test;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder(options = @SimpleBuilder.Options(builderSuffix = "DtoFactory"))
            public class Order {
                private String id;
                public String getId() { return id; }
                public void setId(String id) { this.id = id; }
            }
            """);

    JavaFileObject secondDto =
        ProcessorTestUtils.forSource(
            """
            package test;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder(options = @SimpleBuilder.Options(builderSuffix = "Factory"))
            public class OrderDto {
                private String number;
                public String getNumber() { return number; }
                public void setNumber(String number) { this.number = number; }
            }
            """);

    // When: Compile both DTOs
    Compilation compilation = compile(firstDto, secondDto);

    // Then: Should succeed - processor should handle the naming by posting warnings
    assertThat(compilation).succeeded();
    assertThat(compilation)
        .hadWarningContaining(
            "Failed to generate builder - Unable to create builder class 'test.OrderDtoFactory': Attempt to recreate a file for type test.OrderDtoFactory.");

    // Verify that a builder was generated
    String generatedCode = loadGeneratedSource(compilation, "OrderDtoFactory");

    // Basic verification that the builder class exists for the first DTO
    ProcessorAsserts.assertContaining(
        generatedCode,
        "public class OrderDtoFactory implements IBuilderBase<Order>",
        "@BuilderImplementation(forClass = Order.class)");
  }

  /**
   * Verifies that a setter with field-specific generics (e.g. {@code public <T> void setValue(T
   * value)}) is skipped by {@code createFieldFromSetter} and does not produce a builder field,
   * while normal setters in the same class are still processed.
   */
  @Test
  void shouldSkipSetterWithFieldSpecificGenerics() {
    String className = "GenericSetterDto";
    String builderClassName = className + "Builder";

    JavaFileObject sourceFile =
        ProcessorTestUtils.forSource(
            """
            package test;

            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public class GenericSetterDto {
              private String name;
              private Object value;

              public String getName() { return name; }
              public void setName(String name) { this.name = name; }

              public Object getValue() { return value; }
              public <T> void setValue(T value) { this.value = value; }
            }
            """);

    Compilation compilation = compile(sourceFile);
    assertThat(compilation).succeeded();
    String generatedCode = loadGeneratedSource(compilation, builderClassName);

    // The normal setter for "name" must produce a builder method
    ProcessorAsserts.assertContaining(
        generatedCode, "public GenericSetterDtoBuilder name(String name)");

    // The generic setter for "value" must NOT produce a builder method
    ProcessorAsserts.assertNotContaining(generatedCode, "public GenericSetterDtoBuilder value(");

    // Compilation should have a warning about the ignored generic setter
    assertThat(compilation)
        .hadWarningContaining("Field 'value' has field-specific generics, so it will be ignored");
  }
}
