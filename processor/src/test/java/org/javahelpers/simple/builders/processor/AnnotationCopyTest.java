package org.javahelpers.simple.builders.processor;

import static org.javahelpers.simple.builders.processor.testing.ProcessorAsserts.contains;
import static org.javahelpers.simple.builders.processor.testing.ProcessorTestUtils.createCompiler;
import static org.javahelpers.simple.builders.processor.testing.ProcessorTestUtils.createMockAnnotation;
import static org.javahelpers.simple.builders.processor.testing.ProcessorTestUtils.loadGeneratedSource;
import static org.javahelpers.simple.builders.processor.testing.ProcessorTestUtils.printDiagnosticsOnVerbose;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import javax.tools.JavaFileObject;
import org.javahelpers.simple.builders.processor.testing.ProcessorAsserts;
import org.junit.jupiter.api.Test;

/** Test that annotations from target class fields are copied to builder fields. */
class AnnotationCopyTest {

  private Compilation compileSources(JavaFileObject... sources) {
    Compilation compilation =
        createCompiler()
            .withOptions("-Asimplebuilder.generateMapperHelpers=DISABLED")
            .compile(sources);
    printDiagnosticsOnVerbose(compilation);
    return compilation;
  }

  @Test
  void annotations_copiedToBuilderFields() {
    String packageName = "test";

    JavaFileObject notNullAnnotation =
        createMockAnnotation(
            packageName + ".annotations", "NotNull", "ElementType.FIELD, ElementType.PARAMETER");

    JavaFileObject customAnnotation =
        createMockAnnotation(
            packageName + ".annotations",
            "CustomAnnotation",
            "ElementType.FIELD, ElementType.PARAMETER",
            "String value() default \"\";");

    JavaFileObject person =
        JavaFileObjects.forSourceString(
            packageName + ".Person",
            """
            package test;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
            import test.annotations.NotNull;
            import test.annotations.CustomAnnotation;

            @SimpleBuilder
            public class Person {
              private String name;
              private String email;
              private int age;

              public String getName() { return name; }
              public void setName(@NotNull String name) { this.name = name; }

              public String getEmail() { return email; }
              public void setEmail(@CustomAnnotation("email-field") String email) { this.email = email; }

              public int getAge() { return age; }
              public void setAge(int age) { this.age = age; }
            }
            """);

    Compilation compilation = compileSources(notNullAnnotation, customAnnotation, person);
    String generatedCode = loadGeneratedSource(compilation, "PersonBuilder");
    ProcessorAsserts.assertGenerationSucceeded(compilation, "PersonBuilder", generatedCode);

    // Verify that annotations are copied to builder method parameters
    ProcessorAsserts.assertingResult(
        generatedCode,
        // NotNull annotation should be on the name method parameter
        contains("name(@NotNull String name)"),
        // CustomAnnotation should be on the email method parameter
        contains("email(@CustomAnnotation(\"email-field\") String email)"));
  }

  @Test
  void annotations_constructorParameters_copiedToBuilderFields() {
    String packageName = "test.annotations.constructor";

    JavaFileObject notNullAnnotation =
        createMockAnnotation(packageName, "NotNull", "ElementType.FIELD, ElementType.PARAMETER");

    JavaFileObject positiveAnnotation =
        createMockAnnotation(packageName, "Positive", "ElementType.FIELD, ElementType.PARAMETER");

    JavaFileObject product =
        JavaFileObjects.forSourceString(
            packageName + ".Product",
            """
            package test.annotations.constructor;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public class Product {
              private final String name;
              private final double price;

              public Product(
                @NotNull String name,
                @Positive double price) {
                this.name = name;
                this.price = price;
              }

              public String getName() { return name; }
              public double getPrice() { return price; }
            }
            """);

    Compilation compilation = compileSources(notNullAnnotation, positiveAnnotation, product);
    String generatedCode = loadGeneratedSource(compilation, "ProductBuilder");
    ProcessorAsserts.assertGenerationSucceeded(compilation, "ProductBuilder", generatedCode);

    // Verify that annotations from constructor parameters are copied to builder method parameters
    ProcessorAsserts.assertingResult(
        generatedCode,
        // NotNull annotation should be on the name method parameter
        contains("name(@NotNull String name)"),
        // Positive annotation should be on the price method parameter
        contains("price(@Positive double price)"));
  }

  @Test
  void annotations_frameworkAnnotations_notCopied() {
    String packageName = "test.annotations.filtered";

    JavaFileObject notNullAnnotation =
        createMockAnnotation(packageName, "NotNull", "ElementType.FIELD, ElementType.PARAMETER");

    JavaFileObject person =
        JavaFileObjects.forSourceString(
            packageName + ".Person",
            """
            package test.annotations.filtered;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
            import org.javahelpers.simple.builders.core.annotations.IgnoreInBuilder;

            @SimpleBuilder
            public class Person {
              private String name;
              private String ignoredField;

              public String getName() { return name; }
              public void setName(@NotNull String name) { this.name = name; }

              public String getIgnoredField() { return ignoredField; }
              @IgnoreInBuilder
              public void setIgnoredField(String ignoredField) { this.ignoredField = ignoredField; }
            }
            """);

    Compilation compilation = compileSources(notNullAnnotation, person);
    String generatedCode = loadGeneratedSource(compilation, "PersonBuilder");
    ProcessorAsserts.assertGenerationSucceeded(compilation, "PersonBuilder", generatedCode);

    // Verify that NotNull annotation is copied to method parameters
    ProcessorAsserts.assertingResult(generatedCode, contains("name(@NotNull String name)"));

    // Verify that ignoredField is not in the builder (due to @IgnoreInBuilder)
    ProcessorAsserts.assertNotContaining(generatedCode, "ignoredField");
  }

