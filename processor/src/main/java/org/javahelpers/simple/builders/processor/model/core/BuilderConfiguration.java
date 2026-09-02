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

package org.javahelpers.simple.builders.processor.model.core;

import static org.javahelpers.simple.builders.core.enums.AccessModifier.*;
import static org.javahelpers.simple.builders.core.enums.OptionState.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
import org.javahelpers.simple.builders.core.enums.AccessModifier;
import org.javahelpers.simple.builders.core.enums.FormattingMode;
import org.javahelpers.simple.builders.core.enums.OptionState;

/**
 * Configuration for builder generation. Combines annotation values with compiler options. Priority:
 * Annotation > Compiler Options > Defaults
 *
 * <p>Properties and defaults are read from {@link SimpleBuilder.Options} annotation defaults via
 * reflection.
 *
 * @param generateFieldSupplier Generate field supplier methods
 * @param generateFieldConsumer Generate field consumer methods
 * @param generateBuilderConsumer Generate builder consumer methods
 * @param generateConditionalHelper Generate conditional logic methods
 * @param builderAccess Access level for builder class
 * @param builderConstructorAccess Access level for builder constructors
 * @param methodAccess Access level for setter/fluent methods (NOT build() or create() which are
 *     always public)
 * @param generateVarArgsHelpers Generate varargs helper methods
 * @param generateStringFormatHelpers Generate string format helper methods
 * @param generateAddToCollectionHelpers Generate add2FieldName helper methods for collections
 * @param generateUnboxedOptional Generate unboxed optional methods
 * @param usingArrayListBuilder Use ArrayListBuilder for lists
 * @param usingArrayListBuilderWithElementBuilders Use ArrayListBuilderWithElementBuilders
 * @param usingHashSetBuilder Use HashSetBuilder for sets
 * @param usingHashSetBuilderWithElementBuilders Use HashSetBuilderWithElementBuilders
 * @param usingHashMapBuilder Use HashMapBuilder for maps
 * @param usingGeneratedAnnotation Use Generated annotation
 * @param usingBuilderImplementationAnnotation Use BuilderImplementation annotation
 * @param implementsBuilderBase Implement IBuilderBase interface
 * @param generateWithInterface Generate With interface
 * @param generateJavaDoc Generate Javadoc comments
 * @param builderSuffix Suffix for builder class name
 * @param setterSuffix Suffix for setter method names
 * @param formattingMode Formatting mode for generated source code (null = inherit from compiler
 *     arg)
 * @param strict Strict/fail-fast generation mode
 */
