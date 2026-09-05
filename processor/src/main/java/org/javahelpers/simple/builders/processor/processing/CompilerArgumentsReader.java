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

import javax.annotation.processing.ProcessingEnvironment;
import org.apache.commons.lang3.Strings;
import org.javahelpers.simple.builders.core.enums.AccessModifier;
import org.javahelpers.simple.builders.core.enums.OptionState;
import org.javahelpers.simple.builders.processor.model.core.BuilderConfiguration;

/**
 * Utility class for reading compiler arguments from the annotation processing environment.
 *
 * <p>This class provides a centralized way to read compiler arguments using {@link
 * CompilerArgumentsEnum} values, ensuring consistent handling of option names and values across the
 * processor.
 */
public class CompilerArgumentsReader {
  private final ProcessingEnvironment processingEnv;

  /**
   * Constructs a new CompilerArgumentsReader.
   *
   * @param processingEnv the processing environment providing access to compiler options
   */
  public CompilerArgumentsReader(ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv;
  }

  /**
   * Reads the value of a compiler argument.
   *
   * <p>The method looks up the compiler argument using both the full compiler argument name (with
   * prefix) and the simple option name (without prefix) for backward compatibility.
   *
   * @param argument the compiler argument enum to read
   * @return the value of the compiler argument, or null if not set
   */
  public String readValue(CompilerArgumentsEnum argument) {
    // Try with full compiler argument name first (e.g., "simplebuilder.verbose")
    String value = processingEnv.getOptions().get(argument.getCompilerArgument());

    // Fall back to simple option name for backward compatibility (e.g., "verbose")
    if (value == null) {
      value = processingEnv.getOptions().get(argument.getOptionName());
    }

    return value;
  }

  /**
   * Reads the value of a compiler argument as a boolean.
   *
   * <p>Returns true if the value equals "true" (case-insensitive), false otherwise.
   *
   * @param argument the compiler argument enum to read
   * @return true if the value is "true" (case-insensitive), false otherwise
   */
  public boolean readBooleanValue(CompilerArgumentsEnum argument) {
    String value = readValue(argument);
    return Strings.CI.equalsAny(value, "true", "enabled");
  }

  /**
   * Reads the value of a compiler argument as an OptionState.
   *
   * <p>Returns ENABLED for "true" or "enabled", DISABLED for "false" or "disabled", and UNSET
   * otherwise.
   *
   * @param argument the compiler argument enum to read
   * @return the OptionState value
   */
  public OptionState readOptionState(CompilerArgumentsEnum argument) {
    String value = readValue(argument);
    if (Strings.CI.equalsAny(value, "true", "enabled")) {
      return OptionState.ENABLED;
    } else if (Strings.CI.equalsAny(value, "false", "disabled")) {
      return OptionState.DISABLED;
    }
    return OptionState.UNSET;
  }

  /**
   * Reads the value of a compiler argument as an AccessModifier.
   *
   * <p>Returns the corresponding AccessModifier enum value, or DEFAULT if not set or invalid.
   *
   * @param argument the compiler argument enum to read
   * @return the AccessModifier value, or DEFAULT if not set
   */
  public AccessModifier readAccessModifier(CompilerArgumentsEnum argument) {
    String value = readValue(argument);
    if (Strings.CI.equals(value, "public")) {
      return AccessModifier.PUBLIC;
    } else if (Strings.CI.equals(value, "private")) {
      return AccessModifier.PRIVATE;
    } else if (Strings.CI.equalsAny(value, "package-private", "package_private")) {
      return AccessModifier.PACKAGE_PRIVATE;
    } else {
      return AccessModifier.DEFAULT;
    }
  }