  @Test
  void annotations_withComplexValues_copiedCorrectly() {
    String packageName = "test.complex";

    // Create an enum for testing
    JavaFileObject priorityEnum =
        JavaFileObjects.forSourceString(
            packageName + ".Priority",
            """
            package test.complex;
            public enum Priority {
              LOW, MEDIUM, HIGH
            }
            """);

    // Create a nested annotation
    JavaFileObject metadataAnnotation =
        JavaFileObjects.forSourceString(
            packageName + ".Metadata",
            """
            package test.complex;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;

            @Retention(RetentionPolicy.RUNTIME)
            public @interface Metadata {
              String author();
              int version();
            }
            """);

    // Create a complex annotation with various value types
    JavaFileObject complexAnnotation =
        createMockAnnotation(
            packageName,
            "ComplexAnnotation",
            "ElementType.FIELD, ElementType.PARAMETER",
            """
            // Primitives
            int intValue() default 42;
            long longValue() default 100L;
            boolean boolValue() default true;
            double doubleValue() default 3.14;

            // String
            String stringValue() default "default";

            // Enum
            Priority priority() default Priority.MEDIUM;

            // Class literal
            Class<?> type() default String.class;

            // Array
            String[] tags() default {};
            int[] numbers() default {};

            // Nested annotation
            Metadata metadata() default @Metadata(author = "unknown", version = 1);""");

    JavaFileObject task =
        JavaFileObjects.forSourceString(
            packageName + ".Task",
            """
            package test.complex;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public class Task {
              private String name;
              private String description;
              private String simpleField;

              public String getName() { return name; }
              public void setName(
                @ComplexAnnotation(
                  intValue = 123,
                  longValue = 999L,
                  boolValue = false,
                  doubleValue = 2.71,
                  stringValue = "test-value",
                  priority = Priority.HIGH,
                  type = Task.class,
                  tags = {"urgent", "important"},
                  numbers = {1, 2, 3},
                  metadata = @Metadata(author = "John", version = 2)
                ) String name) {
                this.name = name;
              }

              public String getDescription() { return description; }
              public void setDescription(@ComplexAnnotation String description) {
                this.description = description;
              }

              public String getSimpleField() { return simpleField; }
              public void setSimpleField(
                @ComplexAnnotation(stringValue = "custom", priority = Priority.LOW, tags = "single")
                String simpleField) {
                this.simpleField = simpleField;
              }
            }
            """);

    Compilation compilation =
        compileSources(priorityEnum, metadataAnnotation, complexAnnotation, task);
    String generatedCode = loadGeneratedSource(compilation, "TaskBuilder");
    ProcessorAsserts.assertGenerationSucceeded(compilation, "TaskBuilder", generatedCode);

    // Verify annotation with all value types is copied correctly
    ProcessorAsserts.assertingResult(
        generatedCode,
        // Method with full annotation containing all parameter types
        contains(
            """
            public TaskBuilder name(
                @ComplexAnnotation(intValue = 123, longValue = 999L, boolValue = false, doubleValue = 2.71, stringValue = "test-value", priority = test.complex.Priority.HIGH, type = test.complex.Task.class, tags = {"urgent", "important"}, numbers = {1, 2, 3}, metadata = @test.complex.Metadata(author="John", version=2)) String name)"""),
        // Method with annotation using only default values
        contains("public TaskBuilder description(@ComplexAnnotation String description)"),
        // Method with annotation with partial parameter override
        contains(
            """
            public TaskBuilder simpleField(
                @ComplexAnnotation(stringValue = "custom", priority = test.complex.Priority.LOW, tags = {"single"}) String simpleField)"""),
        // Format method should also have the full annotation
        contains(
            """
            public TaskBuilder name(
                @ComplexAnnotation(intValue = 123, longValue = 999L, boolValue = false, doubleValue = 2.71, stringValue = "test-value", priority = test.complex.Priority.HIGH, type = test.complex.Task.class, tags = {"urgent", "important"}, numbers = {1, 2, 3}, metadata = @test.complex.Metadata(author="John", version=2)) String format,"""),
        // Format method with partial override
        contains(
            """
            public TaskBuilder simpleField(
                @ComplexAnnotation(stringValue = "custom", priority = test.complex.Priority.LOW, tags = {"single"}) String format,"""));
  }

