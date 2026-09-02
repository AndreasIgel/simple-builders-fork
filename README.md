# Simple Builders — Type-safe, fluent builders for Java classes & records, generated at compile time

A zero-reflection Java annotation processor that generates fluent, type-safe builders for **classes and records** — with **Jackson** support, immutable **copy-on-write `with`**, conditional logic, and collection helpers. A lightweight **Lombok alternative**.

[![License](https://img.shields.io/badge/License-MIT%202.0-yellowgreen.svg)](https://github.com/java-helpers/simple-builders/blob/main/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.java-helpers/simple-builders-core.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:io.github.java-helpers%20AND%20a:simple-builders-core)
[![Java Version](https://img.shields.io/badge/Java-17%2B-blue)](https://www.oracle.com/java/technologies/javase/17-relnote-issues.html)
[![Maven](https://img.shields.io/badge/Maven-3.8%2B-orange)](https://maven.apache.org/)
[![codecov](https://codecov.io/gh/java-helpers/simple-builders/graph/badge.svg)](https://codecov.io/gh/java-helpers/simple-builders)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=java-helpers_simple-builders&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=java-helpers_simple-builders)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=java-helpers_simple-builders&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=java-helpers_simple-builders)

## Table of Contents
- [What is Simple Builders?](#what-is-simple-builders)
- [How Simple Builders compares](#how-simple-builders-compares)
  - [Doing what other builders advertise — the Simple Builders way](#doing-what-other-builders-advertise-the-simple-builders-way)
- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Usage](#usage)
  - [Basic Usage](#basic-usage)
    - [Validation Annotations](#validation-annotations)
    - [Required Fields and Null-Safety](#required-fields-and-null-safety)
    - [Conditional Builder Logic](#conditional-builder-logic)
  - [Collections and Nested Objects](#collections-and-nested-objects)
    - [Collection Immutability](#collection-immutability)
  - [With Interface Pattern](#with-interface-pattern)
  - [Builder Configuration](#builder-configuration)
    - [Compiler Arguments](#compiler-arguments)
- [Examples](#examples)
  - [Elementary Builder Example](#elementary-builder-example)
  - [Full-Featured Examples](#full-featured-examples)
  - [Advanced Features](#advanced-features)
  - [Builder Scoping Example](#builder-scoping-example)
- [Performance Measurement](#performance-measurement)
- [Contributing](#contributing)
- [License](#license)
- [Acknowledgements](#acknowledgements)
- [Links](#links)

## What is Simple Builders?

Simple Builders is a Java [annotation processor](https://docs.oracle.com/en/java/javase/17/docs/specs/man/javac.html#annotation-processing) that generates type-safe, fluent builders for existing Java classes and records at compile time. It supports Jackson integration, immutable copy-with updates, conditional logic, and collection helpers, with no runtime reflection.

## How Simple Builders compares

Simple Builders generates fluent, type-safe builders for your **existing** classes and records using standard [JSR-269 annotation processing](https://docs.oracle.com/en/java/javase/17/docs/specs/man/javac.html#annotation-processing) — it adds separate, readable generated source and never modifies your types. The main alternatives solve overlapping but different problems, and each is the better choice in its own niche:

- **Lombok** — the closest "add a builder to my existing class" tool, but it works very differently: rather than generating separate source, it mutates your class at compile time through the compiler's internal, non-public AST APIs. That steps outside the standard model (a normal annotation processor may only add new files, not alter existing ones), so it needs an IDE plugin, hides the generated code behind `delombok`, and — because it depends on internal compiler APIs — generally needs a Lombok update for each new JDK before your project compiles (e.g. JDK 16's strong encapsulation, [JEP 396](https://openjdk.org/jeps/396), broke it until [v1.18.20](https://projectlombok.org/changelog), a pattern that recurs for JDK 17, 21, and 23). Lombok is also a broad toolkit (`@Data`, `@Value`, `@SneakyThrows`, `@Delegate`, `val`, and more) whose implicit behavior can be misused by less-experienced developers — e.g. `@Data`/`@EqualsAndHashCode` on JPA entities (broken equality, lazy-loading pitfalls) or `@SneakyThrows` bypassing checked exceptions. Choose Lombok if you're already invested in it for broad boilerplate reduction; choose Simple Builders for one focused capability with explicit, readable source.
- **Immutables / Google AutoValue / FreeBuilder** — value-type generators: you declare an abstract class or interface and they generate an immutable implementation plus a builder. Great when you want to define new immutable value types; less suited when you just want a builder for classes or records you already have and don't want to restructure your model.
- **RecordBuilder** — focused, excellent builders and `with` methods for records. Choose it if you use records exclusively.

Use Simple Builders when you want fluent, type-safe builders for the classes and records you already have, generated as plain readable source, with no bytecode manipulation and no IDE plugin — and no lock-in: because the builders are ordinary generated Java, you can drop the dependency at any time by copying the generated builder classes into your own sources, and they keep working.

### Doing what other builders advertise — the Simple Builders way

- **Required fields:** Primitive fields and fields annotated with an annotation named `NotNull` or `NonNull` are non-nullable; constructor parameters are builder inputs. `build()` enforces the required/non-null contract with `IllegalStateException` ([configuration details](#required-fields-and-null-safety)).
- **Copy / `with` / `toBuilder`:** The generated `With` interface provides `instance.with(b -> ...)` for copy-and-modify and `instance.with()` for a builder pre-populated from the instance ([`generateWithInterface`](docs/CONFIGURATION.md#generatewithinterface)).
- **Collection immutability:** The target type owns the collection contract. Simple Builders passes through what the type stores; use defensive copying such as `List.copyOf(...)` in the type when the result must be immutable ([configuration details](#collection-immutability)).
- **Incremental / singular collection API:** `add2X` helpers, `ArrayList`/`HashSet`/`HashMap` collection builders, and varargs helpers cover incremental collection construction ([collection helper options](docs/CONFIGURATION.md#collection-helpers)).
- **Inheritance:** Inherited setters are discovered, and constructors exposed by the annotated subclass are used. A final superclass field not exposed by that constructor is intentionally not bypassed ([configuration options](docs/CONFIGURATION.md#configuration-options)).

Value semantics (`equals`, `hashCode`, `toString`) and generating brand-new immutable value types are deliberate out-of-scope paradigm choices, not missing builder features.

## Features

- **Low Runtime Dependencies**: The generated code has only dependencies to a core dependency for CollectionBuilders and to Apache.CommonLang3
- **Type-Safe Builders**: Compile-time type checking for all builder methods
- **Fluent API**: Clean, chainable API for object construction
- **Collections Support**: Built-in support for collections and maps
- **Annotation Preservation**: Validation annotations are automatically copied to builder methods
- **With Interface Pattern**: Type-safe object modifications using generated With interfaces
- **Jackson Support**: Supporting Jackson deserialization via `@JsonPOJOBuilder` and optional generation of `SimpleModule`s (one per package) (both need to be enabled)
- **JavaDoc Usage Examples**: Generated builder methods include auto-generated usage examples in their JavaDoc (per-method fluent snippets plus a class-level example), so IDE tooltips show exactly how to use each builder

## Requirements

- Java 17 or later
- Maven 3.8+ (for building from source)

## Installation

For Maven-based projects, add the following to your POM file in order to use Simple Builders (the dependencies are available at Maven Central):

```xml
...
<properties>
    <simple-builders.version>0.2.0</simple-builders.version>
</properties>
...
<dependencies>
    <dependency>
        <groupId>io.github.java-helpers</groupId>
        <artifactId>simple-builders-core</artifactId>
        <version>${simple-builders.version}</version>
    </dependency>
</dependencies>
...
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.13.0</version>
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
            </configuration>
        </plugin>
    </plugins>
</build>
...
```

If you don't work with a dependency management tool, you can obtain a distribution bundle from [Releases page](https://github.com/java-helpers/simple-builders/releases).

## Usage

### Basic Usage

Annotate your class with `@SimpleBuilder` to generate a builder:

```java
import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

@SimpleBuilder
public class Person {
    private String name;
    private int age;
    private List<String> emailAddresses;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    
    public List<String> getEmailAddresses() { return emailAddresses; }
    public void setEmailAddresses(List<String> emailAddresses) { 
        this.emailAddresses = emailAddresses; 
    }
}
```

Use the generated builder:

```java
Person person = PersonBuilder.create()
    .name("John Doe")
    .age(30)
    .emailAddresses("john@example.com", "j.doe@example.com")
    .add2EmailAddresses("jane@example.com")
    .build();
```

#### Validation Annotations

Simple Builders preserves validation annotations on your builder methods:

```java
import jakarta.validation.constraints.*;

@SimpleBuilder
public class User {
    private String email;
    private int age;

    public String getEmail() { return email; }
    public void setEmail(@Email @NotNull String email) { 
        this.email = email; 
    }
    
    public int getAge() { return age; }
    public void setAge(@Min(18) int age) { 
        this.age = age; 
    }
}
```

The generated builder preserves these annotations:

```java
User user = UserBuilder.create()
    .email("user@example.com")  // @Email and @NotNull are on the parameter
    .age(25)                     // @Min(18) is on the parameter
    .build();
```

This ensures validation frameworks work seamlessly with builder-generated objects.

#### Required Fields and Null-Safety

Simple Builders treats a field as non-nullable when its type is primitive or its parameter carries
an annotation whose simple name is `NotNull` or `NonNull`, regardless of the annotation package.
The name is matched without requiring a particular validation framework.

For non-nullable constructor fields, `build()` requires the builder value to be set and non-null.
For non-nullable setter fields, the field remains optional, but a value supplied to the builder must
be non-null. Violations throw `IllegalStateException`.

For example, both the primitive `price` and the `@NotNull` `name` below are required — omitting
either makes `build()` fail:

```java
@SimpleBuilder
public record Product(@NotNull String name, double price) {}

ProductBuilder.create()
    .name("Keyboard")
    .build(); // throws IllegalStateException: price must be set

ProductBuilder.create()
    .price(99.0)
    .build(); // throws IllegalStateException: name must be set
```

The `NotNull`/`NonNull` simple-name check is framework-agnostic; use the annotation type already
used by your project.

#### Default Values

Specify default values for fields that are applied when not explicitly set before `build()`:

```java
import org.javahelpers.simple.builders.core.annotations.Default;

@SimpleBuilder
public record Product(
    String name,
    double price,
    @Default("GENERAL") String category,
    @Default("true") boolean active
) {}

// category defaults to "GENERAL", active defaults to true
Product product = ProductBuilder.create()
    .name("Laptop")
    .price(1500.0)
    .build();
// product.category() == "GENERAL"
// product.active() == true

// Explicit values override defaults
Product custom = ProductBuilder.create()
    .name("Widget")
    .price(9.99)
    .category("ACCESSORIES")
    .active(false)
    .build();
// custom.category() == "ACCESSORIES"
```

The `@Default` annotation works on both **constructor parameters** (records) and **fields** (classes with setters). The `value()` is a string expression interpreted based on the field type:

- **String** — wrapped in double quotes automatically (e.g. `@Default("GENERAL")` generates `"GENERAL"`)
- **char** — wrapped in single quotes (e.g. `@Default("A")` generates `'A'`)
- **numeric/boolean primitives** — used as-is (e.g. `@Default("0.0")` generates `0.0`)
- **complex types** — used as a raw Java expression (e.g. `@Default("List.of()")` generates `List.of()`)

**Framework-agnostic detection:** The processor also detects annotations named `Default` or `DefaultValue` from any package (e.g. Jakarta REST `@DefaultValue`) if they have a `String value()` member.

**Interaction with non-null checks:** A field with a `@Default` is never considered "required" — even if annotated with `@NotNull`, no validation error is raised when the field is unset.

#### Conditional Builder Logic

Apply builder modifications conditionally using the `conditional()` method:

```java
int age = 45;
Person person = PersonBuilder.create()
    .name("Jane Doe")
    .conditional(
        () -> age >= 18,
        p -> p.role("ADULT"),
        p -> p.role("MINOR"))
    .build();
```

For simple conditions without an else case, use the two-parameter overload:

```java
PersonBuilder.create()
    .name("John Doe")
    .conditional(() -> isPremiumUser, p -> p.discountRate(0.15))
    .build();
```

### Collections and Nested Objects

Simple Builders provides special handling for collections and nested objects:

```java
@SimpleBuilder
public class Project {
    private String name;
    private List<Task> tasks;
    private Map<String, String> metadata;
    private ProjectStatus status;
    
    // Getters and setters...
}

@SimpleBuilder
public class Task {
    private String title;
    private String description;
    private boolean completed;
    
    // Getters and setters...
}

public enum ProjectStatus {
    PLANNING, IN_PROGRESS, COMPLETED, ON_HOLD
}
```

Usage with collections and nested builders:

```java
String version = "1.0.0";
Project project = ProjectBuilder.create()
    .name("Simple Builders in version '%s' with a bit of complexity", version)
    .status(ProjectStatus.IN_PROGRESS)
    .tasks(tasks -> tasks
        .add(taskBuilder -> taskBuilder
            .title("Implement core functionality")
            .completed(true)
        )
        .add(taskBuilder -> taskBuilder
            .title("Add documentation")
            .description("Update README and add Javadocs")
        )
    )
    .metadata(metadata -> metadata
        .put("version", "1.0.0")
        .put("owner", "dev-team"))
    .build();
```

#### Collection Immutability

Immutability of collections in the built object is the target type's responsibility. Simple
Builders stores exactly what the target type stores and does not silently wrap collections, so the
target type's collection contract remains intact. For a record, enforce that contract in its
canonical or compact constructor:

```java
@SimpleBuilder
public record Basket(List<String> items) {
    public Basket {
        items = List.copyOf(items);
    }
}
```

The builder passes its collection value to `Basket`; the record makes the stored collection
unmodifiable.


### With Interface Pattern

Simple Builders generates a nested `With` interface for each builder field, enabling a clean, type-safe way to create modified copies of objects. This pattern is particularly useful for creating variations of an object:

```java
Person person = PersonBuilder.create()
    .name("John Doe")
    .age(30)
    .build();

// Create a modified copy using the With interface
Person olderPerson = PersonBuilder.create()
    .with(person)
    .age(31)  // Only change the age
    .build();

// By implementing the With interface, you can create modified copies of objects in a type-safe way
Person youngerPerson = person.with(p -> p.age(29));
```

The `With` interface provides type-safe setter methods that mirror the builder's API, making it easy to create object variations without manually copying all fields.

### Builder Configuration

Simple Builders provides extensive configuration options to customize the generated builder code. You can control:

- Field setter generation (Supplier, Consumer, Builder patterns)
- Conditional logic helpers
- Access modifiers for builders and methods
- Collection helper methods
- Integration features

Configuration can be applied per-class using `@SimpleBuilder.Options` annotation or project-wide using compiler options.

#### Compiler Arguments

All configuration options are available as compiler arguments using the `-A` flag. For example:

```bash
javac -Asimplebuilder.verbose=true \
      -Asimplebuilder.generateFieldSupplier=false \
      YourClass.java
```

Or in Maven:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <compilerArgs>
            <arg>-Asimplebuilder.verbose=true</arg>
            <arg>-Asimplebuilder.generateFieldSupplier=false</arg>
        </compilerArgs>
    </configuration>
</plugin>
```

📋 **For a complete list of all available compiler arguments, see [`CompilerArgumentsEnum`](processor/src/main/java/org/javahelpers/simple/builders/processor/processing/CompilerArgumentsEnum.java).**

📖 **For complete documentation, examples, and all available options, see the [Configuration Guide](docs/CONFIGURATION.md).**

## Examples

The `example` module contains real-world examples demonstrating various builder configurations and features. You can explore the source DTOs and their generated builders:

### Elementary Builder Example

A comprehensive example showcasing all fundamental Java property types with a minimal, setter-only builder configuration:

- **Source DTO**: [`BookDto.java`](example/src/main/java/org/javahelpers/simple/builders/example/BookDto.java) - Demonstrates all primitive types, collections, Optional, BigDecimal, date/time types, and nested objects
- **Custom Annotation**: [`@ElementaryBuilder`](example/src/main/java/org/javahelpers/simple/builders/example/ElementaryBuilder.java) - A template annotation that disables all advanced features (suppliers, consumers, collection builders, With interface, @Generated annotation)
- **Generated Builder**: [`BookDtoBuilder.java`](example/generated-example-builder/org/javahelpers/simple/builders/example/BookDtoBuilder.java) - Clean, minimal builder with only setter methods
- **Tests**: [`BookDtoBuilderTest.java`](example/src/test/java/org/javahelpers/simple/builders/example/BookDtoBuilderTest.java) - Usage examples

### Built-in Minimal Builder Example

A DTO annotated with the built-in `@SimpleMinimalBuilder` template to generate a minimal builder without optional features:

- **Source DTO**: [`CustomerDto.java`](example/src/main/java/org/javahelpers/simple/builders/example/CustomerDto.java) - Uses `@SimpleMinimalBuilder` to generate a builder with only essential methods
- **Generated Builder**: [`CustomerDtoBuilder.java`](example/generated-example-builder/org/javahelpers/simple/builders/example/CustomerDtoBuilder.java) - Minimal builder output with `create()`, field setters, validation helpers, and `build()`

### Full-Featured Examples

Examples with all builder features enabled:

- **Person DTO**: [`PersonDto.java`](example/src/main/java/org/javahelpers/simple/builders/example/PersonDto.java) and [`PersonDtoBuilder.java`](example/generated-example-builder/org/javahelpers/simple/builders/example/PersonDtoBuilder.java) - Demonstrates nested objects, collections, suppliers, conditional logic, and various setter patterns
  - **Usage Examples**: [`PersonDtoBuilderTest.java`](example/src/test/java/org/javahelpers/simple/builders/example/PersonDtoBuilderTest.java) - Shows supplier methods, collection builders, nested builder consumers, and conditional logic
- **Product Record**: [`ProductRecord.java`](example/src/main/java/org/javahelpers/simple/builders/example/ProductRecord.java) and [`ProductRecordBuilder.java`](example/generated-example-builder/org/javahelpers/simple/builders/example/ProductRecordBuilder.java) - Java Record support with full builder features and With interface pattern
  - **Usage Examples**: [`ProductRecordTest.java`](example/src/test/java/org/javahelpers/simple/builders/example/ProductRecordTest.java) - Comprehensive tests demonstrating With interface for immutable Records, fluent modifications, and custom with methods

### Advanced Features

Examples demonstrating special annotations and nested object relationships:

- **Sponsor DTO**: [`SponsorDto.java`](example/src/main/java/org/javahelpers/simple/builders/example/SponsorDto.java) and [`SponsorDtoBuilder.java`](example/generated-example-builder/org/javahelpers/simple/builders/example/SponsorDtoBuilder.java) - Simple DTO used as nested object in other examples
- **Mannschaft DTO**: [`MannschaftDto.java`](example/src/main/java/org/javahelpers/simple/builders/example/MannschaftDto.java) and [`MannschaftDtoBuilder.java`](example/generated-example-builder/org/javahelpers/simple/builders/example/MannschaftDtoBuilder.java) - Demonstrates `@IgnoreInBuilder` annotation to exclude specific setter methods from the generated builder, plus Set collections with nested objects
- **Default Values**: [`ProductWithDefaults.java`](example/src/main/java/org/javahelpers/simple/builders/example/ProductWithDefaults.java) (record) and [`OrderWithDefaults.java`](example/src/main/java/org/javahelpers/simple/builders/example/OrderWithDefaults.java) (class) - Demonstrate `@Default` annotation for unset builder fields

### Builder Scoping Example

A runnable example demonstrating package-scoped builder generation and usage:

- **Source DTO**: [`ScopedOwnerDto.java`](example/src/main/java/org/javahelpers/simple/builders/example/scoping/ScopedOwnerDto.java) - Configures both package scopes inline and demonstrates the generation-scope, usage-scope, and out-of-scope field cases
- **Trusted helper**: [`TrustedHelperDto.java`](example/src/main/java/org/javahelpers/simple/builders/example/scoping/TrustedHelperDto.java) - In-generation-scope helper whose builder is referenced as a builder consumer
- **Library helper**: [`library/LibraryHelperDto.java`](example/src/main/java/org/javahelpers/simple/builders/example/library/LibraryHelperDto.java) - Annotated but outside the generation scope, so no builder exists and the owner falls back to a plain setter
- **Generated Builder**: [`ScopedOwnerDtoBuilder.java`](example/generated-example-builder/org/javahelpers/simple/builders/example/scoping/ScopedOwnerDtoBuilder.java) - Shows the consumer overload for `trusted` and plain setters for `library` and `sponsor`
- **Tests**: [`ScopedOwnerDtoBuilderTest.java`](example/src/test/java/org/javahelpers/simple/builders/example/scoping/ScopedOwnerDtoBuilderTest.java) - Asserts the generated API shape

These examples serve as both documentation and integration tests for the annotation processor.

## Performance Measurement

The `performance-test` module measures annotation processing performance across different builder frameworks (Simple Builders, MinimalBuilder, RecordBuilder, and Lombok). It generates ~1000 DTO classes from a JSON catalog, compiles them repeatedly, and aggregates timing results.

For the full guide — including quick-start, builder type shortcuts, Maven profiles, and wall-time-only mode for external processors — see [Performance Analysis Guide](performance-test/docs/PERFORMANCE_ANALYSIS.md).

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](docs/CONTRIBUTING.md) for:

- Development setup and project structure
- Building and testing strategies (important for annotation processor modules)
- Debugging with verbose output
- Code style and formatting
- Pull request process

For maintainers, see [RELEASE.md](RELEASE.md) for the release process.

For maintenance guarantees, the project's bus-factor, and a vendoring/fork
strategy for high-reliability adopters, see [GOVERNANCE.md](docs/GOVERNANCE.md).

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgements

This project was made possible thanks to the following:

### Inspiration and Patterns

- **[Benji Weber](https://benjiweber.co.uk/blog/2020/09/19/fun-with-java-records/)** - The With interface pattern is inspired by Benji's innovative work on functional builders and extending Java Records.
- **[RecordBuilder](https://github.com/Randgalt/record-builder)** by Randall Hauch - A state-of-the-art builder solution for Java records. If your project uses records exclusively, RecordBuilder is an excellent choice. Simple Builders extends these concepts to traditional Java classes.

### Tools and Libraries

- **[Roaster](https://github.com/forge/roaster)** - A fluent Java source generation and formatting library from the JBoss Forge ecosystem. Roaster is used to generate and format the builder source code.
- **[Google Compile Testing](https://github.com/google/compile-testing)** - Essential for testing annotation processors with comprehensive compilation diagnostics.

### Learning Resources

The following resources were invaluable for understanding annotation processing:

- **[Baeldung: Java Annotation Processing and Creating a Builder](https://www.baeldung.com/java-annotation-processing-builder)** - Comprehensive guide to annotation processing fundamentals
- **[Roaster GitHub Repository](https://github.com/forge/roaster)** - Reference for Java source generation and formatting with Roaster
- **[Annotation Processing Demo](https://github.com/ledungcobra/annotation-processing-demo)** by Le Dung - Hands-on examples of annotation processor implementation

Thank you to all contributors and the Java community for making this project possible!

## Links

* [Source code](https://github.com/java-helpers/simple-builders/)
* [Downloads](https://github.com/java-helpers/simple-builders/releases)
* [Issue tracker](https://github.com/java-helpers/simple-builders/issues)
* [CI build](https://github.com/java-helpers/simple-builders/actions/)