  /**
   * Reads a complete BuilderConfiguration from compiler arguments.
   *
   * <p>This method reads all configuration options from compiler arguments like:
   *
   * <ul>
   *   <li>{@code -Asimplebuilder.generateFieldSupplier=true}
   *   <li>{@code -Asimplebuilder.builderAccess=public}
   *   <li>etc.
   * </ul>
   *
   * <p>All values default to UNSET or DEFAULT if not specified in compiler arguments.
   *
   * <p><b>Adding a new option:</b> every option in {@link CompilerArgumentsEnum} that represents a
   * configuration value must be read and set here. Omitting it causes the compiler argument to be
   * silently ignored. The full checklist when adding a new option:
   *
   * <ol>
   *   <li>{@code CompilerArgumentsEnum} — add the enum constant.
   *   <li>This method — read the value and set it on the builder.
   *   <li>{@code BuilderConfiguration} — add the field, builder method, merge logic, and a typed
   *       accessor (e.g. {@code formattingModeEnum}) if enum conversion is needed. Set the default
   *       in {@code BuilderConfiguration.DEFAULT}.
   *   <li>{@code BuilderConfigurationReader} — handle annotation-side extraction in {@code
   *       extractOptionsFromAnnotationMirror}.
   *   <li>{@code ProcessingContext} — should NOT need a dedicated field or getter. The resolved
   *       per-target config ({@code context.getConfiguration()}) and global config ({@code
   *       context.getConfigurationReader().getGlobalConfiguration()}) carry all option values.
   *       Special-casing outside {@code BuilderConfiguration} breaks the merge chain and bypasses
   *       annotation overrides.
   * </ol>
   *
   * @return a BuilderConfiguration with values read from compiler arguments
   */
  public BuilderConfiguration readBuilderConfiguration() {
    return BuilderConfiguration.builder()
        .generateSupplier(readOptionState(CompilerArgumentsEnum.GENERATE_FIELD_SUPPLIER))
        .generateConsumer(readOptionState(CompilerArgumentsEnum.GENERATE_FIELD_CONSUMER))
        .generateBuilderConsumer(readOptionState(CompilerArgumentsEnum.GENERATE_BUILDER_CONSUMER))
        .generateConditionalLogic(
            readOptionState(CompilerArgumentsEnum.GENERATE_CONDITIONAL_HELPER))
        .builderAccess(readAccessModifier(CompilerArgumentsEnum.BUILDER_ACCESS))
        .builderConstructorAccess(
            readAccessModifier(CompilerArgumentsEnum.BUILDER_CONSTRUCTOR_ACCESS))
        .methodAccess(readAccessModifier(CompilerArgumentsEnum.METHOD_ACCESS))
        .generateVarArgsHelpers(readOptionState(CompilerArgumentsEnum.GENERATE_VAR_ARGS_HELPERS))
        .generateStringFormatHelpers(
            readOptionState(CompilerArgumentsEnum.GENERATE_STRING_FORMAT_HELPERS))
        .generateMapperHelpers(readOptionState(CompilerArgumentsEnum.GENERATE_MAPPER_HELPERS))
        .generateAddToCollectionHelpers(
            readOptionState(CompilerArgumentsEnum.GENERATE_ADD_TO_COLLECTION_HELPERS))
        .generateUnboxedOptional(readOptionState(CompilerArgumentsEnum.GENERATE_UNBOXED_OPTIONAL))
        .copyTypeAnnotations(readOptionState(CompilerArgumentsEnum.COPY_TYPE_ANNOTATIONS))
        .usingArrayListBuilder(readOptionState(CompilerArgumentsEnum.USING_ARRAY_LIST_BUILDER))
        .usingArrayListBuilderWithElementBuilders(
            readOptionState(CompilerArgumentsEnum.USING_ARRAY_LIST_BUILDER_WITH_ELEMENT_BUILDERS))
        .usingHashSetBuilder(readOptionState(CompilerArgumentsEnum.USING_HASH_SET_BUILDER))
        .usingHashSetBuilderWithElementBuilders(
            readOptionState(CompilerArgumentsEnum.USING_HASH_SET_BUILDER_WITH_ELEMENT_BUILDERS))
        .usingHashMapBuilder(readOptionState(CompilerArgumentsEnum.USING_HASH_MAP_BUILDER))
        .usingGeneratedAnnotation(readOptionState(CompilerArgumentsEnum.USING_GENERATED_ANNOTATION))
        .usingBuilderImplementationAnnotation(
            readOptionState(CompilerArgumentsEnum.USING_BUILDER_IMPLEMENTATION_ANNOTATION))
        .implementsBuilderBase(readOptionState(CompilerArgumentsEnum.IMPLEMENTS_BUILDER_BASE))
        .generateWithInterface(readOptionState(CompilerArgumentsEnum.GENERATE_WITH_INTERFACE))
        .usingJacksonDeserializerAnnotation(
            readOptionState(CompilerArgumentsEnum.USING_JACKSON_DESERIALIZER_ANNOTATION))
        .generateJacksonModule(readOptionState(CompilerArgumentsEnum.GENERATE_JACKSON_MODULE))
        .generateJavaDoc(readOptionState(CompilerArgumentsEnum.GENERATE_JAVADOC))
        .jacksonModulePackage(readValue(CompilerArgumentsEnum.JACKSON_MODULE_PACKAGE))
        .builderSuffix(readValue(CompilerArgumentsEnum.BUILDER_SUFFIX))
        .setterSuffix(readValue(CompilerArgumentsEnum.SETTER_SUFFIX))
        .formattingMode(readValue(CompilerArgumentsEnum.FORMATTING_MODE))
        .strict(readOptionState(CompilerArgumentsEnum.STRICT))
        .build();
  }
}
