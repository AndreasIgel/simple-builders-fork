package org.javahelpers.simple.builders.processor.testing;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.tools.JavaFileObject;
import org.apache.commons.lang3.Strings;
import org.javahelpers.simple.builders.processor.BuilderProcessor;

/**
 * Utilities to simplify annotation-processor tests by reducing boilerplate for building sources,
 * compiling them, and retrieving generated files.
 */
public final class ProcessorTestUtils {

  private static final Pattern PACKAGE_PATTERN =
      Pattern.compile("(?m)^\\s*package\\s+([a-zA-Z_]\\w*(?:\\.[a-zA-Z_]\\w*)*)\\s*;");
  private static final Pattern TOP_LEVEL_TYPE_PATTERN =
      Pattern.compile(
          "(?m)^[ \\t]*(?:(?:public|protected|private|abstract|final|static|sealed|non-sealed|strictfp)\\s+)*(?:@?interface|class|enum|record)\\s+([A-Za-z_]\\w*)\\b");

  private ProcessorTestUtils() {}

  /**
   * Creates a configured {@link Compiler} instance with the BuilderProcessor.
   *
   * <p>This method checks for the system property {@code simplebuilder.verbose} (or {@code
   * Averbose}) and automatically adds {@code -Averbose=true} to the compiler options if either is
   * set to "true".
   *
   * <p>This allows developers to enable verbose processor output for all tests by running: {@code
   * mvn test -Dsimplebuilder.verbose=true}
   *
   * @return a Compiler instance configured with BuilderProcessor and optional verbose output
   */
  public static Compiler createCompiler() {
    Compiler compiler = Compiler.javac().withProcessors(new BuilderProcessor());

    // Check for verbose flag from Maven property
    if (isVerboseEnabled()) {
      compiler = compiler.withOptions("-Averbose=true");
    }

    return compiler;
  }

  /**
   * Checks if verbose mode is enabled via system properties.
   *
   * @return true if simplebuilder.verbose or Averbose is set to "true"
   */
  public static boolean isVerboseEnabled() {
    String verboseProperty = System.getProperty("simplebuilder.verbose");
    String averboseProperty = System.getProperty("Averbose");
    return Strings.CI.equalsAny("true", verboseProperty, averboseProperty);
  }

  /**
   * Prints compilation diagnostics (notes, warnings, errors) and generated source files to
   * System.out if verbose mode is enabled.
   *
   * <p>This is useful for debugging test failures, as it makes the processor's debug output and
   * generated code visible in the test console output and CI logs.
   *
   * @param compilation the compilation result to print diagnostics from
   */
  public static void printDiagnosticsOnVerbose(Compilation compilation) {
    if (!isVerboseEnabled()) {
      return;
    }

    System.out.println("\n========== Compilation Diagnostics ==========");

    // Print notes (includes debug messages)
    if (!compilation.notes().isEmpty()) {
      System.out.println("--- NOTES ---");
      compilation.notes().forEach(diag -> System.out.println(diag.getMessage(null)));
    }

    // Print warnings
    if (!compilation.warnings().isEmpty()) {
      System.out.println("\n--- WARNINGS ---");
      compilation.warnings().forEach(diag -> System.out.println(diag.getMessage(null)));
    }

    // Print errors
    if (!compilation.errors().isEmpty()) {
      System.out.println("\n--- ERRORS ---");
      compilation.errors().forEach(diag -> System.out.println(diag.getMessage(null)));
    }

    System.out.println("=============================================\n");

    // Print generated source files
    printGeneratedSourcesOnVerbose(compilation);
  }

  /**
   * Prints all generated source files to System.out if verbose mode is enabled.
   *
   * <p>This displays the actual generated code before assertions run, making it easy to compare
   * expected vs actual output without debugging.
   *
   * @param compilation the compilation result containing generated files
   */
  public static void printGeneratedSourcesOnVerbose(Compilation compilation) {
    if (!isVerboseEnabled()) {
      return;
    }

    var generatedFiles = compilation.generatedSourceFiles();
    if (generatedFiles.isEmpty()) {
      System.out.println("========== No Source Files Generated ==========\n");
      return;
    }

    System.out.println("========== Generated Source Files ==========");
    generatedFiles.forEach(
        file -> {
          try {
            String fileName = file.getName();
            String content = file.getCharContent(false).toString();
            System.out.println("\n--- " + fileName + " ---");
            System.out.println(content);
            System.out.println("--- End of " + fileName + " ---");
          } catch (Exception e) {
            System.err.println("Failed to read generated file: " + e.getMessage());
          }
        });
    System.out.println("=============================================\n");
  }

  /**
   * Creates a {@link JavaFileObject} for a simple class annotated with @SimpleBuilder. You pass the
   * inner body lines (fields/methods); imports and annotation are handled for you.
   */
  public static JavaFileObject simpleBuilderClass(
      String packageName, String className, List<String> bodyLines) {
    String fqcn = packageName + "." + className;
    String[] lines = buildSourceLines(packageName, className, bodyLines);
    return JavaFileObjects.forSourceLines(fqcn, lines);
  }

  /**
   * Creates a {@link JavaFileObject} using a single body string (ideal for Java text blocks """ ...
   * """). The body is placed inside the annotated class.
   */
  public static JavaFileObject simpleBuilderClass(
      String packageName, String className, String body) {
    String fqcn = packageName + "." + className;
    String source =
        "package "
            + packageName
            + ";\n\n"
            + "import "
            + org.javahelpers.simple.builders.core.annotations.SimpleBuilder.class.getName()
            + ";\n\n"
            + "@SimpleBuilder\n"
            + "public class "
            + className
            + " {\n"
            + body
            + (body.endsWith("\n") ? "" : "\n")
            + "}\n";
    return JavaFileObjects.forSourceString(fqcn, source);
  }

