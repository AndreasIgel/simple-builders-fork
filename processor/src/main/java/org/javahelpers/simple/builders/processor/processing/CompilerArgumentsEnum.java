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

package org.javahelpers.simple.builders.processor.processing;

/**
 * Enumeration of all builder configuration compiler arguments.
 *
 * <p>This enum provides a single source of truth for all option names used in:
 *
 * <ul>
 *   <li>Annotation methods in {@code SimpleBuilder.Options}
 *   <li>Compiler options (with {@code -A} prefix)
 *   <li>Configuration resolution
 * </ul>
 *
 * <p>Each enum constant provides the option name and the full compiler argument.
 */
public enum CompilerArgumentsEnum {
  // === Field Setter Generation ===
  /** Option for field supplier generation. */
  GENERATE_FIELD_SUPPLIER("generateFieldSupplier"),

  /** Option for field consumer generation. */
  GENERATE_FIELD_CONSUMER("generateFieldConsumer"),

  /** Option for builder consumer generation. */
  GENERATE_BUILDER_CONSUMER("generateBuilderConsumer"),

  // === Conditional Logic ===
  /** Option for conditional helper generation. */
  GENERATE_CONDITIONAL_HELPER("generateConditionalHelper"),

  // === Access Control ===
  /** Option for builder access level. */
  BUILDER_ACCESS("builderAccess"),

  /** Option for builder constructor access level. */
  BUILDER_CONSTRUCTOR_ACCESS("builderConstructorAccess"),

  /** Option for method access level. */
  METHOD_ACCESS("methodAccess"),

  // === Collection Options ===
  /** Option for varargs helper generation. */
  GENERATE_VAR_ARGS_HELPERS("generateVarArgsHelpers"),

  /** Option for string format helper generation. */
  GENERATE_STRING_FORMAT_HELPERS("generateStringFormatHelpers"),

  /** Option for mapper helper generation. */
  GENERATE_MAPPER_HELPERS("generateMapperHelpers"),

  /** Option for add to collection helper generation. */
  GENERATE_ADD_TO_COLLECTION_HELPERS("generateAddToCollectionHelpers"),

  /** Option for unboxed optional generation. */
  GENERATE_UNBOXED_OPTIONAL("generateUnboxedOptional"),

  /** Option for copying type annotations. */
  COPY_TYPE_ANNOTATIONS("copyTypeAnnotations"),

  /** Option for ArrayList builder usage. */
  USING_ARRAY_LIST_BUILDER("usingArrayListBuilder"),

  /** Option for ArrayList builder with element builders usage. */
  USING_ARRAY_LIST_BUILDER_WITH_ELEMENT_BUILDERS("usingArrayListBuilderWithElementBuilders"),

  /** Option for HashSet builder usage. */
  USING_HASH_SET_BUILDER("usingHashSetBuilder"),

  /** Option for HashSet builder with element builders usage. */
  USING_HASH_SET_BUILDER_WITH_ELEMENT_BUILDERS("usingHashSetBuilderWithElementBuilders"),

  /** Option for HashMap builder usage. */
  USING_HASH_MAP_BUILDER("usingHashMapBuilder"),

  // === Annotations ===
  /** Option for using Generated annotation. */
  USING_GENERATED_ANNOTATION("usingGeneratedAnnotation"),

  /** Option for using BuilderImplementation annotation. */
  USING_BUILDER_IMPLEMENTATION_ANNOTATION("usingBuilderImplementationAnnotation"),

  // === Integration ===
  /** Option for implementing IBuilderBase interface. */
  IMPLEMENTS_BUILDER_BASE("implementsBuilderBase"),

  /** Option for With interface generation. */
  GENERATE_WITH_INTERFACE("generateWithInterface"),

  /** Option for Jackson support. */
  USING_JACKSON_DESERIALIZER_ANNOTATION("usingJacksonDeserializerAnnotation"),

  /** Option for Jackson Module generation. */
  GENERATE_JACKSON_MODULE("generateJacksonModule"),

  /** Option for Javadoc generation on the generated builder. */
  GENERATE_JAVADOC("generateJavaDoc"),

  /** Option for Jackson Module package name. */
  JACKSON_MODULE_PACKAGE("jacksonModulePackage"),

  // === Naming ===
  /** Option for builder class name suffix. */
  BUILDER_SUFFIX("builderSuffix"),

  /** Option for setter method name suffix. */
  SETTER_SUFFIX("setterSuffix"),

  // === Component Filtering ===
  /**
   * Option for deactivating specific method generators and builder enhancers by class name pattern.
   */
  DEACTIVATE_GENERATION_COMPONENTS("deactivateGenerationComponents"),

  // === Debug Logging ===
  /** Option for verbose logging output. */
  VERBOSE("verbose"),

  // === Performance Optimization ===
  /**
   * Option to control the formatting mode for generated source files. Accepts values {@code jdt},
   * {@code lightweight}, or {@code none}. See {@link
   * org.javahelpers.simple.builders.core.enums.FormattingMode} for details.
   */
  FORMATTING_MODE("formattingMode"),

  // === Performance Tracking ===
  /** Option for performance tracking during annotation processing. */
  PERFORMANCE_TRACKING("performanceTracking"),

  /** Option for performance report JSON output file path. */
  PERFORMANCE_OUTPUT_FILE("performanceOutputFile"),

  // === Error Handling ===
  /**
   * Option for strict/fail-fast generation mode. When enabled, builder (and Jackson module)
   * generation failures are reported as compiler errors that fail the build instead of warnings.
   * Defaults to disabled (warnings only, build does not fail).
   */
  STRICT("strict");

  /** Compiler option prefix for all simple-builders options. */
  private static final String OPTION_PREFIX = "simplebuilder.";

  /** The option name (used in annotation methods). */
  private final String optionName;

  /**
   * Constructs a CompilerArgumentsEnum constant.
   *
   * @param optionName The option name
   */
  CompilerArgumentsEnum(String optionName) {
    this.optionName = optionName;
  }

  /**
   * Gets the option name for use in annotation methods.
   *
   * <p>Example: {@code "generateFieldSupplier"}
   *
   * @return The option name
   */
  public String getOptionName() {
    return optionName;
  }

  /**
   * Gets the full compiler argument including the package prefix.
   *
   * <p>Example: {@code "simplebuilder.generateFieldSupplier"}
   *
   * @return The full compiler argument
   */
  public String getCompilerArgument() {
    return OPTION_PREFIX + optionName;
  }

  /**
   * Finds a CompilerArgumentsEnum by its compiler argument.
   *
   * @param compilerArgument The compiler argument to search for
   * @return The matching CompilerArgumentsEnum, or null if not found
   */
  public static CompilerArgumentsEnum fromCompilerArgument(String compilerArgument) {
    for (CompilerArgumentsEnum option : values()) {
      if (option.getCompilerArgument().equals(compilerArgument)) {
        return option;
      }
    }
    return null;
  }
}
