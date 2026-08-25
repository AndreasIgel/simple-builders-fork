# Configuration

Simple-builders supports fine-grained configuration through the `@SimpleBuilder.Options` annotation and compiler options.

## Table of Contents

- [Overview](#overview)
- [Annotation Configuration](#annotation-configuration)
- [Template Annotations](#template-annotations)
- [Excluding Types from Builder Generation](#excluding-types-from-builder-generation)
- [Compiler Options](#compiler-options)
  - [Maven Configuration](#maven-configuration)
  - [Gradle Configuration](#gradle-configuration)
  - [IntelliJ IDEA Configuration](#intellij-idea-configuration)
- [Configuration Options](#configuration-options)
  - [Field Setter Generation](#field-setter-generation)
  - [Conditional Logic](#conditional-logic)
  - [Access Control](#access-control)
  - [Collection Helpers](#collection-helpers)
  - [Component Filtering](#component-filtering)
  - [Integration](#integration)
  - [Documentation](#documentation)
  - [Reliability](#reliability)
  - [Performance Tracking](#performance-tracking)
- [Examples](#examples)
  - [Minimal Builder](#minimal-builder)
  - [Internal API Builder](#internal-api-builder)
  - [Collection-Heavy Builder](#collection-heavy-builder)
  - [Minimal Builder Template](#minimal-builder-template)
  - [Project-Wide Defaults](#project-wide-defaults)
- [Priority Rules](#priority-rules)
  - [Example: Priority in Action](#example-priority-in-action)
- [AccessModifier Enum](#accessmodifier-enum)
- [Troubleshooting](#troubleshooting)
  - [Compiler Options Not Working](#compiler-options-not-working)
  - [Annotation Values Not Applied](#annotation-values-not-applied)
  - [Access Level Issues](#access-level-issues)
  - [Template Annotations Not Working](#template-annotations-not-working)
- [Best Practices](#best-practices)
- [Reference](#reference)
  - [All Compiler Options](#all-compiler-options)
  - [Complete Options Example](#complete-options-example)

## Overview

Configuration follows a priority system:
1. **Annotation values** - Highest priority
2. **Compiler options** - Medium priority  
3. **Default values** - Lowest priority

This allows you to set project-wide defaults while still being able to override them per-class when needed.

## Annotation Configuration

Configure individual builders using `@SimpleBuilder` with `@SimpleBuilder.Options`:

```java
@SimpleBuilder
@SimpleBuilder.Options(
    generateFieldSupplier = OptionState.ENABLED,
    generateFieldConsumer = OptionState.ENABLED,
    generateBuilderConsumer = OptionState.ENABLED,
    generateConditionalHelper = OptionState.ENABLED,
    builderAccess = AccessModifier.PUBLIC,
    methodAccess = AccessModifier.PUBLIC,
    generateVarArgsHelpers = OptionState.ENABLED,
    usingArrayListBuilder = OptionState.ENABLED,
    usingHashMapBuilder = OptionState.ENABLED,
    generateWithInterface = OptionState.ENABLED
)
public class PersonDto {
    private String name;
    private int age;
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
}
```

## Template Annotations

`@SimpleBuilder.Template` is a **meta-annotation**: it is placed on a custom annotation declaration (`@interface`), not directly on a class or record. Use it to create reusable configuration presets that can be applied to many classes with a single custom annotation. `@SimpleBuilder` itself is the built-in template: it is meta-annotated with `@SimpleBuilder.Template` and can be used directly on a class for one-off builder generation, or you can define your own custom template annotations.

| Need | Use |
|------|-----|
| Generate a builder for a single class/record | `@SimpleBuilder` on the class/record |
| Generate a minimal builder (no optional features) | `@SimpleMinimalBuilder` on the class/record |
| Share the same configuration across many classes | `@SimpleBuilder.Template` on a custom `@interface`, then the custom annotation on each class |

Create reusable configuration presets with custom template annotations. The built-in `@SimpleMinimalBuilder` already disables every optional feature; use the following pattern only when you need a differently named or customized template:

```java
@SimpleBuilder.Template(options = @SimpleBuilder.Options(
    generateFieldSupplier = OptionState.DISABLED,
    generateFieldConsumer = OptionState.DISABLED,
    generateBuilderConsumer = OptionState.DISABLED,
    generateConditionalHelper = OptionState.DISABLED,
    generateVarArgsHelpers = OptionState.DISABLED,
    generateStringFormatHelpers = OptionState.DISABLED,
    generateAddToCollectionHelpers = OptionState.DISABLED,
    generateUnboxedOptional = OptionState.DISABLED,
    copyTypeAnnotations = OptionState.DISABLED,
    usingArrayListBuilder = OptionState.DISABLED,
    usingArrayListBuilderWithElementBuilders = OptionState.DISABLED,
    usingHashSetBuilder = OptionState.DISABLED,
    usingHashSetBuilderWithElementBuilders = OptionState.DISABLED,
    usingHashMapBuilder = OptionState.DISABLED,
    generateWithInterface = OptionState.DISABLED,
    usingGeneratedAnnotation = OptionState.DISABLED,
    usingBuilderImplementationAnnotation = OptionState.DISABLED,
    implementsBuilderBase = OptionState.DISABLED,
    usingJacksonDeserializerAnnotation = OptionState.DISABLED,
    generateJacksonModule = OptionState.DISABLED,
    generateJavaDoc = OptionState.DISABLED
))
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface MinimalBuilder {}
```

Then use your template:

```java
@MinimalBuilder  // No need for @SimpleBuilder - template includes it!
public class PersonDto {
    private String name;
}
```

To make a template annotation propagate to unannotated subclasses, add `@Inherited` to it. `@SimpleBuilder` itself is `@Inherited`, and `@SimpleBuilder.Template` is `@Inherited` as well, but the custom annotation must also declare `@Inherited` for the processor to pick up subclasses:

```java
@SimpleBuilder.Template(options = @SimpleBuilder.Options(...))
@Inherited
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface MinimalBuilder {}

@MinimalBuilder
public class ParentDto { ... }

// ChildDto also gets a builder, because @MinimalBuilder is @Inherited.
public class ChildDto extends ParentDto { ... }
```

> **Note:** Inherited subclasses get a builder that also uses the options declared on the parent's `@SimpleBuilder(options = ...)` or template. If the subclass declares its own `@SimpleBuilder` or template annotation, those options take precedence.

## Excluding Types from Builder Generation

You can opt a whole DTO out of builder generation with `@Ignore4BuilderGeneration`. This is useful when a class inherits `@SimpleBuilder` (which is itself `@Inherited`) or an `@SimpleBuilder.Template`-based annotation from a parent and you do not want a builder for that specific subclass.

```java
import org.javahelpers.simple.builders.core.annotations.Ignore4BuilderGeneration;

@Ignore4BuilderGeneration
public class IgnoredDto extends ParentDto {
    // No builder will be generated for IgnoredDto, even if ParentDto carries
    // @SimpleBuilder or an @Inherited template annotation.
}
```

A type marked with `@Ignore4BuilderGeneration` is treated as having **no builder available**. Other builders that reference it will fall back to plain setters instead of emitting nested-builder consumers. The annotation is intentionally **not** `@Inherited`, so it only suppresses the exact type it is placed on and does not cascade to further subclasses.

## Compiler Options

Set project-wide defaults via compiler options. These apply to all builders unless overridden by annotations.

### Maven Configuration

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.11.0</version>
    <configuration>
        <source>17</source>
        <target>17</target>
        <annotationProcessorPaths>
            <path>
                <groupId>io.github.java-helpers</groupId>
                <artifactId>simple-builders-processor</artifactId>
                <version>${simple-builders.version}</version>
            </path>
        </annotationProcessorPaths>
        <compilerArgs>
            <arg>-Asimplebuilder.generateFieldSupplier=ENABLED</arg>
            <arg>-Asimplebuilder.generateFieldConsumer=ENABLED</arg>
            <arg>-Asimplebuilder.builderAccess=PUBLIC</arg>
            <arg>-Asimplebuilder.usingArrayListBuilder=ENABLED</arg>
        </compilerArgs>
    </configuration>
</plugin>
```

### Gradle Configuration

```gradle
dependencies {
    annotationProcessor "io.github.java-helpers:simple-builders-processor:${simpleBuildersVersion}"
}

compileJava {
    options.compilerArgs += [
        "-Asimplebuilder.generateFieldSupplier=ENABLED",
        "-Asimplebuilder.generateFieldConsumer=ENABLED",
        "-Asimplebuilder.builderAccess=PUBLIC"
    ]
}
```

### IntelliJ IDEA Configuration

1. Go to **Settings → Build, Execution, Deployment → Compiler → Java Compiler**
2. Add to **Additional command line parameters**:
   ```
   -Asimplebuilder.generateFieldSupplier=true -Asimplebuilder.builderAccess=PUBLIC
   ```

## Configuration Options

All options use `OptionState` enum with values: `ENABLED`, `DISABLED`, or `UNSET` (uses default/compiler arg).

### Field Setter Generation

#### `generateFieldSupplier`

**Default**: `ENABLED` | **Compiler Option**: `-Asimplebuilder.generateFieldSupplier=ENABLED|DISABLED`

Generates setter methods that accept `Supplier<T>` for lazy field value initialization.

**When ENABLED**:
```java
// Generated method
public PersonDtoBuilder name(Supplier<String> nameSupplier) {
    this.name = changedValue(nameSupplier.get());
    return this;
}

// Usage
PersonDto person = PersonDtoBuilder.create()
    .name(() -> expensiveNameComputation())
    .build();
```

**When DISABLED**: No `Supplier<>` setter methods are generated.

---

#### `generateFieldConsumer`

**Default**: `ENABLED` | **Compiler Option**: `-Asimplebuilder.generateFieldConsumer=ENABLED|DISABLED`

Generates setter methods that accept `Consumer<StringBuilder>` for String fields, allowing fluent string building.

**When ENABLED**:
```java
// Generated method for String fields
public PersonDtoBuilder name(Consumer<StringBuilder> nameConsumer) {
    StringBuilder builder = new StringBuilder();
    nameConsumer.accept(builder);
    this.name = changedValue(builder.toString());
    return this;
}

// Usage
PersonDto person = PersonDtoBuilder.create()
    .name(sb -> sb.append("Dr. ").append(firstName).append(" ").append(lastName))
    .build();
```

**When DISABLED**: No `Consumer<StringBuilder>` methods are generated.

---

#### `generateBuilderConsumer`

**Default**: `ENABLED` | **Compiler Option**: `-Asimplebuilder.generateBuilderConsumer=ENABLED|DISABLED`

Generates setter methods that accept `Consumer<Builder>` for complex nested objects and collections.

**When ENABLED**:
```java
// Generated method for collection fields
public PersonDtoBuilder tags(Consumer<ArrayListBuilder<String>> tagsConsumer) {
    ArrayListBuilder<String> builder = new ArrayListBuilder<>();
    tagsConsumer.accept(builder);
    this.tags = changedValue(builder.build());
    return this;
}

// Usage
PersonDto person = PersonDtoBuilder.create()
    .tags(list -> list.add("java").add("kotlin").add("scala"))
    .build();
```

**When DISABLED**: No builder consumer methods are generated.

---

### Conditional Logic

#### `generateConditionalHelper`

**Default**: `ENABLED` | **Compiler Option**: `-Asimplebuilder.generateConditionalHelper=ENABLED|DISABLED`

Generates conditional helper methods for fluent conditional logic in builder chains.

**When ENABLED**:
```java
// Generated methods
public PersonDtoBuilder conditional(BooleanSupplier condition,
    Consumer<PersonDtoBuilder> trueCase, 
    Consumer<PersonDtoBuilder> falseCase) { ... }

public PersonDtoBuilder conditional(BooleanSupplier condition,
    Consumer<PersonDtoBuilder> yesCondition) { ... }

// Usage
PersonDto person = PersonDtoBuilder.create()
    .name("John")
    .conditional(() -> isPremiumUser, 
        builder -> builder.premiumFeatures(true),
        builder -> builder.premiumFeatures(false))
    .build();
```

**When DISABLED**: No conditional helper methods are generated.

---

### Access Control

#### `builderAccess`

**Default**: `PUBLIC` | **Compiler Option**: `-Asimplebuilder.builderAccess=PUBLIC|PACKAGE_PRIVATE`

Controls the visibility of the generated builder class.

**Supported Values**: 
- `PUBLIC` - Builder accessible from anywhere (default, recommended for public APIs)
- `PACKAGE_PRIVATE` - Builder only accessible within the same package (good for internal APIs)

⚠️ **Error**: `PRIVATE` is **not allowed** for `builderAccess`. If you try to use it, builder generation will fail with an error message explaining that Java does not allow private top-level classes. The builder will not be generated, but other DTOs in your project will continue processing normally.

**Example with PACKAGE_PRIVATE**:
```java
// Generated builder
class PersonDtoBuilder implements IBuilderBase<PersonDto> {  // No 'public' keyword
    // ... only accessible within the same package
}
```

**Use case**: Use `PACKAGE_PRIVATE` for DTOs that are internal to your package and shouldn't have their builders exposed publicly.

---

#### `builderConstructorAccess`

**Default**: `PUBLIC` | **Compiler Option**: `-Asimplebuilder.builderConstructorAccess=PUBLIC|PACKAGE_PRIVATE|PRIVATE`

Controls the visibility of the builder's constructors.

**Supported Values**: 
- `PUBLIC` - Constructors accessible from anywhere (default)
- `PACKAGE_PRIVATE` - Constructors only accessible within the same package
- `PRIVATE` - Constructors only accessible via static factory methods ✅ **Recommended pattern**

**Example with PRIVATE** (recommended for API design):
```java
// Generated constructors
private PersonDtoBuilder() { }
private PersonDtoBuilder(PersonDto instance) { ... }

// Usage - forced to use static factory methods
PersonDtoBuilder builder = PersonDtoBuilder.create();  // ✅ OK
new PersonDtoBuilder()  // ❌ Compilation error - constructor is private
```

**Use case**: Use `PRIVATE` constructors to enforce using the static `create()` factory method, preventing direct instantiation and ensuring consistent builder creation patterns.

---

#### `methodAccess`

**Default**: `PUBLIC` | **Compiler Option**: `-Asimplebuilder.methodAccess=PUBLIC|PACKAGE_PRIVATE`

Controls the visibility of all generated setter methods.

**Supported Values**: 
- `PUBLIC` - Methods accessible from anywhere (default, recommended)
- `PACKAGE_PRIVATE` - Methods only accessible within the same package

⚠️ **Error**: `PRIVATE` is **not allowed** for `methodAccess`. If you try to use it, builder generation will fail with an error message explaining that all setter methods would be inaccessible. The builder will not be generated, but other DTOs in your project will continue processing normally.

**Example with PACKAGE_PRIVATE**:
```java
// Generated methods without 'public' modifier
PersonDtoBuilder name(String name) {  // Package-private
    this.name = changedValue(name);
    return this;
}
```

**Use case**: Rarely needed. Consider using `PACKAGE_PRIVATE` only when the entire builder API should be internal to the package.

---

### Helper Methods

#### `generateVarArgsHelpers`

**Default**: `ENABLED` | **Compiler Option**: `-Asimplebuilder.generateVarArgsHelpers=ENABLED|DISABLED`

Generates varargs methods for List and Set fields for convenient multi-value initialization.

**When ENABLED**:
```java
// Generated method
public PersonDtoBuilder tags(String... tags) {
    this.tags = changedValue(Arrays.asList(tags));
    return this;
}

// Usage
PersonDto person = PersonDtoBuilder.create()
    .tags("java", "kotlin", "scala")  // Varargs syntax
    .build();
```

**When DISABLED**: No varargs methods are generated; must use collection directly.

---

#### `generateStringFormatHelpers`

**Default**: `ENABLED` | **Compiler Option**: `-Asimplebuilder.generateStringFormatHelpers=ENABLED|DISABLED`

Generates `String.format()` helper methods for String and Optional<String> fields.

**When ENABLED**:
```java
// For String title field:
public BookDtoBuilder title(String format, Object... args) {
  this.title = changedValue(String.format(format, args));
  return this;
}

// For Optional<String> subtitle field:
public BookDtoBuilder subtitle(String format, Object... args) {
  this.subtitle = changedValue(Optional.of(String.format(format, args)));
  return this;
}

// Usage
BookDto book = BookDtoBuilder.create()
    .title("The %s Guide", "Complete")
    .subtitle("A comprehensive %s tutorial for %s", "Java", "beginners")
    .build();
```

**When DISABLED**: No format helper methods are generated.

---

#### `generateAddToCollectionHelpers`

**Default**: `ENABLED` | **Compiler Option**: `-Asimplebuilder.generateAddToCollectionHelpers=ENABLED|DISABLED`

Generates `add2FieldName()` helper methods for List and Set fields to add single elements.

**When ENABLED**:
```java
// Generated methods
public PersonDtoBuilder add2Nicknames(String element) {
    List<String> newCollection;
    if (this.nicknames.isSet()) {
        newCollection = new ArrayList<>(this.nicknames.value());
    } else {
        newCollection = new ArrayList<>();
    }
    newCollection.add(element);
    this.nicknames = changedValue(newCollection);
    return this;
}

// Usage
PersonDto person = PersonDtoBuilder.create()
    .name("John")
    .add2Nicknames("Johnny")
    .add2Nicknames("JD")
    .add2Tags("developer")
    .build();
```

**When DISABLED**: No add2 helper methods are generated; must use collection setters or consumer methods.

---

#### `generateUnboxedOptional`

**Default**: `ENABLED` | **Compiler Option**: `-Asimplebuilder.generateUnboxedOptional=ENABLED|DISABLED`

Generates methods that accept `Optional<T>` and automatically unwrap them.

**When ENABLED**:
```java
// Generated method
public PersonDtoBuilder name(Optional<String> nameOptional) {
    nameOptional.ifPresent(value -> this.name = changedValue(value));
    return this;
}

// Usage
Optional<String> maybeName = findName();
PersonDto person = PersonDtoBuilder.create()
    .name(maybeName)  // Automatically unwrapped
    .build();
```

**When DISABLED**: Must unwrap Optional manually before passing to builder.

---

#### `copyTypeAnnotations`

**Default**: `ENABLED` | **Compiler Option**: `-Asimplebuilder.copyTypeAnnotations=ENABLED|DISABLED`

Copies type annotations (TYPE_USE) from the DTO fields to the builder fields and methods. This is useful for validation annotations (e.g. `@NotNull`, `@Size`) or other metadata that should be preserved.

**When ENABLED**:
```java
// DTO
private List<@NotNull String> items;

// Generated Builder
private TrackedValue<List<@NotNull String>> items;
public Builder items(List<@NotNull String> items) { ... }
```

**When DISABLED**: Type annotations are stripped from the builder.

---

### Collection Helpers

#### `usingArrayListBuilder`

**Default**: `ENABLED` | **Compiler Option**: `-Asimplebuilder.usingArrayListBuilder=ENABLED|DISABLED`

Generates methods using `ArrayListBuilder` for fluent List construction.

**When ENABLED**:
```java
// Generated method
public PersonDtoBuilder tags(Consumer<ArrayListBuilder<String>> consumer) {
    ArrayListBuilder<String> builder = new ArrayListBuilder<>();
    consumer.accept(builder);
    this.tags = changedValue(builder.build());
    return this;
}

// Usage
.tags(list -> list.add("tag1").add("tag2").addAll(otherTags))
```

**When DISABLED**: Basic List setter only; no fluent list building.

---

#### `usingArrayListBuilderWithElementBuilders`

**Default**: `ENABLED` | **Compiler Option**: `-Asimplebuilder.usingArrayListBuilderWithElementBuilders=ENABLED|DISABLED`

Generates methods using `ArrayListBuilderWithElementBuilders` for fluent construction of Lists containing complex objects that have their own builders.

**When ENABLED**:
```java
// For List<PersonDto> where PersonDto has a builder
public TeamDtoBuilder members(Consumer<ArrayListBuilderWithElementBuilders<PersonDto, PersonDtoBuilder>> consumer) {
    ArrayListBuilderWithElementBuilders<PersonDto, PersonDtoBuilder> builder = ...;
    consumer.accept(builder);
    this.members = changedValue(builder.build());
    return this;
}

// Usage - build complex nested objects inline
.members(list -> list
    .add(person -> person.name("Alice").age(30))
    .add(person -> person.name("Bob").age(25)))
```

**When DISABLED**: No builder consumer methods for complex list elements; must construct objects separately.

---

#### `usingHashSetBuilder`

**Default**: `ENABLED` | **Compiler Option**: `-Asimplebuilder.usingHashSetBuilder=ENABLED|DISABLED`

Generates methods using `HashSetBuilder` for fluent Set construction.

**When ENABLED**:
```java
// Generated method
public PersonDtoBuilder tags(Consumer<HashSetBuilder<String>> consumer) {
    HashSetBuilder<String> builder = new HashSetBuilder<>();
    consumer.accept(builder);
    this.tags = changedValue(builder.build());
    return this;
}

// Usage
.tags(set -> set.add("tag1").add("tag2").addAll(otherTags))
```

**When DISABLED**: Basic Set setter only; no fluent set building.

---

#### `usingHashSetBuilderWithElementBuilders`

**Default**: `ENABLED` | **Compiler Option**: `-Asimplebuilder.usingHashSetBuilderWithElementBuilders=ENABLED|DISABLED`

Generates methods using `HashSetBuilderWithElementBuilders` for fluent construction of Sets containing complex objects that have their own builders.

**When ENABLED**:
```java
// For Set<PersonDto> where PersonDto has a builder
public TeamDtoBuilder uniqueMembers(Consumer<HashSetBuilderWithElementBuilders<PersonDto, PersonDtoBuilder>> consumer) {
    HashSetBuilderWithElementBuilders<PersonDto, PersonDtoBuilder> builder = ...;
    consumer.accept(builder);
    this.uniqueMembers = changedValue(builder.build());
    return this;
}

// Usage - build complex nested objects inline
.uniqueMembers(set -> set
    .add(person -> person.name("Alice").email("alice@example.com"))
    .add(person -> person.name("Bob").email("bob@example.com")))
```

**When DISABLED**: No builder consumer methods for complex set elements; must construct objects separately.

---

#### `usingHashMapBuilder`

**Default**: `ENABLED` | **Compiler Option**: `-Asimplebuilder.usingHashMapBuilder=ENABLED|DISABLED`

Generates methods using `HashMapBuilder` for fluent Map construction.

---

### Component Filtering

#### `deactivateGenerationComponents`

**Default**: `""` (empty) | **Compiler Option**: `-Asimplebuilder.deactivateGenerationComponents=pattern1,pattern2,...`

Deactivates specific method generators and builder enhancers by class name pattern. This allows you to override default generators/enhancers with your own custom implementations.

**Primary Use Case**: Override Default Components

When you want to replace a built-in generator or enhancer with your own custom implementation, you first deactivate the default component, then register your custom one via ServiceLoader.

For detailed instructions on creating custom generators and enhancers, see [**CUSTOMIZING.md**](CUSTOMIZING.md).

**Pattern Matching**:
- **Exact match**: `ConditionalEnhancer` - deactivates exactly this class
- **Wildcard suffix**: `*HelperGenerator` - deactivates all classes ending with HelperGenerator
- **Wildcard prefix**: `String*` - deactivates all classes starting with String
- **Wildcard anywhere**: `*Consumer*` - deactivates all classes containing Consumer

**Feature Toggling vs Component Override**:
- **Feature toggling**: Use `@SimpleBuilder.Options(generateConditionalHelper = DISABLED)`
- **Component override**: Use `deactivateGenerationComponents` + custom ServiceLoader implementation

See [**CUSTOMIZING.md**](CUSTOMIZING.md) for complete implementation examples and best practices.

---

### Integration & Annotations

#### `generateWithInterface`

**Default**: `ENABLED` | **Compiler Option**: `-Asimplebuilder.generateWithInterface=ENABLED|DISABLED`

Generates a `With` interface that can be implemented by your DTO to enable fluent modification methods.

**When ENABLED**:
```java
// Generated interface inside the builder
public interface With {
    default PersonDto with(Consumer<PersonDtoBuilder> modifications) {
        PersonDtoBuilder builder = new PersonDtoBuilder((PersonDto) this);
        modifications.accept(builder);
        return builder.build();
    }
    
    default PersonDtoBuilder with() {
        return new PersonDtoBuilder((PersonDto) this);
    }
}

// Your DTO can implement it
public class PersonDto implements PersonDtoBuilder.With {
    // ...
}

// Usage - create modified copies
PersonDto original = new PersonDto();
PersonDto modified = original.with(p -> p.name("New Name").age(30));
```

**When DISABLED**: No `With` interface is generated. DTOs cannot use the fluent modification pattern.

---

#### `implementsBuilderBase`

**Default**: `ENABLED` | **Compiler Option**: `-Asimplebuilder.implementsBuilderBase=ENABLED|DISABLED`

Makes the generated builder implement `IBuilderBase<T>` interface for framework integration.

**When ENABLED**:
```java
public class PersonDtoBuilder implements IBuilderBase<PersonDto> {
    // Can be used with generic builder frameworks
}
```

**When DISABLED**: Builder is a standalone class without interface.

---

#### `usingGeneratedAnnotation`

**Default**: `ENABLED` | **Compiler Option**: `-Asimplebuilder.usingGeneratedAnnotation=ENABLED|DISABLED`

Adds `@Generated` annotation to the builder class for tooling and code coverage exclusion.

**When ENABLED**:
```java
@Generated("Generated by org.javahelpers.simple.builders.processor.BuilderProcessor")
public class PersonDtoBuilder implements IBuilderBase<PersonDto> {
    // ...
}
```

**When DISABLED**: No `@Generated` annotation. Useful if you want the builder counted in code coverage.

---

#### `usingBuilderImplementationAnnotation`

**Default**: `ENABLED` | **Compiler Option**: `-Asimplebuilder.usingBuilderImplementationAnnotation=ENABLED|DISABLED`

Adds `@BuilderImplementation` annotation linking the builder to its target class.

**When ENABLED**:
```java
@BuilderImplementation(forClass = PersonDto.class)
public class PersonDtoBuilder implements IBuilderBase<PersonDto> {
    // ...
}
```

**When DISABLED**: No `@BuilderImplementation` annotation.

---

#### `usingJacksonDeserializerAnnotation`

**Default**: `DISABLED` | **Compiler Option**: `-Asimplebuilder.usingJacksonDeserializerAnnotation=ENABLED|DISABLED`

Adds `@JsonPOJOBuilder` annotation to the builder class for Jackson deserialization support.

**When ENABLED**:
```java
@JsonPOJOBuilder(withPrefix = "")
public class PersonDtoBuilder {
    // ...
}
```

**When DISABLED**: No `@JsonPOJOBuilder` annotation.

**Note**: This requires `com.fasterxml.jackson.core:jackson-databind` on the classpath during compilation. If missing, the annotation is skipped with a warning.

---

#### `generateJacksonModule`

**Default**: `DISABLED` | **Compiler Option**: `-Asimplebuilder.generateJacksonModule=ENABLED|DISABLED`

Generates a Jackson `SimpleModule` (named `SimpleBuildersJacksonModule`) that registers all generated builders via MixIns. This allows deserialization without annotating your DTOs with `@JsonDeserialize`.

**Requirement**: You MUST also enable [`usingJacksonDeserializerAnnotation`](#usingjacksondeserializerannotation). If `generateJacksonModule` is enabled but `usingJacksonDeserializerAnnotation` is disabled, the processor will issue a warning and skip module generation.

**When ENABLED**:
1. The builder is generated as usual.
2. A `SimpleBuildersJacksonModule` class is generated.
3. The module registers a MixIn for the DTO that points to the Builder.

**Package Name**:
By default, a `SimpleBuildersJacksonModule` is generated in **each package** that contains DTOs configured for Jackson module generation. This ensures deterministic behavior.
To specify a single fixed package name for all generated modules (grouping them into one), use the [`jacksonModulePackage`](#jacksonmodulepackage) option.

**Generated Module Example**:
```java
package com.example.project.dto; // Generated in the same package as DTOs (by default)

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class SimpleBuildersJacksonModule extends SimpleModule {
    public SimpleBuildersJacksonModule() {
        setMixInAnnotation(PersonDto.class, PersonDtoMixin.class);
    }
    
    @JsonDeserialize(builder = PersonDtoBuilder.class)
    private interface PersonDtoMixin {}
}
```

**Usage**:
```java
ObjectMapper mapper = new ObjectMapper();

// Register the module for your package
// Note: If you have DTOs in multiple packages and use the default strategy,
// you need to register the generated module for each package.
mapper.registerModule(new com.example.project.dto.SimpleBuildersJacksonModule());

PersonDto dto = mapper.readValue(json, PersonDto.class);
```

**Tip**: Use the [`jacksonModulePackage`](#jacksonmodulepackage) option to generate a single module for your entire project, making registration easier:
`mapper.registerModule(new com.example.project.config.SimpleBuildersJacksonModule());`

**Note**: This requires `com.fasterxml.jackson.core:jackson-databind` on the classpath.

---

#### `jacksonModulePackage`

**Default**: `null` (uses package of each processed DTO) | **Compiler Option**: `-Asimplebuilder.jacksonModulePackage=com.your.package`

Specifies the package name where the `SimpleBuildersJacksonModule` class will be generated.
This is highly recommended to ensure deterministic output location and avoid split-package issues.

**Note**: If not specified, a separate `SimpleBuildersJacksonModule` will be generated in **each package** containing processed DTOs.

**Example**:
`-Asimplebuilder.jacksonModulePackage=com.example.project.config`

---

### Documentation

#### `generateJavaDoc`

**Default**: `ENABLED` | **Compiler Option**: `-Asimplebuilder.generateJavaDoc=ENABLED|DISABLED`

Controls whether the processor emits Javadoc comments on the generated builder class, its fields, constructors and methods.

**When ENABLED** (default):
The generated builder contains Javadoc blocks explaining the purpose of the class, setters, `build()`, `create()` and any helper methods.

**When DISABLED**:
No Javadoc is emitted. This produces smaller generated source files and is useful when generated code is committed to version control and Javadoc noise is undesirable.

**Example**:
```java
@SimpleBuilder(options = @SimpleBuilder.Options(generateJavaDoc = OptionState.DISABLED))
public class PersonDto {
    private String name;
}
```

---

### Naming

#### `builderSuffix`

**Default**: `"Builder"` | **Compiler Option**: `-Asimplebuilder.builderSuffix=CustomSuffix`

Customizes the suffix appended to the DTO class name to create the builder class name.

**Example**:
```java
@SimpleBuilder.Options(builderSuffix = "Factory")
public class PersonDto { }

// Generated class name: PersonDtoFactory (instead of PersonDtoBuilder)
```

---

#### `setterSuffix`

**Default**: `""` (empty) | **Compiler Option**: `-Asimplebuilder.setterSuffix=customPrefix`

Adds a prefix to all setter method names.

**Example**:
```java
@SimpleBuilder.Options(setterSuffix = "with")
public class PersonDto {
    private String name;
}

// Generated method: withName(String name) instead of name(String name)
```

### Reliability

#### `strict`

**Default**: `DISABLED` | **Compiler Option**: `-Asimplebuilder.strict=ENABLED|DISABLED`

When enabled, builder and Jackson-module generation failures are promoted from compiler warnings
to errors that fail the build. By default, strict mode is disabled and generation failures are
reported as warnings so compilation can continue.

---

### Performance Tracking

#### `performanceTracking`

**Default**: `false` | **Compiler Option**: `-Asimplebuilder.performanceTracking=true|false`

Enables performance tracking during annotation processing. When enabled, the processor measures
execution times for each processing phase, method generator, builder enhancer, and per-class
processing. A summary report is logged to the compiler output at the end of processing.

**When enabled**: A hierarchical performance report is printed to the compiler log, including:
- Total processing time and average time per class
- Phase breakdown (Configuration Resolution, Builder Definition Extraction, DTO Mapping, Code Generation)
- Top 20 slowest classes with field and collection counts
- Top 5 slowest MethodGenerators and BuilderEnhancers

**When disabled** (default): No performance tracking occurs. The `NoOpPerformanceTracker` is used,
which has zero overhead as the JIT compiler eliminates all tracking calls.

**Example**:
```bash
# Maven
mvn compile -Dsimplebuilder.performanceTracking=true

# Or via compiler arg
-Asimplebuilder.performanceTracking=true
```

---

#### `performanceOutputFile`

**Default**: *(empty, no file output)* | **Compiler Option**: `-Asimplebuilder.performanceOutputFile=path/to/report.json`

Specifies a file path where the performance report is written as structured JSON. This is useful
for automated performance analysis and comparison across multiple runs.

**When set**: In addition to the log output, a JSON file is written containing:
- `timestamp` — ISO-8601 timestamp of the report
- `totalClasses` — number of classes processed
- `totalProcessingTimeNanos` / `totalProcessingTimeSeconds` — total processing time
- `averagePerClassMs` — average processing time per class
- `phaseBreakdown` — hierarchical phase timings with elapsed nanos, seconds, and percentages
- `classMetrics` — per-class metrics (name, elapsed nanos/ms, field count, collection count), sorted by elapsed time descending
- `generatorStats` — per-generator stats (name, elapsed nanos, call count, avg ms/call)
- `enhancerStats` — per-enhancer stats (name, elapsed nanos, call count, avg ms/call)

**When not set** (default): Only the log report is generated; no JSON file is written.

**Example**:
```bash
# Maven
mvn compile \
  -Dsimplebuilder.performanceTracking=true \
  -Dsimplebuilder.performanceOutputFile=target/performance-report.json

# Or via compiler arg
-Asimplebuilder.performanceTracking=true
-Asimplebuilder.performanceOutputFile=target/performance-report.json
```

**Note**: `performanceTracking` must be enabled for `performanceOutputFile` to have any effect.

## Examples

### Minimal Builder

Use the built-in `@SimpleMinimalBuilder` template to generate only essential builder methods with a single annotation:

```java
import org.javahelpers.simple.builders.core.annotations.SimpleMinimalBuilder;

@SimpleMinimalBuilder
public class MinimalDto {
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
```

**Generated**: Only basic builder methods (`create()`, field setters, `build()`).

For a concrete generated example, see [`CustomerDtoBuilder.java`](../example/generated-example-builder/org/javahelpers/simple/builders/example/CustomerDtoBuilder.java) produced from [`CustomerDto.java`](../example/src/main/java/org/javahelpers/simple/builders/example/CustomerDto.java).

If you need a different name or extra customization, you can still build a custom `@SimpleBuilder.Template` with all optional features disabled.

### Internal API Builder

Create builders for internal use only:

```java
@SimpleBuilder
@SimpleBuilder.Options(
    builderAccess = AccessModifier.PACKAGE_PRIVATE,
    methodAccess = AccessModifier.PACKAGE_PRIVATE
)
public class InternalConfig {
    private String secretKey;
    
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
}
```

**Generated**: Package-private builder and methods, only accessible within the same package

### Collection-Heavy Builder

Optimize for collection manipulation:

```java
@SimpleBuilder
@SimpleBuilder.Options(
    generateVarArgsHelpers = OptionState.ENABLED,
    usingArrayListBuilder = OptionState.ENABLED,
    usingArrayListBuilderWithElementBuilders = OptionState.ENABLED,
    usingHashSetBuilder = OptionState.ENABLED,
    usingHashMapBuilder = OptionState.ENABLED
)
public class TeamDto {
    private List<String> memberNames;
    private Set<PersonDto> members;
    private Map<String, PersonDto> memberMap;
}
```

**Generated**: Chained collection builders for fluent collection manipulation

Example usage:
```java
TeamDto team = TeamDtoBuilder.create()
    .memberNames(list -> list.add("Alice").add("Bob"))
    .members(set -> set
        .add(person -> person.name("Alice").age(30))
        .add(person -> person.name("Bob").age(25)))
    .memberMap(map -> map.put("Alice", alice).put("Bob", bob))
    .build();
```

### Minimal Builder Template

The built-in `@SimpleMinimalBuilder` is the simplest way to get a lightweight builder. If you need a differently named template or want to build your own preset, create a custom annotation meta-annotated with `@SimpleBuilder.Template`:

```java
@SimpleBuilder.Template(options = @SimpleBuilder.Options(
    generateFieldSupplier = OptionState.DISABLED,
    generateFieldConsumer = OptionState.DISABLED,
    generateBuilderConsumer = OptionState.DISABLED,
    generateConditionalHelper = OptionState.DISABLED,
    generateVarArgsHelpers = OptionState.DISABLED,
    generateStringFormatHelpers = OptionState.DISABLED,
    generateAddToCollectionHelpers = OptionState.DISABLED,
    generateUnboxedOptional = OptionState.DISABLED,
    copyTypeAnnotations = OptionState.DISABLED,
    usingArrayListBuilder = OptionState.DISABLED,
    usingArrayListBuilderWithElementBuilders = OptionState.DISABLED,
    usingHashSetBuilder = OptionState.DISABLED,
    usingHashSetBuilderWithElementBuilders = OptionState.DISABLED,
    usingHashMapBuilder = OptionState.DISABLED,
    generateWithInterface = OptionState.DISABLED,
    usingGeneratedAnnotation = OptionState.DISABLED,
    usingBuilderImplementationAnnotation = OptionState.DISABLED,
    implementsBuilderBase = OptionState.DISABLED,
    usingJacksonDeserializerAnnotation = OptionState.DISABLED,
    generateJacksonModule = OptionState.DISABLED,
    generateJavaDoc = OptionState.DISABLED
))
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface MinimalBuilder {}
```

Use everywhere:
```java
@MinimalBuilder
public class CustomerDto {
    private String name;
    private List<OrderDto> orders;
}
```

### Project-Wide Defaults

Set sensible defaults for your entire project:

```xml
<!-- Maven compiler plugin configuration -->
<compilerArgs>
    <!-- Enable all field setter variants -->
    <arg>-Asimplebuilder.generateFieldSupplier=ENABLED</arg>
    <arg>-Asimplebuilder.generateFieldConsumer=ENABLED</arg>
    <arg>-Asimplebuilder.generateBuilderConsumer=ENABLED</arg>
    
    <!-- Make builders package-private for internal APIs -->
    <arg>-Asimplebuilder.builderAccess=PACKAGE_PRIVATE</arg>
    
    <!-- Enable collection helpers -->
    <arg>-Asimplebuilder.usingArrayListBuilder=ENABLED</arg>
    <arg>-Asimplebuilder.usingHashMapBuilder=ENABLED</arg>
</compilerArgs>
```

Override per-class when needed:

```java
@SimpleBuilder
@SimpleBuilder.Options(builderAccess = AccessModifier.PUBLIC)  // Override: make this one public
public class PublicApiDto {
    private String data;
}

@SimpleBuilder  // Uses project defaults: package-private
public class InternalDto {
    private String data;
}
```

## Priority Rules

Configuration resolution follows these priority rules:

1. **Annotation values** (highest priority)
   - Values in `@SimpleBuilder.Options(...)` always win
2. **Compiler options** (medium priority)
   - Used when no annotation value is specified
3. **Default values** (lowest priority)
   - Used when neither annotation nor compiler option is specified

### Example: Priority in Action

```java
// Compiler option: -Asimple.builders.generateFieldSupplier=false
// Global default: true

@SimpleBuilder
@SimpleBuilder.Options(generateFieldSupplier = OptionState.ENABLED)  // Annotation wins!
public class Person {
    private String name;
}

@SimpleBuilder  // Uses compiler option (false)
public class Company {
    private String name;
}

// No compiler option set
@SimpleBuilder  // Uses global default (true)
public class Product {
    private String name;
}
```

## AccessModifier Enum

The `AccessModifier` enum provides type-safe access control:

```java
public enum AccessModifier {
    PUBLIC,           // Accessible from anywhere
    PROTECTED,        // Accessible within same package and subclasses
    PACKAGE_PRIVATE,  // Accessible only within same package (default Java visibility)
    PRIVATE           // Accessible only within same class
}
```

Use in annotations:
```java
@SimpleBuilder.Options(
    builderAccess = AccessModifier.PACKAGE_PRIVATE,
    methodAccess = AccessModifier.PUBLIC
)
```

Or in compiler options:
```
-Asimplebuilder.builderAccess=PACKAGE_PRIVATE
```

## Troubleshooting

### Compiler Options Not Working

1. **Check option names**: Ensure you're using the full option name (e.g., `-Asimplebuilder.generateFieldSupplier`)
2. **Verify processor is running**: Ensure annotation processor is configured correctly
3. **Check IDE configuration**: Some IDEs need special configuration for compiler options
4. **Clean and rebuild**: Run `mvn clean compile` to ensure fresh build

### Annotation Values Not Applied

1. **Verify annotation import**: Import `org.javahelpers.simple.builders.core.annotations.SimpleBuilder`
2. **Check annotation placement**: Use `@SimpleBuilder` on the class, `@SimpleBuilder.Options` on the same class
3. **Verify compilation**: Recompile after changing annotations
4. **Check for syntax errors**: Ensure AccessModifier enum values are correct

### Access Level Issues

1. **Package-private builders**: Ensure DTO and builder are in the same package
2. **Private builders**: May cause issues with reflection-based frameworks
3. **Protected builders**: Only accessible to subclasses
4. **AccessModifier import**: Import `org.javahelpers.simple.builders.core.enums.AccessModifier`

### Template Annotations Not Working

1. **Check @SimpleBuilder.Template**: Ensure template annotation has `@SimpleBuilder.Template`
2. **Verify options parameter**: Template can specify `options = @SimpleBuilder.Options(...)` or rely on the built-in defaults (omitting it is equivalent to an empty `@Options()`)
3. **Retention and Target**: Add `@Retention(RetentionPolicy.CLASS)` and `@Target(ElementType.TYPE)`
4. **Combining with @SimpleBuilder**: `@SimpleBuilder` is itself a built-in template. If both `@SimpleBuilder` and a custom template are present on the same class, `@SimpleBuilder` takes precedence in that scope. To use a custom template, place only the custom annotation on the class.
5. **Subclasses not getting a builder**: Add `@Inherited` to the custom template annotation so it propagates to unannotated subclasses (see [Template Annotations](#template-annotations) above). Without `@Inherited`, only the exact type carrying the annotation gets a builder.
6. **Subclass builder has wrong options**: Ensure the custom template annotation is `@Inherited` and that the parent annotation declares the desired `@SimpleBuilder.Options`. Inherited options are applied to subclass builders; a subclass's own annotation overrides the inherited options.

### Builder Not Generated - Access Modifier Errors

If you see warnings like "Failed to generate builder" with access modifier messages:

**Problem**: Used `PRIVATE` for `builderAccess` or `methodAccess`
```java
@SimpleBuilder.Options(builderAccess = AccessModifier.PRIVATE)  // ❌ ERROR
```

**Solution**: Use `PUBLIC` or `PACKAGE_PRIVATE` instead
```java
@SimpleBuilder.Options(builderAccess = AccessModifier.PACKAGE_PRIVATE)  // ✅ OK
```

**Note**: Only `builderConstructorAccess = PRIVATE` is valid - this enforces using the `create()` factory method.

**Error Messages**:
- `builderAccess=PRIVATE` → "Java does not allow private top-level classes"
- `methodAccess=PRIVATE` → "Makes all setter methods inaccessible"

## Best Practices

1. **Use templates for common patterns**: Define reusable templates for your project
2. **Set project-wide defaults**: Configure sensible defaults via compiler options
3. **Override sparingly**: Only override when truly necessary
4. **Document templates**: Add JavaDoc to custom template annotations
5. **Use type-safe enums**: Prefer `AccessModifier` enum over string values
6. **Test configurations**: Verify generated code meets expectations
7. **Consider team preferences**: Choose configurations that work for everyone

### Access Modifier Best Practices

**Recommended Combinations**:

✅ **Public API Builder** (most common):
```java
@SimpleBuilder.Options(
    builderAccess = AccessModifier.PUBLIC,            // ✅ Accessible everywhere
    builderConstructorAccess = AccessModifier.PRIVATE, // ✅ Forces use of create()
    methodAccess = AccessModifier.PUBLIC               // ✅ Accessible everywhere
)
```

✅ **Internal/Package-Private Builder**:
```java
@SimpleBuilder.Options(
    builderAccess = AccessModifier.PACKAGE_PRIVATE,    // ✅ Internal to package
    builderConstructorAccess = AccessModifier.PRIVATE, // ✅ Forces use of create()
    methodAccess = AccessModifier.PACKAGE_PRIVATE      // ✅ Internal to package
)
```

❌ **Invalid Combinations (Will Cause Builder Generation to Fail)**:

```java
// ❌ ERROR: Private builder class causes generation failure
builderAccess = AccessModifier.PRIVATE  
// Error: "Java does not allow private top-level classes"
// Result: Builder NOT generated, other DTOs continue processing

// ❌ ERROR: Private methods cause generation failure
methodAccess = AccessModifier.PRIVATE   
// Error: "Makes all setter methods inaccessible"
// Result: Builder NOT generated, other DTOs continue processing

// ❌ ERROR: Both invalid configurations
builderAccess = AccessModifier.PRIVATE,
methodAccess = AccessModifier.PRIVATE
// Result: Builder NOT generated, other DTOs continue processing
```

**Why `PRIVATE` constructors are different**:
- ✅ `builderConstructorAccess = PRIVATE` **IS ALLOWED** - Enforces using `create()` factory method (recommended pattern)
- ❌ `builderAccess = PRIVATE` **CAUSES ERROR** - Java doesn't allow private top-level classes
- ❌ `methodAccess = PRIVATE` **CAUSES ERROR** - Makes all methods inaccessible and builder unusable

**What happens when validation fails**:
1. Builder generation for that DTO is skipped
2. A clear warning message is logged explaining the problem
3. Compilation continues and succeeds
4. Other DTOs in your project still get their builders generated
5. No invalid Java code is produced

## Reference

### All Compiler Options

```
# Field Setter Generation
-Asimplebuilder.generateFieldSupplier=ENABLED|DISABLED
-Asimplebuilder.generateFieldConsumer=ENABLED|DISABLED
-Asimplebuilder.generateBuilderConsumer=ENABLED|DISABLED

# Conditional Logic
-Asimplebuilder.generateConditionalHelper=ENABLED|DISABLED

# Access Control
-Asimplebuilder.builderAccess=PUBLIC|PACKAGE_PRIVATE  # PRIVATE not recommended (unusable builder)
-Asimplebuilder.builderConstructorAccess=PUBLIC|PACKAGE_PRIVATE|PRIVATE  # PRIVATE recommended for factory pattern
-Asimplebuilder.methodAccess=PUBLIC|PACKAGE_PRIVATE  # PRIVATE not recommended (unusable methods)

# Helper Methods
-Asimplebuilder.generateVarArgsHelpers=ENABLED|DISABLED
-Asimplebuilder.generateStringFormatHelpers=ENABLED|DISABLED
-Asimplebuilder.generateAddToCollectionHelpers=ENABLED|DISABLED
-Asimplebuilder.generateUnboxedOptional=ENABLED|DISABLED

# Collection Helpers
-Asimplebuilder.usingArrayListBuilder=ENABLED|DISABLED
-Asimplebuilder.usingArrayListBuilderWithElementBuilders=ENABLED|DISABLED
-Asimplebuilder.usingHashSetBuilder=ENABLED|DISABLED
-Asimplebuilder.usingHashSetBuilderWithElementBuilders=ENABLED|DISABLED
-Asimplebuilder.usingHashMapBuilder=ENABLED|DISABLED

# Component Filtering
-Asimplebuilder.deactivateGenerationComponents=pattern1,pattern2,...

# Integration & Annotations
-Asimplebuilder.generateWithInterface=ENABLED|DISABLED
-Asimplebuilder.implementsBuilderBase=ENABLED|DISABLED
-Asimplebuilder.usingGeneratedAnnotation=ENABLED|DISABLED
-Asimplebuilder.usingBuilderImplementationAnnotation=ENABLED|DISABLED

# Documentation
-Asimplebuilder.generateJavaDoc=ENABLED|DISABLED

# Naming
-Asimplebuilder.builderSuffix=CustomSuffix
-Asimplebuilder.setterSuffix=customPrefix

# Reliability
-Asimplebuilder.strict=ENABLED|DISABLED

# Performance Tracking
-Asimplebuilder.performanceTracking=true|false
-Asimplebuilder.performanceOutputFile=path/to/report.json
```

### Complete Options Example

```java
@SimpleBuilder
@SimpleBuilder.Options(
    // Field Setter Generation
    generateFieldSupplier = OptionState.ENABLED,
    generateFieldConsumer = OptionState.ENABLED,
    generateBuilderConsumer = OptionState.ENABLED,
    
    // Conditional Logic
    generateConditionalHelper = OptionState.ENABLED,
    
    // Access Control
    builderAccess = AccessModifier.PUBLIC,
    builderConstructorAccess = AccessModifier.PUBLIC,
    methodAccess = AccessModifier.PUBLIC,
    
    // Helper Methods
    generateVarArgsHelpers = OptionState.ENABLED,
    generateStringFormatHelpers = OptionState.ENABLED,
    generateUnboxedOptional = OptionState.ENABLED,
    
    // Collection Helpers
    usingArrayListBuilder = OptionState.ENABLED,
    usingArrayListBuilderWithElementBuilders = OptionState.ENABLED,
    usingHashSetBuilder = OptionState.ENABLED,
    usingHashSetBuilderWithElementBuilders = OptionState.ENABLED,
    usingHashMapBuilder = OptionState.ENABLED,
    
    // Integration & Annotations
    generateWithInterface = OptionState.ENABLED,
    implementsBuilderBase = OptionState.ENABLED,
    usingGeneratedAnnotation = OptionState.ENABLED,
    usingBuilderImplementationAnnotation = OptionState.ENABLED,
    usingJacksonDeserializerAnnotation = OptionState.ENABLED,

    // Documentation
    generateJavaDoc = OptionState.ENABLED,

    // Naming
    builderSuffix = "Builder",
    setterSuffix = ""
)
public class ExampleDto {
    private String name;
}
```

---

**Related**: [README.md](../README.md)