  /**
   * Creates a {@link JavaFileObject} from a full Java source string by extracting the package name
   * and the top-level type name.
   *
   * <p>This is useful when you already have a complete source text block and want to avoid
   * duplicating the fully qualified class name separately.
   *
   * @param source full Java source code (may or may not declare a package)
   * @return JavaFileObject suitable for compilation testing
   * @throws IllegalArgumentException if the top-level type name cannot be determined
   */
  public static JavaFileObject forSource(String source) {
    String pkg = extractPackageName(source);
    String type = extractTopLevelTypeName(source);
    if (type == null || type.isBlank()) {
      throw new IllegalArgumentException("Cannot determine top-level type name from source.");
    }
    String fqcn = (pkg == null || pkg.isBlank()) ? type : (pkg + "." + type);
    return JavaFileObjects.forSourceString(fqcn, source);
  }

  private static String extractPackageName(String source) {
    Matcher m = PACKAGE_PATTERN.matcher(source);
    return m.find() ? m.group(1) : null;
  }

  private static String extractTopLevelTypeName(String source) {
    Matcher m = TOP_LEVEL_TYPE_PATTERN.matcher(source);
    return m.find() ? m.group(1) : null;
  }

  /**
   * Load the generated builder source code by simple name from the given compilation. No assertions
   * are performed here; callers should assert separately.
   */
  public static String loadGeneratedSource(Compilation compilation, String builderSimpleName) {
    JavaFileObject file =
        compilation.generatedFiles().stream()
            .filter(f -> f.getKind() == JavaFileObject.Kind.SOURCE)
            .filter(f -> f.getName().endsWith(builderSimpleName + ".java"))
            .findFirst()
            .orElseThrow(
                () -> new AssertionError("Generated source file not found: " + builderSimpleName));
    try {
      return file.getCharContent(true).toString();
    } catch (Exception e) {
      throw new RuntimeException("Failed to read generated source for: " + builderSimpleName, e);
    }
  }

  private static String[] buildSourceLines(
      String packageName, String className, List<String> bodyLines) {
    // Build lines with imports + annotation + class skeleton
    String[] header =
        new String[] {
          "package " + packageName + ";",
          "",
          "import "
              + org.javahelpers.simple.builders.core.annotations.SimpleBuilder.class.getName()
              + ";",
          "",
          "@SimpleBuilder",
          "public class " + className + " {"
        };

    String[] footer = new String[] {"}"};

    String[] body = bodyLines.toArray(String[]::new);

    String[] lines = new String[header.length + body.length + footer.length];
    System.arraycopy(header, 0, lines, 0, header.length);
    System.arraycopy(body, 0, lines, header.length, body.length);
    System.arraycopy(footer, 0, lines, header.length + body.length, footer.length);
    return lines;
  }

  /**
   * Creates a mock annotation for testing without any members.
   *
   * <p>Mock annotations are used instead of real dependencies because:
   *
   * <ul>
   *   <li>Annotation processors work at compile-time and only see annotation metadata, not
   *       implementations
   *   <li>Avoids adding unnecessary runtime dependencies to the test classpath
   *   <li>Ensures test isolation from external library versions
   *   <li>Follows standard practice for annotation processor testing with Google Compile Testing
   * </ul>
   *
   * <p>The processor only cares about the qualified name, not the actual implementation.
   *
   * @param packageName the package name of the annotation (e.g., "jakarta.validation")
   * @param annotationName the simple name of the annotation (e.g., "Valid")
   * @param targets the annotation targets (e.g., "ElementType.FIELD, ElementType.PARAMETER")
   * @return a JavaFileObject representing the mocked annotation
   */
  public static JavaFileObject createMockAnnotation(
      String packageName, String annotationName, String targets) {
    return createMockAnnotation(packageName, annotationName, targets, "");
  }

  /**
   * Creates a mock annotation for testing with optional members.
   *
   * <p>Mock annotations are used instead of real dependencies because:
   *
   * <ul>
   *   <li>Annotation processors work at compile-time and only see annotation metadata, not
   *       implementations
   *   <li>Avoids adding unnecessary runtime dependencies to the test classpath
   *   <li>Ensures test isolation from external library versions
   *   <li>Follows standard practice for annotation processor testing with Google Compile Testing
   * </ul>
   *
   * <p>The processor only cares about the qualified name, not the actual implementation.
   *
   * @param packageName the package name of the annotation (e.g., "jakarta.validation")
   * @param annotationName the simple name of the annotation (e.g., "Valid")
   * @param targets the annotation targets (e.g., "ElementType.FIELD, ElementType.PARAMETER")
   * @param members the annotation members/attributes (e.g., "int min() default 0;")
   * @return a JavaFileObject representing the mocked annotation
   */
  public static JavaFileObject createMockAnnotation(
      String packageName, String annotationName, String targets, String members) {
    String qualifiedName = packageName + "." + annotationName;
    String membersSection = members.isEmpty() ? "" : "\n  " + members + "\n";

    return JavaFileObjects.forSourceString(
        qualifiedName,
        """
        package %s;
        import java.lang.annotation.ElementType;
        import java.lang.annotation.Retention;
        import java.lang.annotation.RetentionPolicy;
        import java.lang.annotation.Target;

        @Retention(RetentionPolicy.RUNTIME)
        @Target({%s})
        public @interface %s {%s}
        """
            .formatted(packageName, targets, annotationName, membersSection));
  }
}
