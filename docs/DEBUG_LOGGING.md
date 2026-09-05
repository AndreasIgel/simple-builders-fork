# Debug Logging for Simple Builders

The Simple Builders annotation processor supports conditional debug logging that provides detailed information about the builder generation process.

## Logging Levels

- **INFO**: Always visible - Shows success messages for each builder generated. When debug mode is enabled, INFO messages use hierarchical indentation.
- **WARNING**: Always visible - Shows when a builder cannot be generated (e.g., wrong annotation target, generation errors). When debug mode is enabled, WARNING messages use hierarchical indentation.
- **ERROR**: Always visible - Shows fatal configuration errors (e.g., unsupported JDK version). Stops compilation completely.
- **DEBUG**: Conditional - Shows detailed tracing of field discovery, method analysis, and code generation steps with hierarchical indentation

## Enabling Debug Logging

Debug logging uses `Diagnostic.Kind.OTHER` but is only activated when explicitly enabled via the compiler argument `-Averbose=true` or the Maven system property `-Dsimplebuilder.verbose=true`.

When Maven runs javac in-process, `mvn clean compile -Dsimplebuilder.verbose=true` works without an `-Averbose=${simplebuilder.verbose}` mapping in `pom.xml`, and the `-D` value overrides any `-A` mapping from the build file. Gradle also supports `-Dsimplebuilder.verbose=true` (or `systemProp.simplebuilder.verbose=true` in `gradle.properties`) with in-process compilation; use the `-A` mapping for forked compilation and IDE builds. Gradle does not track the property as a task input, so run `clean` or `--rerun-tasks` after changing it.

### Option 1: Via Maven Property (Recommended)

Add a property in your `pom.xml`:

```xml
<properties>
  <simplebuilder.verbose>false</simplebuilder.verbose>
</properties>

<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <version>3.13.0</version>
      <configuration>
        <compilerArgs>
          <arg>-Averbose=${simplebuilder.verbose}</arg>
        </compilerArgs>
      </configuration>
    </plugin>
  </plugins>
</build>
```

Then enable debug logging from command line:
```bash
mvn clean compile -Dsimplebuilder.verbose=true
```

### Option 2: Static Configuration

Set it permanently in your `pom.xml`:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <version>3.13.0</version>
      <configuration>
        <compilerArgs>
          <arg>-Averbose=true</arg>
        </compilerArgs>
      </configuration>
    </plugin>
  </plugins>
</build>
```

## Conditional Hierarchical Logging

The Simple Builders processor provides **conditional hierarchical logging** to balance readability in production with detailed debugging when needed:

### **When Debug Mode is DISABLED (Default):**
- INFO and WARNING messages appear flat without indentation
- Suitable for production systems where log noise should be minimized
- Example: `simple-builders: Successfully generated 3 builder(s) in this processing round`

### **When Debug Mode is ENABLED (-Averbose=true):**
- INFO and WARNING messages have no extra prefix, DEBUG messages are posted on `[INFO]` chanel so they have the prefix `[INFO] [DEBUG]`
- Provides full visibility into the processing hierarchy
- Example: `[INFO]         │  └─ simple-builders: Successfully generated 3 builder(s) in this processing round`
- Example: `[WARNING]      │  │  ├─ Builder field conflict: field 'name'...`
- Example: `[INFO] [DEBUG] │  │  │  └─ Processing method: setName`
- All message types align vertically despite different prefix lengths

This approach ensures clean production logs while maintaining full debugging capabilities when needed.

## Example Debug Output

When debug logging is enabled, you'll see detailed output with visual separators:

```
[INFO] simple-builders: PROCESSING ROUND START
[INFO] [DEBUG] simple-builders: Processing round started. Found 3 annotated elements.
[INFO] [DEBUG] Processing element: PersonDto
[INFO] [DEBUG] ├─ Extracting builder definition from: org.example.PersonDto
[INFO] [DEBUG] │  ├─ Builder will be generated as: org.example.PersonDtoBuilder
[INFO] [DEBUG] │  ├─ Analysing setters for finding fields
[INFO] [DEBUG] │  │  ├─ Analyzing method: setName(java.lang.String)
[INFO] [DEBUG] │  │  │  └─ Adding field: name (type: java.lang.String)
[INFO] [DEBUG] │  │  ├─ Analyzing method: setAge(int)
[INFO] [DEBUG] │  │  │  └─ Adding field: age (type: int)
[INFO] [DEBUG] │  └─ Processed 2 possible setters: added 2 fields, skipped 0
[INFO] [DEBUG] ├─ Code generation for builder: PersonDtoBuilder
[INFO] [DEBUG] │  ├─ Class builder created
[INFO] [DEBUG] │  ├─ Generating 0 constructor fields and 2 setter fields
[INFO] [DEBUG] │  │  └─ Fields added: 2 fields
[INFO] [DEBUG] │  ├─ Adding Methods for 4 candidates
[INFO] [DEBUG] │  │  └─ 4 Methods added
[INFO] [DEBUG] │  ├─ Writing builder class to file: org.example.PersonDtoBuilder
[INFO] [DEBUG] │  └─ Successfully generated builder: PersonDtoBuilder
[INFO] [DEBUG] Processing element: OrderDto
[INFO] [DEBUG] ├─ Extracting builder definition from: org.example.OrderDto
[INFO] [DEBUG] │  └─ Builder will be generated as: org.example.OrderDtoBuilder
[INFO] [DEBUG] │  └─ Processed 1 possible setters: added 1 fields, skipped 0
[INFO] [DEBUG] ├─ Code generation for builder: OrderDtoBuilder
[INFO] [DEBUG] │  └─ Successfully generated builder: OrderDtoBuilder
[INFO] [DEBUG] Processing element: CustomerDto (with conflicts)
[INFO] [DEBUG] ├─ Extracting builder definition from: org.example.CustomerDto
[INFO] [DEBUG] │  └─ Builder will be generated as: org.example.CustomerDtoBuilder
[INFO] [DEBUG] │  └─ Processed 2 possible setters: added 2 fields, skipped 0
[WARNING]      │  │  └─ Builder field conflict: field 'name' (type Optional) renamed to 'nameOptional' to avoid conflict
[INFO] [DEBUG] ├─ Code generation for builder: CustomerDtoBuilder
[INFO] [DEBUG] │  └─ Successfully generated builder: CustomerDtoBuilder
[INFO]         simple-builders: Successfully generated 3 builder(s) in this processing round
```

**Note**: Debug messages are prefixed with `[DEBUG]` and use `Diagnostic.Kind.OTHER` which appears as `[INFO]` in Maven output.

## Normal Output (Without Debug)

Without debug logging enabled, you only see the INFO-level messages:

```
[INFO] simple-builders: PROCESSING ROUND START
[INFO] simple-builders: Successfully generated 3 builder(s) in this processing round
```

## Troubleshooting

If debug logging is not appearing:

1. Verify the compiler plugin configuration includes the debug or verbose argument
2. Check that you're using Maven 3.6+ 
3. Ensure the annotation processor is actually running (check for generated files in `target/generated-sources/annotations`)
4. Try with `-X` flag to see all Maven debug output and verify the compiler arguments are being passed
