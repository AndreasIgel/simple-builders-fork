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

package org.javahelpers.simple.builders.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.javahelpers.simple.builders.core.enums.AccessModifier;
import org.javahelpers.simple.builders.core.enums.OptionState;

/**
 * Annotation to mark classes and records for builder generation.
 *
 * <p>Place this annotation directly on a class or record to trigger generation of a fluent builder
 * class with support for various patterns and helper methods. Can be used standalone or combined
 * with {@link Options} for fine-grained control.
 *
 * <p><b>When to use {@code @SimpleBuilder} vs {@link Template}:</b>
 *
 * <ul>
 *   <li>Use {@code @SimpleBuilder} directly on a class/record for one-off builder generation.
 *   <li>Use {@link Template} on a <b>custom annotation declaration</b> to create a reusable
 *       configuration preset that can be applied to many classes. {@code @SimpleBuilder.Template}
 *       cannot be placed on a class or record directly; it is only valid on annotation types
 *       ({@link ElementType#ANNOTATION_TYPE}).
 * </ul>
 *
 * <p>Available configuration options:
 *
 * <ul>
 *   <li><b>Field Setters:</b> generateFieldSupplier, generateFieldConsumer, generateBuilderConsumer
 *       (all default: true)
 *   <li><b>Conditional Logic:</b> generateConditionalHelper (default: true)
 *   <li><b>Access Control:</b> builderAccess, builderConstructorAccess, methodAccess (default:
 *       PUBLIC)
 *   <li><b>Collection Helpers:</b> generateVarArgsHelpers, usingArrayListBuilder,
 *       usingArrayListBuilderWithElementBuilders, usingHashSetBuilder,
 *       usingHashSetBuilderWithElementBuilders, usingHashMapBuilder (all default: true)
 *   <li><b>Integration:</b> generateWithInterface (default: true)
 *   <li><b>Documentation:</b> generateJavaDoc (default: true)
 * </ul>
 *
 * <p>This annotation is itself a built-in {@link Template}: it is meta-annotated with
 * {@code @SimpleBuilder.Template(options = @Options())}. When placed on a class or record, the
 * processor treats it like any other template annotation. The optional {@link #options()} attribute
 * on a concrete {@code @SimpleBuilder} usage overrides the template defaults.
 *
 * <p>Use {@link Template} to create reusable configuration presets for project- or layer-specific
 * conventions. A custom template annotation is an annotation type that is itself meta-annotated
 * with {@code @SimpleBuilder.Template(options = @SimpleBuilder.Options(...))} and then applied to
 * classes and records.
 *
 * <p>This annotation is {@link Inherited}: a subclass of an annotated type is treated as if it also
 * carried {@code @SimpleBuilder} for the purpose of triggering builder generation, unless it is
 * explicitly excluded via {@link Ignore4BuilderGeneration}. The {@link Template} meta-annotation is
 * {@link Inherited} as well, so custom template annotations that are themselves {@code @Inherited}
 * propagate to subclasses in the same way. Configuration options declared on the parent's
 * {@code @SimpleBuilder(options = ...)} or template are also inherited by subclass builders.
 *
 * <p>Related annotations:
 *
 * <ul>
 *   <li>{@link IgnoreInBuilder} - exclude individual setters/constructors from builder generation
 *   <li>{@link Ignore4BuilderGeneration} - exclude a class/record from builder generation, even
 *       when an inherited {@code @SimpleBuilder} or {@code @SimpleBuilder.Template} would otherwise
 *       trigger it
 * </ul>
 *
 * @see Options
 * @see Template
 * @see IgnoreInBuilder
 * @see Ignore4BuilderGeneration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@Inherited
@SimpleBuilder.Template
public @interface SimpleBuilder {

  /**
   * Configuration options for builder generation.
   *
   * <p>Allows inline configuration of builder generation options:
   *
   * <pre>{@code
   * @SimpleBuilder(options = @SimpleBuilder.Options(
   *     builderAccess = AccessModifier.PACKAGE_PRIVATE,
   *     generateFieldSupplier = OptionState.DISABLED
   * ))
   * public class PersonDto { ... }
   * }</pre>
   *
   * @return the configuration options, or default (all UNSET) if not specified
   */
  Options options() default @Options();

  /**
   * Configuration options for builder generation.
   *
   * <p>Allows fine-grained control over what gets generated in the builder class. Used inline
   * within {@link SimpleBuilder} or as part of {@link Template}.
   *
   * <p>All options have sensible defaults and can be overridden via compiler options using {@code
   * -A} flag.
   */
  @Retention(RetentionPolicy.CLASS)
  @interface Options {
    // === Generation Options ===
    /**
     * Generate a supplier method by which the user of this builder could define a function, which
     * supplies the value for this field. <br>
     * The generated method has the parameter-type {@code Supplier<T>} with T being the type of the
     * field.
     *
     * <p>Example:
     *
     * <pre>{@code
     * PersonDto person = PersonDtoBuilder.create()
     *     .name(() -> fetchNameFromDatabase())
     *     .age(() -> calculateAge())
     *     .build();
     * }</pre>
     *
     * Default: ENABLED <br>
     * Compiler option: -Asimplebuilder.generateFieldSupplier
     */
    OptionState generateFieldSupplier() default OptionState.UNSET;

    /**
     * Generate a consumer method with parameter-type {@code Consumer<T>} with T being the type of
     * the field. <br>
     * This is only done for complex field types, so that users could use setter to change the
     * properties of that parameter.
     *
     * <p>Example:
     *
     * <pre>{@code
     * PersonDto person = PersonDtoBuilder.create()
     *     .address(addr -> {
     *         addr.setStreet("Main St");
     *         addr.setCity("Berlin");
     *     })
     *     .build();
     * }</pre>
     *
     * Default: ENABLED <br>
     * Compiler option: -Asimplebuilder.generateFieldConsumer
     */
    OptionState generateFieldConsumer() default OptionState.UNSET;

    /**
     * Generate a builder consumer method with parameter-type {@code Consumer<Builder<T>>} with T
     * being the type of the field <br>
     * This is only done for complex field types, which have a recognized builder so that users
     * could use the chained builder methods to set the value of this complex field.
     *
     * <p>Example:
     *
     * <pre>{@code
     * PersonDto person = PersonDtoBuilder.create()
     *     .address(ab -> ab
     *         .street("Main St")
     *         .city("Berlin")
     *         .zipCode("10115"))
     *     .build();
     * }</pre>
     *
     * Default: ENABLED <br>
     * Compiler option: -Asimplebuilder.generateBuilderConsumer
     */
    OptionState generateBuilderConsumer() default OptionState.UNSET;

    /**
     * Generate conditional logic method (conditional) <br>
     * Allows conditional execution of builder methods based on a boolean supplier.
     *
     * <p>Example:
     *
     * <pre>{@code
     * PersonDto person = PersonDtoBuilder.create()
     *     .name("John")
     *     .conditional(() -> includeEmail, b -> b.email("john@example.com"))
     *     .conditional(() -> isPremium, b -> b.memberLevel("GOLD"))
     *     .build();
     * }</pre>
     *
     * Default: ENABLED <br>
     * Compiler option: -Asimplebuilder.generateConditionalHelper
     */
    OptionState generateConditionalHelper() default OptionState.UNSET;

    // === Access Control ===
    /**
     * Access level for the generated builder class.
     *
     * <ul>
     *   <li><b>PUBLIC</b> - For public APIs (default)
     *   <li><b>PACKAGE_PRIVATE</b> - For internal use within a package
     * </ul>
     *
     * <p><b>Note:</b> {@code PRIVATE} is <b>not allowed</b> for builder classes. Java does not
     * allow private top-level classes, so using {@code PRIVATE} will cause builder generation to
     * fail with a clear error message. Use {@code PACKAGE_PRIVATE} for internal builders instead.
     *
     * <p>Example:
     *
     * <pre>{@code
     * @SimpleBuilder(options = @SimpleBuilder.Options(
     *     builderAccess = AccessModifier.PACKAGE_PRIVATE
     * ))
     * public class PersonDto {
     *     // Generates: class PersonDtoBuilder (package-private)
     * }
     * }</pre>
     *
     * <p>Default: {@link AccessModifier#PUBLIC PUBLIC}
     *
     * <p>Compiler option: -Asimplebuilder.builderAccess (values: PUBLIC, PACKAGE_PRIVATE)
     *
     * @see #builderConstructorAccess() for controlling constructor visibility
     */
    AccessModifier builderAccess() default AccessModifier.PUBLIC;

    /**
     * Access level for generated builder constructors.
     *
     * <p>Common pattern: Use PRIVATE constructors with PUBLIC static factory methods (create()).
     *
     * <p>Example:
     *
     * <pre>{@code
     * @SimpleBuilder(options = @SimpleBuilder.Options(
     *     builderConstructorAccess = AccessModifier.PRIVATE
     * ))
     * public class PersonDto {
     *     // Generates: private PersonDtoBuilder() and private PersonDtoBuilder(PersonDto)
     *     // Use via: PersonDtoBuilder.create() or PersonDtoBuilder.from(instance)
     * }
     * }</pre>
     *
     * <p>Default: {@link AccessModifier#PUBLIC PUBLIC}
     *
     * <p>Compiler option: -Asimplebuilder.builderConstructorAccess (values: PUBLIC,
     * PACKAGE_PRIVATE, PRIVATE)
     */
    AccessModifier builderConstructorAccess() default AccessModifier.PUBLIC;

    /**
     * Access level for generated builder methods.
     *
     * <p>Typically matches builder class access. Use PACKAGE_PRIVATE for internal APIs.
     *
     * <p><b>Note:</b> {@code PRIVATE} is <b>not allowed</b> for builder methods. Private methods
     * would make all setter methods inaccessible, rendering the builder unusable. Using {@code
     * PRIVATE} will cause builder generation to fail with a clear error message.
     *
     * <p>Example:
     *
     * <pre>{@code
     * @SimpleBuilder(options = @SimpleBuilder.Options(
     *     methodAccess = AccessModifier.PACKAGE_PRIVATE
     * ))
     * public class PersonDto {
     *     // Generates: PersonDtoBuilder name(String name) (package-private)
     * }
     * }</pre>
     *
     * <p>Default: {@link AccessModifier#PUBLIC PUBLIC}
     *
     * <p>Compiler option: -Asimplebuilder.methodAccess (values: PUBLIC, PACKAGE_PRIVATE)
     */
    AccessModifier methodAccess() default AccessModifier.PUBLIC;

    // === Collection Options ===
    /**
     * Generate helper methods with VarArgs for Lists and Sets. <br>
     * Allows passing multiple elements directly instead of creating a list/set.
     *
     * <p>Example:
     *
     * <pre>{@code
     * PersonDto person = PersonDtoBuilder.create()
     *     .hobbies("Reading", "Gaming", "Cooking") // VarArgs instead of List.of(...)
     *     .build();
     * }</pre>
     *
     * Default: ENABLED <br>
     * Compiler option: -Asimplebuilder.generateVarArgsHelpers
     */
    OptionState generateVarArgsHelpers() default OptionState.UNSET;

    /**
     * Generate String format helper methods for String fields. <br>
     * Allows using String.format() style for setting string values.
     *
     * <p>Example:
     *
     * <pre>{@code
     * PersonDto person = PersonDtoBuilder.create()
     *     .name("Hello %s %s", firstName, lastName)
     *     .description("Age: %d, City: %s", age, city)
     *     .build();
     * }</pre>
     *
     * Default: ENABLED <br>
     * Compiler option: -Asimplebuilder.generateStringFormatHelpers
     */
    OptionState generateStringFormatHelpers() default OptionState.UNSET;

    /**
     * Generate add2FieldName helper methods for List and Set fields. <br>
     * Allows adding single elements to collections in a fluent way.
     *
     * <p>Example:
     *
     * <pre>{@code
     * PersonDto person = PersonDtoBuilder.create()
     *     .name("John")
     *     .add2Nicknames("Johnny")
     *     .add2Nicknames("JD")
     *     .add2Tags("developer")
     *     .build();
     * }</pre>
     *
     * Default: ENABLED <br>
     * Compiler option: -Asimplebuilder.generateAddToCollectionHelpers
     */
    OptionState generateAddToCollectionHelpers() default OptionState.UNSET;

    /**
     * Generate unboxed optional methods that accept the inner type T directly instead of {@code
     * Optional<T>}. <br>
     * For Optional fields, this generates a setter that accepts T and wraps it with
     * Optional.ofNullable().
     *
     * <p>Example:
     *
     * <pre>{@code
     * // Field: Optional<String> email
     * PersonDto person = PersonDtoBuilder.create()
     *     .email("john@example.com") // String instead of Optional.of("john@example.com")
     *     .build();
     * }</pre>
     *
     * Default: ENABLED <br>
     * Compiler option: -Asimplebuilder.generateUnboxedOptional
     */
    OptionState generateUnboxedOptional() default OptionState.UNSET;

    /**
     * Copy type annotations from the DTO fields to the builder fields/methods. <br>
     * Useful for validation annotations (e.g. @NotNull, @Size) or other metadata that should be
     * preserved.
     *
     * <p>Default: ENABLED <br>
     * Compiler option: -Asimplebuilder.copyTypeAnnotations
     */
    OptionState copyTypeAnnotations() default OptionState.UNSET;

    /**
     * Generate helper methods with a ArrayListBuilder supplier for lists instead of simple
     * supplier, which would not allow to use in a chanined way: <br>
     * Example with ArrayListBuilder: <br>
     *
     * <pre>{@code
     * MannschaftDto mannschaft = MannschaftDtoBuilder()
     *     .create()
     *     .mitglieder(mb -> mb.add("Max").add("Moritz"))
     *     .build();
     * }</pre>
     *
     * Instead of <br>
     *
     * <pre>{@code
     * MannschaftDto mannschaft = MannschaftDtoBuilder()
     *     .create()
     *     .mitglieder(mitglieder -> {
     *         mitglieder.add("Max");
     *         mitglieder.add("Moritz");
     *     })
     *     .build();
     * }</pre>
     *
     * Default: ENABLED Compiler option: -Asimplebuilder.usingArrayListBuilder
     */
    OptionState usingArrayListBuilder() default OptionState.UNSET;

    /**
     * Generate helper methods with a ArrayListBuilderWithElementBuilders supplier for lists of
     * complex objects instead of simple supplier, which would not allow to use in a chanined way:
     * <br>
     * Example with ArrayListBuilderWithElementBuilders: <br>
     *
     * <pre>{@code
     * MannschaftDto mannschaft = MannschaftDtoBuilder()
     *     .create()
     *     .mitglieder(mb -> mb
     *         .add(pb -> pb.name("Max").alter(20))
     *         .add(pb -> pb.name("Moritz").alter(22)))
     *     .build();
     * }</pre>
     *
     * Instead of <br>
     *
     * <pre>{@code
     * MannschaftDto mannschaft = MannschaftDtoBuilder()
     *     .create()
     *     .mitglieder(mitglieder -> {
     *         mitglieder.add(new PersonDto("Max", 20));
     *         mitglieder.add(new PersonDto("Moritz", 22));
     *     })
     *     .build();
     * }</pre>
     *
     * Default: ENABLED Compiler option: -Asimplebuilder.usingArrayListBuilderWithElementBuilders
     */
    OptionState usingArrayListBuilderWithElementBuilders() default OptionState.UNSET;

    /**
     * Generate helper methods with a ArrayListBuilder supplier for lists instead of simple
     * supplier, which would not allow to use in a chanined way: <br>
     * Example with ArrayListBuilder: <br>
     *
     * <pre>{@code
     * MannschaftDto mannschaft = MannschaftDtoBuilder()
     *     .create()
     *     .mitglieder(mb -> mb.add("Max").add("Moritz"))
     *     .build();
     * }</pre>
     *
     * Instead of <br>
     *
     * <pre>{@code
     * MannschaftDto mannschaft = MannschaftDtoBuilder()
     *     .create()
     *     .mitglieder(mitglieder -> {
     *         mitglieder.add("Max");
     *         mitglieder.add("Moritz");
     *     })
     *     .build();
     * }</pre>
     *
     * Default: ENABLED Compiler option: -Asimplebuilder.usingHashSetBuilder
     */
    OptionState usingHashSetBuilder() default OptionState.UNSET;

    /**
     * Generate helper methods with a HashSetBuilderWithElementBuilders supplier for lists of
     * complex objects instead of simple supplier, which would not allow to use in a chanined way:
     * <br>
     * Example with HashSetBuilderWithElementBuilders: <br>
     *
     * <pre>{@code
     * MannschaftDto mannschaft = MannschaftDtoBuilder()
     *     .create()
     *     .mitglieder(mb -> mb
     *         .add(pb -> pb.name("Max").alter(20))
     *         .add(pb -> pb.name("Moritz").alter(22)))
     *     .build();
     * }</pre>
     *
     * Instead of <br>
     *
     * <pre>{@code
     * MannschaftDto mannschaft = MannschaftDtoBuilder()
     *     .create()
     *     .mitglieder(mitglieder -> {
     *         mitglieder.add(new PersonDto("Max", 20));
     *         mitglieder.add(new PersonDto("Moritz", 22));
     *     })
     *     .build();
     * }</pre>
     *
     * Default: ENABLED Compiler option: -Asimplebuilder.usingHashSetBuilderWithElementBuilders
     */
    OptionState usingHashSetBuilderWithElementBuilders() default OptionState.UNSET;

    /**
     * Generate helper methods with a HashMapBuilder supplier for maps instead of simple supplier,
     * which would not allow to use in a chanined way: <br>
     * Example with HashMapBuilder: <br>
     *
     * <pre>{@code
     * MannschaftDto mannschaft = MannschaftDtoBuilder()
     *     .create()
     *     .mitglieder(mb -> mb.put(20, "Max").put(22, "Moritz"))
     *     .build();
     * }</pre>
     *
     * Instead of <br>
     *
     * <pre>{@code
     * MannschaftDto mannschaft = MannschaftDtoBuilder()
     *     .create()
     *     .mitglieder(mitglieder -> {
     *         mitglieder.put(20, "Max");
     *         mitglieder.put(22, "Moritz");
     *     })
     *     .build();
     * }</pre>
     *
     * Default: ENABLED Compiler option: -Asimplebuilder.usingHashMapBuilder
     */
    OptionState usingHashMapBuilder() default OptionState.UNSET;

    // === Annotations ===
    /**
     * Use {@code @Generated} annotation on the generated builder class. <br>
     * Marks the builder as generated code for tooling and analysis.
     *
     * <p>Example:
     *
     * <pre>{@code
     * @Generated("Generated by org.javahelpers.simple.builders.processor.BuilderProcessor")
     * public class PersonDtoBuilder {
     *     // ...
     * }
     * }</pre>
     *
     * Default: ENABLED <br>
     * Compiler option: -Asimplebuilder.usingGeneratedAnnotation
     */
    OptionState usingGeneratedAnnotation() default OptionState.UNSET;

    /**
     * Use {@code @BuilderImplementation} annotation on the generated builder class. <br>
     * Links the generated builder back to the original DTO class.
     *
     * <p>Example:
     *
     * <pre>{@code
     * @BuilderImplementation(PersonDto.class)
     * public class PersonDtoBuilder {
     *     // ...
     * }
     * }</pre>
     *
     * Default: ENABLED <br>
     * Compiler option: -Asimplebuilder.usingBuilderImplementationAnnotation
     */
    OptionState usingBuilderImplementationAnnotation() default OptionState.UNSET;

    // === Integration ===
    /**
     * Implement {@code IBuilderBase} interface in the generated builder class. <br>
     * Provides a common base interface for all generated builders.
     *
     * <p>Example:
     *
     * <pre>{@code
     * public class PersonDtoBuilder implements IBuilderBase<PersonDto> {
     *     @Override
     *     public PersonDto build() {
     *         // ...
     *     }
     * }
     * }</pre>
     *
     * Default: ENABLED <br>
     * Compiler option: -Asimplebuilder.implementsBuilderBase
     *
     * @return the option state for implementing BuilderBase interface
     */
    OptionState implementsBuilderBase() default OptionState.UNSET;

    /**
     * Generate With interface for integrating builder into DTOs. <br>
     * Creates a nested interface that can be implemented by the DTO for fluent updates.
     *
     * <p>Example:
     *
     * <pre>{@code
     * PersonDto updated = person.with(b -> b
     *     .name("New Name")
     *     .age(30));
     * }
     *
     * // Generated:
     * public interface WithPersonDto {
     *     default PersonDto with(Consumer<PersonDtoBuilder> updater) { ... }
     * }
     * }</pre>
     *
     * Default: ENABLED <br>
     * Compiler option: -Asimplebuilder.generateWithInterface
     *
     * @return the option state for generating With interface
     */
    OptionState generateWithInterface() default OptionState.UNSET;

    /**
     * Add Jackson annotations to the generated builder class. <br>
     * Adds {@code @JsonPOJOBuilder(withPrefix = "...")} to the builder class. The prefix matches
     * the configured {@link #setterSuffix()}.
     *
     * <p>Example:
     *
     * <pre>{@code
     * @JsonDeserialize(builder = PersonDtoBuilder.class)
     * public class PersonDto { ... }
     *
     * // Generated:
     * @JsonPOJOBuilder(withPrefix = "")
     * public class PersonDtoBuilder { ... }
     * }</pre>
     *
     * Default: DISABLED <br>
     * Compiler option: -Asimplebuilder.usingJacksonDeserializerAnnotation
     *
     * @return the option state for using Jackson deserializer annotation
     */
    OptionState usingJacksonDeserializerAnnotation() default OptionState.UNSET;

    /**
     * Generate a Jackson SimpleModule containing registrations for all generated builders. <br>
     * This module allows Jackson to use the generated builders for deserialization without needing
     * to annotate the DTO classes.
     *
     * <p>The generated module class will be named {@code SimpleBuildersJacksonModule} (by default).
     * By default, a module is generated in <b>each package</b> containing processed DTOs. To group
     * all registrations into a single module, use {@link #jacksonModulePackage()}.
     *
     * <p>Default: DISABLED <br>
     * Compiler option: -Asimplebuilder.generateJacksonModule
     *
     * @return the option state for generating Jackson module
     */
    OptionState generateJacksonModule() default OptionState.UNSET;

    /**
     * Specifies the package name where the {@code SimpleBuildersJacksonModule} class will be
     * generated. <br>
     * This is useful to avoid split-package issues or to group all module registrations into a
     * single module.
     *
     * <p>If not specified, a separate module will be generated in <b>each package</b> containing
     * processed DTOs.
     *
     * <p>Default: "" (empty - generate one module per package) <br>
     * Compiler option: -Asimplebuilder.jacksonModulePackage
     *
     * @return the package name for the Jackson module
     */
    String jacksonModulePackage() default "";

    /**
     * Generate Javadoc comments on the generated builder class and its members. <br>
     * When disabled, no class, field, constructor or method Javadoc is emitted, producing smaller
     * generated files.
     *
     * <p>Default: ENABLED <br>
     * Compiler option: -Asimplebuilder.generateJavaDoc
     *
     * @return the option state for generating Javadoc
     */
    OptionState generateJavaDoc() default OptionState.UNSET;

    // === Builder Scoping ===
    /**
     * Comma-separated list of packages for which builders are generated. <br>
     * Subpackages are included automatically. When non-empty, builder generation is restricted to
     * DTOs whose package equals or is a subpackage of a listed package. Types in this scope are
     * trusted to have their builder generated in the same compilation and may be referenced without
     * a type-existence search.
     *
     * <p>Example: {@code "com.example.dto, com.example.shared"}
     *
     * <p>Default: "" (empty - builders are generated for all {@code @SimpleBuilder} annotated
     * DTOs, no type search) <br>
     * Compiler option: -Asimplebuilder.builderGenerationPackages
     *
     * @return the packages for which builders are generated
     */
    String builderGenerationPackages() default "";

    /**
     * Comma-separated list of packages whose builders may be used as helper methods for other
     * DTOs. <br>
     * Subpackages are included automatically. A type in this scope but not in
     * {@link #builderGenerationPackages()} must have its compiled builder verified (via type
     * search) before a builder reference is emitted. If the builder type cannot be resolved, the
     * field falls back to a plain setter.
     *
     * <p>Example: {@code "com.example.library, com.example.external"}
     *
     * <p>Default: "" (empty - all annotated types may be referenced without type search) <br>
     * Compiler option: -Asimplebuilder.builderUsagePackages
     *
     * @return the packages whose builders may be used as helpers
     */
    String builderUsagePackages() default "";

    // === Naming ===
    /**
     * Suffix to append to the DTO name to generate the builder class name. <br>
     * For example, with suffix "Builder", a DTO named "PersonDto" will generate "PersonDtoBuilder".
     *
     * <p>Example:
     *
     * <pre>{@code
     * @SimpleBuilder(options = @SimpleBuilder.Options(
     *     builderSuffix = "Factory"
     * ))
     * public class PersonDto {
     *     // Generates: PersonDtoFactory instead of PersonDtoBuilder
     * }
     * }</pre>
     *
     * Default: "Builder" <br>
     * Compiler option: -Asimplebuilder.builderSuffix
     *
     * @return the suffix for the builder class name
     */
    String builderSuffix() default "Builder";

    /**
     * Suffix to append to setter method names in the generated builder. <br>
     * For example, with suffix "with", a field named "name" will generate "withName()". <br>
     * When a suffix is set, the field name is capitalized after the suffix.
     *
     * <p>Example:
     *
     * <pre>{@code
     * @SimpleBuilder(options = @SimpleBuilder.Options(
     *     setterSuffix = "with"
     * ))
     * public class PersonDto {
     *     // Generates: withName(String) instead of name(String)
     * }
     *
     * PersonDto person = PersonDtoBuilder.create()
     *     .withName("John")
     *     .withAge(25)
     *     .build();
     * }</pre>
     *
     * Default: "" (empty - no suffix) <br>
     * Compiler option: -Asimplebuilder.setterSuffix
     *
     * @return the suffix for setter method names
     */
    String setterSuffix() default "";

    /**
     * Formatting mode for the generated source code. <br>
     * Controls how the generated builder source is post-processed for readability.
     *
     * <p>Accepted values (case-insensitive):
     *
     * <ul>
     *   <li>{@code "jdt"} - Full Eclipse JDT formatting (default)
     *   <li>{@code "lightweight"} - Lightweight cosmetic formatting (tabs to spaces, javadoc fixup,
     *       blank line collapsing)
     *   <li>{@code "none"} - No formatting, raw Roaster output
     * </ul>
     *
     * <p>An empty string (the default) means "inherit from compiler argument {@code
     * -Asimplebuilder.formattingMode}", which itself defaults to {@code jdt}.
     *
     * <p>Example:
     *
     * <pre>{@code
     * @SimpleBuilder(options = @SimpleBuilder.Options(
     *     formattingMode = "lightweight"
     * ))
     * public class PersonDto { ... }
     * }</pre>
     *
     * Default: "" (empty - inherit from compiler argument) <br>
     * Compiler option: -Asimplebuilder.formattingMode
     *
     * @return the formatting mode for generated source code
     */
    String formattingMode() default "";
  }

  /**
   * Meta-annotation for creating custom SimpleBuilder annotation templates.
   *
   * <p>This meta-annotation is placed on a <b>custom annotation declaration</b> (i.e., an
   * {@code @interface}) to pre-configure SimpleBuilder options. The custom annotation can then be
   * applied to classes and records just like {@link SimpleBuilder}, and the processor will
   * automatically apply the configured options.
   *
   * <p>This annotation can <b>only</b> be placed on annotation types ({@link
   * ElementType#ANNOTATION_TYPE}); it cannot be used directly on a class or record. Use {@link
   * SimpleBuilder} for direct one-off annotation of classes, or use this meta-annotation to define
   * a reusable custom annotation for a shared configuration across many classes.
   *
   * <p>This meta-annotation is {@link Inherited}. Note that this only controls inheritance of the
   * {@code @SimpleBuilder.Template} meta-annotation itself; for a custom template annotation to
   * propagate to unannotated subclasses, the custom annotation must additionally be declared with
   * {@code @Inherited}. Without {@code @Inherited} on the custom annotation, only the exact type
   * carrying it gets a builder. As with {@link SimpleBuilder}, configuration options declared on
   * the template are also applied to inherited subclass builders, as long as the custom template
   * annotation is itself {@code @Inherited} and no direct annotation overrides it.
   *
   * <p>Example:
   *
   * <pre>{@code
   * @SimpleBuilder.Template(options = @SimpleBuilder.Options(
   *     generateFieldSupplier = true,
   *     generateFieldConsumer = true
   * ))
   * @Retention(RetentionPolicy.CLASS)
   * @Target(ElementType.TYPE)
   * public @interface FullFeaturedBuilder {
   * }
   *
   * // Usage - just use the template annotation, no @SimpleBuilder needed
   * @FullFeaturedBuilder
   * public class PersonDto {
   *     private String name;
   * }
   * }</pre>
   *
   * <p>To make the template propagate to subclasses, add {@code @Inherited} to the custom
   * annotation:
   *
   * <pre>{@code
   * @SimpleBuilder.Template(options = @SimpleBuilder.Options(...))
   * @Inherited
   * @Retention(RetentionPolicy.CLASS)
   * @Target(ElementType.TYPE)
   * public @interface FullFeaturedBuilder {}
   *
   * @FullFeaturedBuilder
   * public class ParentDto { ... }
   *
   * // ChildDto also gets a builder, because @FullFeaturedBuilder is @Inherited.
   * public class ChildDto extends ParentDto { ... }
   * }</pre>
   *
   * <p>Related annotations:
   *
   * <ul>
   *   <li>{@link IgnoreInBuilder} - exclude individual setters/constructors from builder generation
   *   <li>{@link Ignore4BuilderGeneration} - exclude a class/record from builder generation, even
   *       when this template is inherited from a parent type
   * </ul>
   *
   * @see IgnoreInBuilder
   * @see Ignore4BuilderGeneration
   */
  @Retention(RetentionPolicy.CLASS)
  @Target(ElementType.ANNOTATION_TYPE)
  @Inherited
  @interface Template {
    /**
     * The options to apply when this template is used. Defaults to an empty {@link Options}
     * instance, so templates inherit the built-in defaults unless options are explicitly set.
     *
     * @return the builder configuration options
     */
    Options options() default @Options();
  }
}