public record BuilderConfiguration(
    OptionState generateFieldSupplier,
    OptionState generateFieldConsumer,
    OptionState generateBuilderConsumer,
    OptionState generateConditionalHelper,
    AccessModifier builderAccess,
    AccessModifier builderConstructorAccess,
    AccessModifier methodAccess,
    OptionState generateVarArgsHelpers,
    OptionState generateStringFormatHelpers,
    OptionState generateAddToCollectionHelpers,
    OptionState generateUnboxedOptional,
    OptionState copyTypeAnnotations,
    OptionState usingArrayListBuilder,
    OptionState usingArrayListBuilderWithElementBuilders,
    OptionState usingHashSetBuilder,
    OptionState usingHashSetBuilderWithElementBuilders,
    OptionState usingHashMapBuilder,
    OptionState usingGeneratedAnnotation,
    OptionState usingBuilderImplementationAnnotation,
    OptionState implementsBuilderBase,
    OptionState generateWithInterface,
    OptionState usingJacksonDeserializerAnnotation,
    OptionState generateJacksonModule,
    OptionState generateJavaDoc,
    String jacksonModulePackage,
    String builderSuffix,
    String setterSuffix,
    String formattingMode,
    OptionState strict) {

  public static final BuilderConfiguration DEFAULT =
      builder()
          .generateSupplier(ENABLED)
          .generateConsumer(ENABLED)
          .generateBuilderConsumer(ENABLED)
          .generateConditionalLogic(ENABLED)
          .builderAccess(PUBLIC)
          .builderConstructorAccess(PUBLIC)
          .methodAccess(PUBLIC)
          .generateVarArgsHelpers(ENABLED)
          .generateStringFormatHelpers(ENABLED)
          .generateAddToCollectionHelpers(ENABLED)
          .generateUnboxedOptional(ENABLED)
          .copyTypeAnnotations(ENABLED)
          .usingArrayListBuilder(ENABLED)
          .usingArrayListBuilderWithElementBuilders(ENABLED)
          .usingHashSetBuilder(ENABLED)
          .usingHashSetBuilderWithElementBuilders(ENABLED)
          .usingHashMapBuilder(ENABLED)
          .usingGeneratedAnnotation(ENABLED)
          .usingBuilderImplementationAnnotation(ENABLED)
          .implementsBuilderBase(ENABLED)
          .generateWithInterface(ENABLED)
          .usingJacksonDeserializerAnnotation(DISABLED)
          .generateJacksonModule(DISABLED)
          .generateJavaDoc(ENABLED)
          .jacksonModulePackage(null)
          .builderSuffix("Builder")
          .setterSuffix("")
          .formattingMode(FormattingMode.JDT.getOptionValue())
          .strict(DISABLED)
          .build();

  // === Convenience accessors with 'is' prefix for boolean properties ===
  public boolean shouldGenerateFieldSupplier() {
    return generateFieldSupplier == ENABLED;
  }

  public boolean shouldGenerateFieldConsumer() {
    return generateFieldConsumer == ENABLED;
  }

  public boolean shouldGenerateBuilderConsumer() {
    return generateBuilderConsumer == ENABLED;
  }

  public boolean shouldGenerateConditionalLogic() {
    return generateConditionalHelper == ENABLED;
  }

  public boolean shouldGenerateWithInterface() {
    return generateWithInterface == ENABLED;
  }

  public boolean shouldUseJacksonDeserializerAnnotation() {
    return usingJacksonDeserializerAnnotation == ENABLED;
  }

  public boolean shouldGenerateJacksonModule() {
    return generateJacksonModule == ENABLED;
  }

  public boolean shouldGenerateJavaDoc() {
    return generateJavaDoc == ENABLED;
  }

  public boolean shouldGenerateVarArgsHelpers() {
    return generateVarArgsHelpers == ENABLED;
  }

  public boolean shouldGenerateStringFormatHelpers() {
    return generateStringFormatHelpers == ENABLED;
  }

  public boolean shouldGenerateAddToCollectionHelpers() {
    return generateAddToCollectionHelpers == ENABLED;
  }

  public boolean shouldUseArrayListBuilder() {
    return usingArrayListBuilder == ENABLED;
  }

  public boolean shouldUseArrayListBuilderWithElementBuilders() {
    return usingArrayListBuilderWithElementBuilders == ENABLED;
  }

  public boolean shouldUseHashSetBuilder() {
    return usingHashSetBuilder == ENABLED;
  }

  public boolean shouldUseHashSetBuilderWithElementBuilders() {
    return usingHashSetBuilderWithElementBuilders == ENABLED;
  }

  public boolean shouldUseHashMapBuilder() {
    return usingHashMapBuilder == ENABLED;
  }

  public boolean shouldGenerateUnboxedOptional() {
    return generateUnboxedOptional == ENABLED;
  }

  public boolean shouldCopyTypeAnnotations() {
    return copyTypeAnnotations == ENABLED;
  }

  public boolean shouldUseGeneratedAnnotation() {
    return usingGeneratedAnnotation == ENABLED;
  }

  public boolean shouldUseBuilderImplementationAnnotation() {
    return usingBuilderImplementationAnnotation == ENABLED;
  }

  public boolean shouldImplementBuilderBase() {
    return implementsBuilderBase == ENABLED;
  }

  // === String accessors ===
  public AccessModifier getBuilderAccess() {
    return builderAccess;
  }

  public AccessModifier getBuilderConstructorAccess() {
    return builderConstructorAccess;
  }

  public AccessModifier getMethodAccess() {
    return methodAccess;
  }

  public String getJacksonModulePackage() {
    return jacksonModulePackage;
  }

  public String getBuilderSuffix() {
    return builderSuffix;
  }

  public String getSetterSuffix() {
    return setterSuffix;
  }

  public boolean isStrictModeEnabled() {
    return strict == ENABLED;
  }

  public FormattingMode formattingModeEnum() {
    return FormattingMode.fromString(formattingMode);
  }

  /**
   * Merges this configuration with another configuration.
   *
   * <p>The other configuration takes priority: if a field in the other configuration is not
   * UNSET/DEFAULT, it will override the value from this configuration. If the other configuration
   * is null, this configuration is returned unchanged.
   *
   * @param other the configuration to merge with this one (can be null)
   * @return a new BuilderConfiguration with merged values
   */
  public BuilderConfiguration merge(BuilderConfiguration other) {
    if (other == null) {
      return this;
    }

    return BuilderConfiguration.builder()
        .generateSupplier(mergeOptionState(other.generateFieldSupplier, this.generateFieldSupplier))
        .generateConsumer(mergeOptionState(other.generateFieldConsumer, this.generateFieldConsumer))
        .generateBuilderConsumer(
            mergeOptionState(other.generateBuilderConsumer, this.generateBuilderConsumer))
        .generateConditionalLogic(
            mergeOptionState(other.generateConditionalHelper, this.generateConditionalHelper))
        .builderAccess(mergeAccessModifier(other.builderAccess, this.builderAccess))
        .builderConstructorAccess(
            mergeAccessModifier(other.builderConstructorAccess, this.builderConstructorAccess))
        .methodAccess(mergeAccessModifier(other.methodAccess, this.methodAccess))
        .generateVarArgsHelpers(
            mergeOptionState(other.generateVarArgsHelpers, this.generateVarArgsHelpers))
        .generateStringFormatHelpers(
            mergeOptionState(other.generateStringFormatHelpers, this.generateStringFormatHelpers))
        .generateAddToCollectionHelpers(
            mergeOptionState(
                other.generateAddToCollectionHelpers, this.generateAddToCollectionHelpers))
        .generateUnboxedOptional(
            mergeOptionState(other.generateUnboxedOptional, this.generateUnboxedOptional))
        .copyTypeAnnotations(mergeOptionState(other.copyTypeAnnotations, this.copyTypeAnnotations))
        .usingArrayListBuilder(
            mergeOptionState(other.usingArrayListBuilder, this.usingArrayListBuilder))
        .usingArrayListBuilderWithElementBuilders(
            mergeOptionState(
                other.usingArrayListBuilderWithElementBuilders,
                this.usingArrayListBuilderWithElementBuilders))
        .usingHashSetBuilder(mergeOptionState(other.usingHashSetBuilder, this.usingHashSetBuilder))
        .usingHashSetBuilderWithElementBuilders(
            mergeOptionState(
                other.usingHashSetBuilderWithElementBuilders,
                this.usingHashSetBuilderWithElementBuilders))
        .usingHashMapBuilder(mergeOptionState(other.usingHashMapBuilder, this.usingHashMapBuilder))
        .usingGeneratedAnnotation(
            mergeOptionState(other.usingGeneratedAnnotation, this.usingGeneratedAnnotation))
        .usingBuilderImplementationAnnotation(
            mergeOptionState(
                other.usingBuilderImplementationAnnotation,
                this.usingBuilderImplementationAnnotation))
        .implementsBuilderBase(
            mergeOptionState(other.implementsBuilderBase, this.implementsBuilderBase))
        .generateWithInterface(
            mergeOptionState(other.generateWithInterface, this.generateWithInterface))
        .usingJacksonDeserializerAnnotation(
            mergeOptionState(
                other.usingJacksonDeserializerAnnotation, this.usingJacksonDeserializerAnnotation))
        .generateJacksonModule(
            mergeOptionState(other.generateJacksonModule, this.generateJacksonModule))
        .generateJavaDoc(mergeOptionState(other.generateJavaDoc, this.generateJavaDoc))
        .jacksonModulePackage(mergeString(other.jacksonModulePackage, this.jacksonModulePackage))
        .builderSuffix(mergeString(other.builderSuffix, this.builderSuffix))
        .setterSuffix(mergeString(other.setterSuffix, this.setterSuffix))
        .formattingMode(mergeString(other.formattingMode, this.formattingMode))
        .strict(mergeOptionState(other.strict, this.strict))
        .build();
  }

  /**
   * Merges two OptionState values, preferring the other value if it's not UNSET.
   *
   * @param other the other value (higher priority)
   * @param thisValue the current value (lower priority)
   * @return the merged value
   */
  private static OptionState mergeOptionState(OptionState other, OptionState thisValue) {
    return other != UNSET ? other : thisValue;
  }

  /**
   * Merges two AccessModifier values, preferring the other value if it's not DEFAULT.
   *
   * @param other the other value (higher priority)
   * @param thisValue the current value (lower priority)
   * @return the merged value
   */
  private static AccessModifier mergeAccessModifier(
      AccessModifier other, AccessModifier thisValue) {
    return other != AccessModifier.DEFAULT ? other : thisValue;
  }

  /**
   * Merges two String values, preferring the other value if it's not null.
   *
   * <p>Note: String values are normalized with trimToNull in the builder, so null means
   * unset/blank.
   *
   * @param other the other value (higher priority)
   * @param thisValue the current value (lower priority)
   * @return the merged value
   */
  private static String mergeString(String other, String thisValue) {
    return other != null && !other.isEmpty() ? other : thisValue;
  }

  @Override
  public String toString() {
    return new ConfigToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .appendValueIfSet("generateFieldSupplier", generateFieldSupplier)
        .appendValueIfSet("generateFieldConsumer", generateFieldConsumer)
        .appendValueIfSet("generateBuilderConsumer", generateBuilderConsumer)
        .appendValueIfSet("generateConditionalHelper", generateConditionalHelper)
        .appendIfNotDefault("builderAccess", builderAccess)
        .appendIfNotDefault("methodAccess", methodAccess)
        .appendValueIfSet("generateVarArgsHelpers", generateVarArgsHelpers)
        .appendValueIfSet("generateUnboxedOptional", generateUnboxedOptional)
        .appendValueIfSet("copyTypeAnnotations", copyTypeAnnotations)
        .appendValueIfSet("usingArrayListBuilder", usingArrayListBuilder)
        .appendValueIfSet(
            "usingArrayListBuilderWithElementBuilders", usingArrayListBuilderWithElementBuilders)
        .appendValueIfSet("usingHashSetBuilder", usingHashSetBuilder)
        .appendValueIfSet(
            "usingHashSetBuilderWithElementBuilders", usingHashSetBuilderWithElementBuilders)
        .appendValueIfSet("usingHashMapBuilder", usingHashMapBuilder)
        .appendValueIfSet("generateWithInterface", generateWithInterface)
        .appendValueIfSet("usingJacksonDeserializerAnnotation", usingJacksonDeserializerAnnotation)
        .appendValueIfSet("generateJacksonModule", generateJacksonModule)
        .appendValueIfSet("generateJavaDoc", generateJavaDoc)
        .appendIfNotEmpty("jacksonModulePackage", jacksonModulePackage)
        .appendIfNotEmpty("builderSuffix", builderSuffix)
        .appendIfNotEmpty("setterSuffix", setterSuffix)
        .appendIfNotEmpty("formattingMode", formattingMode)
        .appendValueIfSet("strict", strict)
        .toString();
  }

  private static class ConfigToStringBuilder {
    final ToStringBuilder builder;

    public ConfigToStringBuilder(
        final BuilderConfiguration builderConfig, final ToStringStyle style) {
      this.builder = new ToStringBuilder(builderConfig, style);
    }

    public ConfigToStringBuilder appendIfNotEmpty(String fieldName, String value) {
      if (value != null && !value.isEmpty()) {
        builder.append(fieldName, value);
      }
      return this;
    }

    public ConfigToStringBuilder appendIfNotDefault(String fieldName, AccessModifier value) {
      if (value != AccessModifier.DEFAULT) {
        builder.append(fieldName, value);
      }
      return this;
    }

    public ConfigToStringBuilder appendValueIfSet(String fieldName, OptionState optionState) {
      if (optionState != OptionState.UNSET) {
        builder.append(fieldName, optionState);
      }
      return this;
    }

    public String toString() {
      return builder.toString();
    }
  }

  // === Builder Pattern ===
  // All defaults are DEFAULT to allow proper three-state resolution
  public static class Builder {
    // === Field Setter Generation ===
    private OptionState generateFieldSupplier = OptionState.UNSET;
    private OptionState generateFieldConsumer = OptionState.UNSET;
    private OptionState generateBuilderConsumer = OptionState.UNSET;

    // === Conditional Logic ===
    private OptionState generateConditionalHelper = OptionState.UNSET;

    // === Access Control ===
    private AccessModifier builderAccess = AccessModifier.DEFAULT;
    private AccessModifier builderConstructorAccess = AccessModifier.DEFAULT;
    private AccessModifier methodAccess = AccessModifier.DEFAULT;

    // === Collection Options ===
    private OptionState generateVarArgsHelpers = OptionState.UNSET;
    private OptionState generateStringFormatHelpers = OptionState.UNSET;
    private OptionState generateAddToCollectionHelpers = OptionState.UNSET;
    private OptionState generateUnboxedOptional = OptionState.UNSET;
    private OptionState copyTypeAnnotations = OptionState.UNSET;
    private OptionState usingArrayListBuilder = OptionState.UNSET;
    private OptionState usingArrayListBuilderWithElementBuilders = OptionState.UNSET;
    private OptionState usingHashSetBuilder = OptionState.UNSET;
    private OptionState usingHashSetBuilderWithElementBuilders = OptionState.UNSET;
    private OptionState usingHashMapBuilder = OptionState.UNSET;

    // === Annotations ===
    private OptionState usingGeneratedAnnotation = OptionState.UNSET;
    private OptionState usingBuilderImplementationAnnotation = OptionState.UNSET;

    // === Integration ===
    private OptionState implementsBuilderBase = OptionState.UNSET;
    private OptionState generateWithInterface = OptionState.UNSET;
    private OptionState usingJacksonDeserializerAnnotation = OptionState.UNSET;
    private OptionState generateJacksonModule = OptionState.UNSET;
    private OptionState generateJavaDoc = OptionState.UNSET;
    private String jacksonModulePackage = null;

    // === Naming ===
    private String builderSuffix = null;
    private String setterSuffix = null;

    // === Formatting ===
    private String formattingMode = null;

    // === Error Handling ===
    private OptionState strict = OptionState.UNSET;

    // === Setters ===
    public Builder generateSupplier(OptionState value) {
      this.generateFieldSupplier = value;
      return this;
    }

    public Builder generateSupplier(boolean value) {
      this.generateFieldSupplier = value ? ENABLED : DISABLED;
      return this;
    }

    public Builder generateConsumer(OptionState value) {
      this.generateFieldConsumer = value;
      return this;
    }

    public Builder generateConsumer(boolean value) {
      this.generateFieldConsumer = value ? ENABLED : DISABLED;
      return this;
    }

    public Builder generateBuilderConsumer(OptionState value) {
      this.generateBuilderConsumer = value;
      return this;
    }

    public Builder generateBuilderConsumer(boolean value) {
      this.generateBuilderConsumer = value ? ENABLED : DISABLED;
      return this;
    }

    public Builder generateConditionalLogic(OptionState value) {
      this.generateConditionalHelper = value;
      return this;
    }

    public Builder generateConditionalLogic(boolean value) {
      this.generateConditionalHelper = value ? ENABLED : DISABLED;
      return this;
    }

    public Builder generateWithInterface(OptionState value) {
      this.generateWithInterface = value;
      return this;
    }

    public Builder generateWithInterface(boolean value) {
      this.generateWithInterface = value ? ENABLED : DISABLED;
      return this;
    }

    public Builder usingJacksonDeserializerAnnotation(OptionState value) {
      this.usingJacksonDeserializerAnnotation = value;
      return this;
    }

    public Builder usingJacksonDeserializerAnnotation(boolean value) {
      this.usingJacksonDeserializerAnnotation = value ? ENABLED : DISABLED;
      return this;
    }

    public Builder generateJacksonModule(OptionState value) {
      this.generateJacksonModule = value;
      return this;
    }

    public Builder generateJacksonModule(boolean value) {
      this.generateJacksonModule = value ? ENABLED : DISABLED;
      return this;
    }

    public Builder generateJavaDoc(OptionState value) {
      this.generateJavaDoc = value;
      return this;
    }

    public Builder generateJavaDoc(boolean value) {
      this.generateJavaDoc = value ? ENABLED : DISABLED;
      return this;
    }

    public Builder jacksonModulePackage(String value) {
      this.jacksonModulePackage = StringUtils.trimToNull(value);
      return this;
    }

    public Builder generateVarArgsHelpers(OptionState value) {
      this.generateVarArgsHelpers = value;
      return this;
    }

    public Builder generateVarArgsHelpers(boolean value) {
      this.generateVarArgsHelpers = value ? ENABLED : DISABLED;
      return this;
    }

    public Builder generateStringFormatHelpers(OptionState value) {
      this.generateStringFormatHelpers = value;
      return this;
    }

    public Builder generateStringFormatHelpers(boolean value) {
      this.generateStringFormatHelpers = value ? ENABLED : DISABLED;
      return this;
    }

    public Builder generateAddToCollectionHelpers(OptionState value) {
      this.generateAddToCollectionHelpers = value;
      return this;
    }

    public Builder generateAddToCollectionHelpers(boolean value) {
      this.generateAddToCollectionHelpers = value ? ENABLED : DISABLED;
      return this;
    }

    public Builder generateUnboxedOptional(OptionState value) {
      this.generateUnboxedOptional = value;
      return this;
    }

    public Builder generateUnboxedOptional(boolean value) {
      this.generateUnboxedOptional = value ? ENABLED : DISABLED;
      return this;
    }

    public Builder copyTypeAnnotations(OptionState value) {
      this.copyTypeAnnotations = value;
      return this;
    }

    public Builder copyTypeAnnotations(boolean value) {
      this.copyTypeAnnotations = value ? ENABLED : DISABLED;
      return this;
    }

    public Builder usingArrayListBuilder(OptionState value) {
      this.usingArrayListBuilder = value;
      return this;
    }

    public Builder usingArrayListBuilder(boolean value) {
      this.usingArrayListBuilder = value ? ENABLED : DISABLED;
      return this;
    }

    public Builder usingArrayListBuilderWithElementBuilders(OptionState value) {
      this.usingArrayListBuilderWithElementBuilders = value;
      return this;
    }

    public Builder usingArrayListBuilderWithElementBuilders(boolean value) {
      this.usingArrayListBuilderWithElementBuilders = value ? ENABLED : DISABLED;
      return this;
    }

    public Builder usingHashSetBuilder(OptionState value) {
      this.usingHashSetBuilder = value;
      return this;
    }

    public Builder usingHashSetBuilder(boolean value) {
      this.usingHashSetBuilder = value ? ENABLED : DISABLED;
      return this;
    }

    public Builder usingHashSetBuilderWithElementBuilders(OptionState value) {
      this.usingHashSetBuilderWithElementBuilders = value;
      return this;
    }

    public Builder usingHashSetBuilderWithElementBuilders(boolean value) {
      this.usingHashSetBuilderWithElementBuilders = value ? ENABLED : DISABLED;
      return this;
    }

    public Builder usingHashMapBuilder(OptionState value) {
      this.usingHashMapBuilder = value;
      return this;
    }

    public Builder usingHashMapBuilder(boolean value) {
      this.usingHashMapBuilder = value ? ENABLED : DISABLED;
      return this;
    }

    public Builder usingGeneratedAnnotation(OptionState value) {
      this.usingGeneratedAnnotation = value;
      return this;
    }

    public Builder usingGeneratedAnnotation(boolean value) {
      this.usingGeneratedAnnotation = value ? ENABLED : DISABLED;
      return this;
    }

    public Builder usingBuilderImplementationAnnotation(OptionState value) {
      this.usingBuilderImplementationAnnotation = value;
      return this;
    }

    public Builder usingBuilderImplementationAnnotation(boolean value) {
      this.usingBuilderImplementationAnnotation = value ? ENABLED : DISABLED;
      return this;
    }

    public Builder implementsBuilderBase(OptionState value) {
      this.implementsBuilderBase = value;
      return this;
    }

    public Builder implementsBuilderBase(boolean value) {
      this.implementsBuilderBase = value ? ENABLED : DISABLED;
      return this;
    }

    public Builder builderAccess(AccessModifier value) {
      this.builderAccess = value;
      return this;
    }

    public Builder builderAccess(String value) {
      this.builderAccess = AccessModifier.valueOf(value.toUpperCase());
      return this;
    }

    public Builder builderConstructorAccess(AccessModifier value) {
      this.builderConstructorAccess = value;
      return this;
    }

    public Builder builderConstructorAccess(String value) {
      this.builderConstructorAccess = AccessModifier.valueOf(value.toUpperCase());
      return this;
    }

    public Builder methodAccess(AccessModifier value) {
      this.methodAccess = value;
      return this;
    }

    public Builder methodAccess(String value) {
      this.methodAccess = AccessModifier.valueOf(value.toUpperCase());
      return this;
    }

    public Builder builderSuffix(String value) {
      this.builderSuffix = value == null ? null : value.trim();
      return this;
    }

    public Builder setterSuffix(String value) {
      this.setterSuffix = value == null ? null : value.trim();
      return this;
    }

    public Builder formattingMode(String value) {
      this.formattingMode = StringUtils.trimToNull(value);
      return this;
    }

    public Builder strict(OptionState value) {
      this.strict = value;
      return this;
    }

    public Builder strict(boolean value) {
      this.strict = value ? ENABLED : DISABLED;
      return this;
    }

    public BuilderConfiguration build() {
      return new BuilderConfiguration(
          generateFieldSupplier,
          generateFieldConsumer,
          generateBuilderConsumer,
          generateConditionalHelper,
          builderAccess,
          builderConstructorAccess,
          methodAccess,
          generateVarArgsHelpers,
          generateStringFormatHelpers,
          generateAddToCollectionHelpers,
          generateUnboxedOptional,
          copyTypeAnnotations,
          usingArrayListBuilder,
          usingArrayListBuilderWithElementBuilders,
          usingHashSetBuilder,
          usingHashSetBuilderWithElementBuilders,
          usingHashMapBuilder,
          usingGeneratedAnnotation,
          usingBuilderImplementationAnnotation,
          implementsBuilderBase,
          generateWithInterface,
          usingJacksonDeserializerAnnotation,
          generateJacksonModule,
          generateJavaDoc,
          jacksonModulePackage,
          builderSuffix,
          setterSuffix,
          formattingMode,
          strict);
    }
  }

  public static Builder builder() {
    return new Builder();
  }
}