  @Test
  void annotations_deprecatedCopied_suppressWarningsFiltered() {
    String packageName = "test.javafilter";

    JavaFileObject service =
        JavaFileObjects.forSourceString(
            packageName + ".MyDto",
            """
            package test.javafilter;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public class MyDto {
              private String name;

              public String getName() { return name; }

              public void setName(
                  @Deprecated
                  @SuppressWarnings("unused") String name) {
                this.name = name;
              }
            }
            """);

    Compilation compilation = compileSources(service);
    String generatedCode = loadGeneratedSource(compilation, "MyDtoBuilder");
    ProcessorAsserts.assertGenerationSucceeded(compilation, "MyDtoBuilder", generatedCode);

    // @Deprecated is applied to the generated builder method itself (not the parameter —
    // the parameter is a new declaration, the deprecation is about the property).
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains(
            """
            @Deprecated
            public MyDtoBuilder name(String name) {"""));

    // Class-level @SuppressWarnings is present because the deprecated builder method may be
    // called internally by other generated methods (varargs helpers, etc.).
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains(
            """
            @SuppressWarnings({"deprecation", "removal"})
            public class MyDtoBuilder implements IBuilderBase<MyDto>"""));
  }

  @Test
  void annotations_deprecatedSetterMethod_methodLevelDeprecatedAndBuildSuppressed() {
    String packageName = "test.deprecated.setter";

    JavaFileObject service =
        JavaFileObjects.forSourceString(
            packageName + ".MyDto",
            """
            package test.deprecated.setter;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public class MyDto {
              private String name;

              public String getName() { return name; }

              @Deprecated
              public void setName(String name) {
                this.name = name;
              }
            }
            """);

    Compilation compilation = compileSources(service);
    String generatedCode = loadGeneratedSource(compilation, "MyDtoBuilder");
    ProcessorAsserts.assertGenerationSucceeded(compilation, "MyDtoBuilder", generatedCode);

    // The generated builder method IS @Deprecated because the setter is the write API for the
    // property and the builder method replaces it — deprecation propagates.
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains(
            """
            @Deprecated
            public MyDtoBuilder name(String name) {"""));

    // build() calls the deprecated setter via result::setName; the whole builder class carries a
    // class-level @SuppressWarnings so all internal calls to deprecated members are silenced.
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains(
            """
            @SuppressWarnings({"deprecation", "removal"})
            public class MyDtoBuilder implements IBuilderBase<MyDto>"""));
  }

  @Test
  void annotations_deprecatedRecordComponent_methodLevelDeprecated() {
    String packageName = "test.deprecated.record";

    JavaFileObject book =
        JavaFileObjects.forSourceString(
            packageName + ".Book",
            """
            package test.deprecated.record;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public record Book(@Deprecated String title, int pages) {}
            """);

    Compilation compilation = compileSources(book);
    String generatedCode = loadGeneratedSource(compilation, "BookBuilder");
    ProcessorAsserts.assertGenerationSucceeded(compilation, "BookBuilder", generatedCode);

    // The generated builder method for the deprecated record component is @Deprecated (on the
    // method, not the parameter — the parameter is a new declaration).
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains(
            """
            @Deprecated
            public BookBuilder title(String title) {"""));

    // The non-deprecated field is not annotated — include the preceding Javadoc closing so
    // that an @Deprecated annotation between them would break the match.
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains(
            """
             */
              public BookBuilder pages(int pages) {"""));
  }

  @Test
  void annotations_deprecatedDtoClass_builderAndFactoryMethodsDeprecated() {
    String packageName = "test.deprecated.dto";

    // The DTO class itself is @Deprecated. This must propagate @Deprecated to the generated
    // builder class, both constructors, and the create() factory method. The builder class
    // also needs class-level @SuppressWarnings because it internally instantiates the
    // deprecated DTO and calls its constructor. The field-level builder method (name()) and
    // build() are NOT @Deprecated because only the class is deprecated, not the field/param/
    // setter. Most builder options are DISABLED to keep the generated code minimal enough for
    // a single full text-block comparison.
    JavaFileObject service =
        JavaFileObjects.forSourceString(
            packageName + ".MyDto",
            """
            package test.deprecated.dto;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
            import org.javahelpers.simple.builders.core.enums.OptionState;

            @Deprecated
            @SimpleBuilder(options = @SimpleBuilder.Options(
                generateFieldSupplier = OptionState.DISABLED,
                generateFieldConsumer = OptionState.DISABLED,
                generateBuilderConsumer = OptionState.DISABLED,
                generateConditionalHelper = OptionState.DISABLED,
                generateVarArgsHelpers = OptionState.DISABLED,
                generateStringFormatHelpers = OptionState.DISABLED,
                generateAddToCollectionHelpers = OptionState.DISABLED,
                generateWithInterface = OptionState.DISABLED
            ))
            public class MyDto {
              private String name;

              public String getName() { return name; }

              public void setName(String name) {
                this.name = name;
              }
            }
            """);

    Compilation compilation = compileSources(service);
    String generatedCode = loadGeneratedSource(compilation, "MyDtoBuilder");
    ProcessorAsserts.assertGenerationSucceeded(compilation, "MyDtoBuilder", generatedCode);

    // Full text-block comparison of the generated builder class. With most options disabled,
    // the output is minimal enough to compare comprehensively. The text block includes the
    // complete class from annotations to closing brace — imports are omitted as they are not
    // relevant to the deprecation feature.
    ProcessorAsserts.assertContaining(
        generatedCode,
        """
        @Generated("Generated by org.javahelpers.simple.builders.processor.BuilderProcessor")
        @BuilderImplementation(forClass = MyDto.class)
        @Deprecated
        @SuppressWarnings({"deprecation", "removal"})
        public class MyDtoBuilder implements IBuilderBase<MyDto> {

          /**
           * Tracked value for <code>name</code>: name.
           */
          private TrackedValue<String> name = unsetValue();

          /**
           * Empty constructor of builder for {@code test.deprecated.dto.MyDto}.
           */
          @Deprecated
          public MyDtoBuilder() {
          }

          /**
           * Initialisation of builder for {@code test.deprecated.dto.MyDto} by a instance.
           *
           * @param instance object instance for initialisiation
           */
          @Deprecated
          public MyDtoBuilder(MyDto instance) {
            this.name = initialValue(instance.getName());
          }

          /**
           * Creating a new builder for {@code test.deprecated.dto.MyDto}.
           *
           * <h4>Example:</h4>
           *
           * <pre>{@code
           * MyDtoBuilder builder = MyDtoBuilder.create();
           * }</pre>
           *
           * @return builder for {@code test.deprecated.dto.MyDto}
           */
          @Deprecated
          public static MyDtoBuilder create() {
            return new MyDtoBuilder();
          }

          /**
           * Sets the value for <code>name</code>.
           * <p>
           * Generated from setter {@link MyDto#setName(String) setName(String name)}
           *
           * <h4>Example:</h4>
           *
           * <pre>{@code
           * builder.name("example value");
           * }</pre>
           *
           * @param name name
           * @return current instance of builder
           */
          public MyDtoBuilder name(String name) {
            this.name = changedValue(name);
            return this;
          }

          /**
           * Builds the configured DTO instance.
           *
           * <h4>Example:</h4>
           *
           * <pre>{@code
           * MyDto result = builder.build();
           * }</pre>
           */
          @Override
          public MyDto build() {
            MyDto result = new MyDto();
            this.name.ifSet(result::setName);
            return result;
          }

          /**
           * Returns a string representation of this builder, including only fields that have been set.
           *
           * @return string representation of the builder
           */
          @Override
          public String toString() {
            return new ToStringBuilder(this, BuilderToStringStyle.INSTANCE).append("name", this.name).toString();
          }
        }
        """);
  }

  @Test
  void annotations_deprecatedDtoClassWithJavadoc_deprecatedJavadocPropagatedToBuilderClass() {
    String packageName = "test.deprecated.dto.javadoc";

    // The DTO class is @Deprecated AND has an @deprecated javadoc tag. The @deprecated javadoc
    // text must be propagated to the generated builder class javadoc, the create() factory method
    // javadoc, and the constructor javadoc. This covers the case where addDeprecatedJavadoc is
    // called with a non-null deprecatedJavaDoc and a potentially null pre-existing javadoc.
    JavaFileObject myDto =
        JavaFileObjects.forSourceString(
            packageName + ".MyDto",
            """
            package test.deprecated.dto.javadoc;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
            import org.javahelpers.simple.builders.core.enums.OptionState;

            /**
             * A DTO that is now obsolete.
             *
             * @deprecated use {@link NewDto} instead
             */
            @Deprecated
            @SimpleBuilder(options = @SimpleBuilder.Options(
                generateFieldSupplier = OptionState.DISABLED,
                generateFieldConsumer = OptionState.DISABLED,
                generateBuilderConsumer = OptionState.DISABLED,
                generateConditionalHelper = OptionState.DISABLED,
                generateVarArgsHelpers = OptionState.DISABLED,
                generateStringFormatHelpers = OptionState.DISABLED,
                generateAddToCollectionHelpers = OptionState.DISABLED,
                generateWithInterface = OptionState.DISABLED
            ))
            public class MyDto {
              private String name;

              public String getName() { return name; }

              public void setName(String name) {
                this.name = name;
              }
            }
            """);

    Compilation compilation = compileSources(myDto);
    String generatedCode = loadGeneratedSource(compilation, "MyDtoBuilder");
    ProcessorAsserts.assertGenerationSucceeded(compilation, "MyDtoBuilder", generatedCode);

    // The @deprecated javadoc from the DTO class is propagated to the builder class
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains(
            """
             * @deprecated use {@link NewDto} instead"""));

    // The builder class itself is @Deprecated (with @SuppressWarnings in between)
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains(
            """
            @Deprecated
            @SuppressWarnings({"deprecation", "removal"})
            public class MyDtoBuilder implements IBuilderBase<MyDto>"""));
  }

  @Test
  void annotations_deprecatedJavadocText_propagatedToBuilderMethods() {
    String packageName = "test.deprecated.javadoc";

    JavaFileObject service =
        JavaFileObjects.forSourceString(
            packageName + ".MyDto",
            """
            package test.deprecated.javadoc;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public class MyDto {
              private String name;

              public String getName() { return name; }

              /**
               * Sets the name.
               *
               * @deprecated use {@link #label} instead
               */
              @Deprecated
              public void setName(String name) {
                this.name = name;
              }
            }
            """);

    Compilation compilation = compileSources(service);
    String generatedCode = loadGeneratedSource(compilation, "MyDtoBuilder");
    ProcessorAsserts.assertGenerationSucceeded(compilation, "MyDtoBuilder", generatedCode);

    // The @deprecated javadoc text is propagated to the generated builder method
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains(
            """
             * @deprecated use {@link #label} instead"""));

    // The method itself is @Deprecated (detected from the deprecated setter)
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains(
            """
            @Deprecated
            public MyDtoBuilder name(String name) {"""));
  }

  @Test
  void annotations_deprecatedWithSinceAndForRemoval_attributesPreserved() {
    String packageName = "test.deprecated.attrs";

    JavaFileObject service =
        JavaFileObjects.forSourceString(
            packageName + ".MyDto",
            """
            package test.deprecated.attrs;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public class MyDto {
              private String name;

              public String getName() { return name; }

              @Deprecated(since = "1.2", forRemoval = true)
              public void setName(String name) {
                this.name = name;
              }
            }
            """);

    Compilation compilation = compileSources(service);
    String generatedCode = loadGeneratedSource(compilation, "MyDtoBuilder");
    ProcessorAsserts.assertGenerationSucceeded(compilation, "MyDtoBuilder", generatedCode);

    // The since and forRemoval attributes are preserved on the generated builder method
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains(
            """
            @Deprecated(since = "1.2", forRemoval = true)
            public MyDtoBuilder name(String name) {"""));
  }

  @Test
  void annotations_deprecatedGetter_fromInstanceConstructorSuppressed() {
    String packageName = "test.deprecated.getter";

    JavaFileObject service =
        JavaFileObjects.forSourceString(
            packageName + ".MyDto",
            """
            package test.deprecated.getter;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public class MyDto {
              private String name;

              @Deprecated
              public String getName() { return name; }

              public void setName(String name) {
                this.name = name;
              }
            }
            """);

    Compilation compilation = compileSources(service);
    String generatedCode = loadGeneratedSource(compilation, "MyDtoBuilder");
    ProcessorAsserts.assertGenerationSucceeded(compilation, "MyDtoBuilder", generatedCode);

    // A deprecated getter does NOT propagate @Deprecated to the generated builder method
    // (deprecated getter != deprecated property). The class carries a class-level
    // @SuppressWarnings because the from-instance constructor calls the deprecated getter.
    // The constructor itself does NOT carry its own @SuppressWarnings — suppression is only
    // at class level.
    ProcessorAsserts.assertingResult(
        generatedCode,
        // name() is not @Deprecated — include preceding Javadoc closing to prove it
        contains(
            """
             */
              public MyDtoBuilder name(String name) {"""),
        // Class-level @SuppressWarnings is present
        contains(
            """
            @SuppressWarnings({"deprecation", "removal"})
            public class MyDtoBuilder implements IBuilderBase<MyDto>"""),
        // From-instance constructor: no own @SuppressWarnings (preceding Javadoc closing),
        // and the body calls the deprecated getter (instance.getName())
        contains(
            """
             */
              public MyDtoBuilder(MyDto instance) {
                this.name = initialValue(instance.getName());
              }"""));
  }

  @Test
  void annotations_deprecatedFieldType_methodLevelDeprecatedAndClassSuppressed() {
    String packageName = "test.deprecated.fieldtype";

    // The field type itself is @Deprecated. The builder method for this field should be
    // @Deprecated (consumers calling it would get deprecation warnings from the deprecated
    // parameter type), and the class needs @SuppressWarnings because the builder internally
    // uses the deprecated type.
    JavaFileObject dto =
        JavaFileObjects.forSourceString(
            packageName + ".MyDto",
            """
            package test.deprecated.fieldtype;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public class MyDto {
              private OldType value;

              public OldType getValue() { return value; }

              public void setValue(OldType value) {
                this.value = value;
              }
            }
            """);

    JavaFileObject oldType =
        JavaFileObjects.forSourceString(
            packageName + ".OldType",
            """
            package test.deprecated.fieldtype;

            @Deprecated
            public class OldType {
              private String data;

              public OldType() {}

              public String getData() { return data; }

              public void setData(String data) {
                this.data = data;
              }
            }
            """);

    Compilation compilation = compileSources(dto, oldType);
    String generatedCode = loadGeneratedSource(compilation, "MyDtoBuilder");
    ProcessorAsserts.assertGenerationSucceeded(compilation, "MyDtoBuilder", generatedCode);

    // The builder method is @Deprecated because the field type is deprecated
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains(
            """
            @Deprecated
            public MyDtoBuilder value(OldType value) {"""));

    // Class-level @SuppressWarnings is present because the builder internally uses the
    // deprecated type (e.g. TrackedValue<OldType>, from-instance constructor)
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains(
            """
            @SuppressWarnings({"deprecation", "removal"})
            public class MyDtoBuilder implements IBuilderBase<MyDto>"""));
  }

  @Test
  void annotations_deprecatedElementBuilder_classLevelSuppressed() {
    String packageName = "test.deprecated.elementbuilder";

    // The element type has its own @SimpleBuilder, and the element type is @Deprecated.
    // The generated collection helper in MyDtoBuilder calls ItemForDeprecationBuilder.create()
    // internally, which produces deprecation warnings — so MyDtoBuilder needs
    // class-level @SuppressWarnings.
    JavaFileObject dto =
        JavaFileObjects.forSourceString(
            packageName + ".MyDto",
            """
            package test.deprecated.elementbuilder;
            import java.util.List;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public class MyDto {
              private List<ItemForDeprecation> items;

              public List<ItemForDeprecation> getItems() { return items; }

              public void setItems(List<ItemForDeprecation> items) {
                this.items = items;
              }
            }
            """);

    JavaFileObject itemDto =
        JavaFileObjects.forSourceString(
            packageName + ".ItemForDeprecation",
            """
            package test.deprecated.elementbuilder;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @Deprecated
            @SimpleBuilder
            public class ItemForDeprecation {
              private String name;

              public String getName() { return name; }

              public void setName(String name) {
                this.name = name;
              }
            }
            """);

    Compilation compilation = compileSources(dto, itemDto);
    String generatedCode = loadGeneratedSource(compilation, "MyDtoBuilder");
    ProcessorAsserts.assertGenerationSucceeded(compilation, "MyDtoBuilder", generatedCode);

    // Class-level @SuppressWarnings is present because the builder internally calls
    // ItemForDeprecationBuilder.create() which is @Deprecated
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains(
            """
            @SuppressWarnings({"deprecation", "removal"})
            public class MyDtoBuilder implements IBuilderBase<MyDto>"""));
  }

  @Test
  void annotations_frameworkAnnotations_filtered() {
    String packageName = "test.framework";

    // Create an annotation in the SimpleBuilder framework package
    JavaFileObject frameworkAnnotation =
        createMockAnnotation(
            "org.javahelpers.simple.builders.custom",
            "FrameworkAnnotation",
            "ElementType.FIELD, ElementType.PARAMETER",
            "String value() default \"\";");

    JavaFileObject entity =
        JavaFileObjects.forSourceString(
            packageName + ".Entity",
            """
            package test.framework;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
            import org.javahelpers.simple.builders.custom.FrameworkAnnotation;

            @SimpleBuilder
            public class Entity {
              private String id;

              public String getId() { return id; }

              public void setId(
                  @FrameworkAnnotation("internal") String id) {
                this.id = id;
              }
            }
            """);

    Compilation compilation = compileSources(frameworkAnnotation, entity);
    String generatedCode = loadGeneratedSource(compilation, "EntityBuilder");
    ProcessorAsserts.assertGenerationSucceeded(compilation, "EntityBuilder", generatedCode);

    // Verify that SimpleBuilder framework annotations are NOT copied to builder methods
    ProcessorAsserts.assertingResult(
        generatedCode,
        // Method should exist without the framework annotation
        contains("public EntityBuilder id(String id)"));

    // Framework annotation should not appear anywhere in generated code
    ProcessorAsserts.assertNotContaining(generatedCode, "@FrameworkAnnotation");
  }

  @Test
  void annotations_customAnnotations_copiedCorrectly() {
    String packageName = "test.custom";

    // Create a custom annotation that should NOT be filtered
    JavaFileObject validAnnotation =
        createMockAnnotation(
            packageName,
            "ValidAnnotation",
            "ElementType.FIELD, ElementType.PARAMETER",
            "String value() default \"\";");

    JavaFileObject model =
        JavaFileObjects.forSourceString(
            packageName + ".Model",
            """
            package test.custom;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public class Model {
              private String data;

              public String getData() { return data; }
              public void setData(@ValidAnnotation("keep-me") String data) {
                this.data = data;
              }
            }
            """);

    Compilation compilation = compileSources(validAnnotation, model);
    String generatedCode = loadGeneratedSource(compilation, "ModelBuilder");
    ProcessorAsserts.assertGenerationSucceeded(compilation, "ModelBuilder", generatedCode);

    // Verify that custom annotations ARE copied (not filtered out)
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains(
            """
            public ModelBuilder data(
                @ValidAnnotation("keep-me") String data)"""));
  }

  @Test
  void annotations_generatedAnnotations_notCopied() {
    String packageName = "test.generated";

    // Create Generated annotation (commonly used by code generators)
    JavaFileObject generatedAnnotation =
        createMockAnnotation(
            "javax.annotation",
            "Generated",
            "ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER",
            "String[] value();");

    JavaFileObject model =
        JavaFileObjects.forSourceString(
            packageName + ".Model",
            """
            package test.generated;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
            import javax.annotation.Generated;

            @SimpleBuilder
            public class Model {
              private String code;

              public String getCode() { return code; }
              public void setCode(@Generated(\"SomeGenerator\") String code) {
                this.code = code;
              }
            }
            """);

    Compilation compilation = compileSources(generatedAnnotation, model);
    String generatedCode = loadGeneratedSource(compilation, "ModelBuilder");
    ProcessorAsserts.assertGenerationSucceeded(compilation, "ModelBuilder", generatedCode);

    // Verify that @Generated annotations are NOT copied to parameters (they're metadata, not
    // validation)
    ProcessorAsserts.assertingResult(
        generatedCode,
        // Method should exist without @Generated on the parameter
        contains("public ModelBuilder code(String code)"));

    // The annotation should not be on the method parameter
    ProcessorAsserts.assertNotContaining(generatedCode, "code(@Generated");
  }

  @Test
  void annotations_emptyArrayValues_formattedCorrectly() {
    String packageName = "test.emptyarray";

    JavaFileObject arrayAnnotation =
        createMockAnnotation(
            packageName,
            "ArrayAnnotation",
            "ElementType.FIELD, ElementType.PARAMETER",
            "String[] tags() default {};\n  int[] numbers() default {};");

    JavaFileObject data =
        JavaFileObjects.forSourceString(
            packageName + ".Data",
            """
            package test.emptyarray;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public class Data {
              private String value;

              public String getValue() { return value; }
              public void setValue(@ArrayAnnotation(tags = {}, numbers = {}) String value) {
                this.value = value;
              }
            }
            """);

    Compilation compilation = compileSources(arrayAnnotation, data);
    String generatedCode = loadGeneratedSource(compilation, "DataBuilder");
    ProcessorAsserts.assertGenerationSucceeded(compilation, "DataBuilder", generatedCode);

    // Verify empty arrays are formatted correctly
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains("@ArrayAnnotation(tags = {}, numbers = {})"),
        contains(
            "public DataBuilder value(@ArrayAnnotation(tags = {}, numbers = {}) String value)"));
  }

  @Test
  void annotations_mixedPrimitiveArrays_formattedCorrectly() {
    String packageName = "test.mixedarray";

    JavaFileObject rangeAnnotation =
        createMockAnnotation(
            packageName,
            "Range",
            "ElementType.FIELD, ElementType.PARAMETER",
            "int[] values();\n  double[] decimals() default {1.0, 2.0};");

    JavaFileObject measurement =
        JavaFileObjects.forSourceString(
            packageName + ".Measurement",
            """
            package test.mixedarray;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public class Measurement {
              private String sensor;

              public String getSensor() { return sensor; }
              public void setSensor(
                @Range(values = {0, 100, 255}, decimals = {0.5, 1.5, 2.5}) String sensor) {
                this.sensor = sensor;
              }
            }
            """);

    Compilation compilation = compileSources(rangeAnnotation, measurement);
    String generatedCode = loadGeneratedSource(compilation, "MeasurementBuilder");
    ProcessorAsserts.assertGenerationSucceeded(compilation, "MeasurementBuilder", generatedCode);

    // Verify mixed primitive arrays are formatted correctly
    ProcessorAsserts.assertingResult(
        generatedCode,
        contains(
            """
            public MeasurementBuilder sensor(
                @Range(values = {0, 100, 255}, decimals = {0.5, 1.5, 2.5}) String sensor)"""),
        // Format method should also have the annotation
        contains(
            """
            public MeasurementBuilder sensor(
                @Range(values = {0, 100, 255}, decimals = {0.5, 1.5, 2.5}) String format,"""));
  }

  @Test
  void annotations_validAnnotation_jakartaValidation_filteredFromBuilderParameters() {
    String packageName = "test.jakarta";

    JavaFileObject address =
        JavaFileObjects.forSourceString(
            packageName + ".Address",
            """
            package test.jakarta;

            public class Address {
              private String street;
              private String city;

              public String getStreet() { return street; }
              public void setStreet(String street) { this.street = street; }

              public String getCity() { return city; }
              public void setCity(String city) { this.city = city; }
            }
            """);

    JavaFileObject person =
        JavaFileObjects.forSourceString(
            packageName + ".Person",
            """
            package test.jakarta;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
            import jakarta.validation.Valid;
            import jakarta.validation.constraints.NotNull;

            @SimpleBuilder
            public class Person {
              private String name;
              private Address address;

              public String getName() { return name; }
              public void setName(@NotNull String name) { this.name = name; }

              public Address getAddress() { return address; }
              public void setAddress(@Valid @NotNull Address address) { this.address = address; }
            }
            """);

    Compilation compilation =
        compileSources(
            createJakartaValidAnnotation(), createJakartaNotNullAnnotation(), address, person);
    String generatedCode = loadGeneratedSource(compilation, "PersonBuilder");
    ProcessorAsserts.assertGenerationSucceeded(compilation, "PersonBuilder", generatedCode);

    ProcessorAsserts.assertingResult(
        generatedCode,
        contains("name(@NotNull String name)"),
        contains("address(@NotNull Address address)"));

    ProcessorAsserts.assertNotContaining(generatedCode, "@Valid");
  }

  @Test
  void annotations_validAnnotation_javaxValidation_filteredFromBuilderParameters() {
    String packageName = "test.javax";

    JavaFileObject contact =
        JavaFileObjects.forSourceString(
            packageName + ".Contact",
            """
            package test.javax;

            public class Contact {
              private String email;
              private String phone;

              public String getEmail() { return email; }
              public void setEmail(String email) { this.email = email; }

              public String getPhone() { return phone; }
              public void setPhone(String phone) { this.phone = phone; }
            }
            """);

    JavaFileObject customer =
        JavaFileObjects.forSourceString(
            packageName + ".Customer",
            """
            package test.javax;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
            import javax.validation.Valid;
            import javax.validation.constraints.Size;

            @SimpleBuilder
            public class Customer {
              private String id;
              private Contact contact;

              public String getId() { return id; }
              public void setId(@Size(min = 5, max = 20) String id) { this.id = id; }

              public Contact getContact() { return contact; }
              public void setContact(@Valid Contact contact) { this.contact = contact; }
            }
            """);

    Compilation compilation =
        compileSources(
            createMockAnnotation(
                "javax.validation",
                "Valid",
                "ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD"),
            createMockAnnotation(
                "javax.validation.constraints",
                "Size",
                "ElementType.FIELD, ElementType.PARAMETER",
                "int min() default 0;\n  int max() default Integer.MAX_VALUE;"),
            contact,
            customer);
    String generatedCode = loadGeneratedSource(compilation, "CustomerBuilder");
    ProcessorAsserts.assertGenerationSucceeded(compilation, "CustomerBuilder", generatedCode);

    ProcessorAsserts.assertingResult(
        generatedCode,
        contains("id(@Size(min = 5, max = 20) String id)"),
        contains("contact(Contact contact)"));

    ProcessorAsserts.assertNotContaining(generatedCode, "@Valid");
  }

  @Test
  void annotations_validAnnotation_constructorParameters_filteredFromBuilderParameters() {
    String packageName = "test.constructor";

    JavaFileObject department =
        JavaFileObjects.forSourceString(
            packageName + ".Department",
            """
            package test.constructor;

            public class Department {
              private String name;

              public String getName() { return name; }
              public void setName(String name) { this.name = name; }
            }
            """);

    JavaFileObject employee =
        JavaFileObjects.forSourceString(
            packageName + ".Employee",
            """
            package test.constructor;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
            import jakarta.validation.Valid;
            import jakarta.validation.constraints.NotNull;

            @SimpleBuilder
            public class Employee {
              private final String name;
              private final Department department;

              public Employee(
                  @NotNull String name,
                  @Valid @NotNull Department department) {
                this.name = name;
                this.department = department;
              }

              public String getName() { return name; }
              public Department getDepartment() { return department; }
            }
            """);

    Compilation compilation =
        compileSources(
            createJakartaValidAnnotation(), createJakartaNotNullAnnotation(), department, employee);
    String generatedCode = loadGeneratedSource(compilation, "EmployeeBuilder");
    ProcessorAsserts.assertGenerationSucceeded(compilation, "EmployeeBuilder", generatedCode);

    ProcessorAsserts.assertingResult(
        generatedCode,
        contains("name(@NotNull String name)"),
        contains("department(@NotNull Department department)"));

    ProcessorAsserts.assertNotContaining(generatedCode, "@Valid");
  }

  @Test
  void annotations_validAnnotation_mixedWithConstraints_onlyValidFiltered() {
    String packageName = "test.mixed";

    JavaFileObject metadata =
        JavaFileObjects.forSourceString(
            packageName + ".Metadata",
            """
            package test.mixed;

            public class Metadata {
              private String key;
              private String value;

              public String getKey() { return key; }
              public void setKey(String key) { this.key = key; }

              public String getValue() { return value; }
              public void setValue(String value) { this.value = value; }
            }
            """);

    JavaFileObject document =
        JavaFileObjects.forSourceString(
            packageName + ".Document",
            """
            package test.mixed;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
            import jakarta.validation.Valid;
            import jakarta.validation.constraints.NotNull;
            import jakarta.validation.constraints.Size;

            @SimpleBuilder
            public class Document {
              private String title;
              private Metadata metadata;

              public String getTitle() { return title; }
              public void setTitle(@NotNull @Size(min = 1, max = 100) String title) {
                this.title = title;
              }

              public Metadata getMetadata() { return metadata; }
              public void setMetadata(@Valid @NotNull Metadata metadata) {
                this.metadata = metadata;
              }
            }
            """);

    Compilation compilation =
        compileSources(
            createJakartaValidAnnotation(),
            createJakartaNotNullAnnotation(),
            createMockAnnotation(
                "jakarta.validation.constraints",
                "Size",
                "ElementType.FIELD, ElementType.PARAMETER",
                "int min() default 0;\n  int max() default Integer.MAX_VALUE;"),
            metadata,
            document);
    String generatedCode = loadGeneratedSource(compilation, "DocumentBuilder");
    ProcessorAsserts.assertGenerationSucceeded(compilation, "DocumentBuilder", generatedCode);

    ProcessorAsserts.assertingResult(
        generatedCode,
        contains("title(@NotNull @Size(min = 1, max = 100) String title)"),
        contains("metadata(@NotNull Metadata metadata)"));

    ProcessorAsserts.assertNotContaining(generatedCode, "@Valid");
  }

  /**
   * Creates a mock jakarta.validation.Valid annotation for testing.
   *
   * <p>This helper is used multiple times across different test methods.
   *
   * @return a JavaFileObject representing the mocked Valid annotation
   */
  private JavaFileObject createJakartaValidAnnotation() {
    return createMockAnnotation(
        "jakarta.validation",
        "Valid",
        "ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD");
  }

  /**
   * Creates a mock jakarta.validation.constraints.NotNull annotation for testing.
   *
   * <p>This helper is used multiple times across different test methods.
   *
   * @return a JavaFileObject representing the mocked NotNull annotation
   */
  private JavaFileObject createJakartaNotNullAnnotation() {
    return createMockAnnotation(
        "jakarta.validation.constraints", "NotNull", "ElementType.FIELD, ElementType.PARAMETER");
  }
}
