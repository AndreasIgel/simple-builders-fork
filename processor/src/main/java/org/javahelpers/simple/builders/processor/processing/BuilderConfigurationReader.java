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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import org.javahelpers.simple.builders.core.enums.AccessModifier;
import org.javahelpers.simple.builders.core.enums.OptionState;
import org.javahelpers.simple.builders.processor.exceptions.BuilderException;
import org.javahelpers.simple.builders.processor.model.core.BuilderConfiguration;
import org.javahelpers.simple.builders.processor.processing.logging.ProcessingLogger;

/**
 * Reads builder configuration from annotated elements.
 *
 * <p>Both {@code @SimpleBuilder} and custom annotations are treated as template annotations: an
 * annotation triggers builder generation when it is itself meta-annotated with
 * {@code @SimpleBuilder.Template}. {@code @SimpleBuilder} is the built-in template; its optional
 * {@code options()} attribute overrides the defaults declared on its own
 * {@code @SimpleBuilder.Template} meta-annotation. Custom template annotations carry their
 * configuration on the {@code @SimpleBuilder.Template} meta-annotation.
 *
 * <p>Priority order (highest to lowest):
 *
 * <ol>
 *   <li>Directly declared template annotations on the element (with {@code @SimpleBuilder} taking
 *       precedence over other direct templates in the same scope)
 *   <li>Inherited template annotations
 *   <li>Global compiler arguments
 *   <li>Built-in defaults
 * </ol>
 *
 * <p>Custom template annotations are annotations that are themselves meta-annotated with
 * {@code @SimpleBuilder.Template}; they are placed directly on the class or record. Direct
 * annotations always override inherited annotations.
 */
public class BuilderConfigurationReader {
  private static final String SIMPLE_BUILDER_ANNOTATION =
      "org.javahelpers.simple.builders.core.annotations.SimpleBuilder";
  private static final String SIMPLE_BUILDER_TEMPLATE_ANNOTATION =
      "org.javahelpers.simple.builders.core.annotations.SimpleBuilder.Template";
  private static final String SIMPLE_BUILDER_TEMPLATE_ANNOTATION_ALT =
      "org.javahelpers.simple.builders.core.annotations.SimpleBuilder$Template";

  private final BuilderConfiguration globalConfiguration;
  private final ProcessingLogger logger;
  private final Elements elementUtils;

  /**
   * Creates a new BuilderConfigurationReader.
   *
   * @param globalConfiguration the global configuration from compiler arguments
   * @param logger the logger for debug output
   * @param elementUtils the Elements utility for annotation processing
   */
  public BuilderConfigurationReader(
      BuilderConfiguration globalConfiguration, ProcessingLogger logger, Elements elementUtils) {
    this.globalConfiguration = globalConfiguration;
    this.logger = logger;
    this.elementUtils = elementUtils;
  }

  /**
   * Gets the global builder configuration read from compiler arguments.
   *
   * @return the global builder configuration
   */
  public BuilderConfiguration getGlobalConfiguration() {
    return globalConfiguration;
  }

  /**
   * Resolves the complete builder configuration for an element by chaining all configuration
   * sources in priority order.
   *
   * <p>Priority chain (highest to lowest):
   *
   * <ol>
   *   <li>Direct template annotations on the element
   *   <li>Inherited template annotations
   *   <li>Global compiler arguments
   *   <li>Built-in defaults
   * </ol>
   *
   * <p>When both {@code @SimpleBuilder} and another direct template annotation are present on the
   * same element, {@code @SimpleBuilder} takes precedence within that scope. A subclass's own
   * annotation always overrides inherited annotations.
   *
   * @param element the annotated element to resolve configuration for
   * @return the fully resolved configuration with all sources merged
   */
  public BuilderConfiguration resolveConfiguration(Element element) throws BuilderException {
    String elementName = element.getSimpleName().toString();
    logger.debugStartOperation("Resolving configuration for element: %s", elementName);

    BuilderConfiguration inheritedConfig = readFromScope(element, AnnotationScope.INHERITED);
    BuilderConfiguration directConfig = readFromScope(element, AnnotationScope.DIRECT);

    BuilderConfiguration result =
        BuilderConfiguration.DEFAULT
            .merge(globalConfiguration)
            .merge(inheritedConfig)
            .merge(directConfig);

    // Validate access modifiers and warn about problematic configurations
    validateAccessModifiers(element, result);

    logger.debugEndOperation("Resulting configuration resolved: %s", result.toString());
    return result;
  }

  /**
   * Reads the highest-priority template configuration for the element in the requested scope.
   *
   * <p>If {@code @SimpleBuilder} is present in the scope, its effective configuration (built-in
   * template defaults overridden by any inline {@code options()}) is returned. Otherwise the first
   * custom template annotation found in the scope is used.
   */
  private BuilderConfiguration readFromScope(Element element, AnnotationScope scope) {
    List<? extends AnnotationMirror> mirrors = getAnnotationMirrors(element, scope);
    BuilderConfiguration simpleBuilderConfig = null;
    BuilderConfiguration customTemplateConfig = null;

    for (AnnotationMirror mirror : mirrors) {
      if (isSimpleBuilderAnnotation(mirror)) {
        simpleBuilderConfig = extractSimpleBuilderConfiguration(mirror);
      } else if (customTemplateConfig == null) {
        BuilderConfiguration templateConfig = extractCustomTemplateConfiguration(mirror);
        if (templateConfig != null) {
          logger.debug(
              "Annotation based Configuration for scope %s: %s", scope, templateConfig.toString());
          customTemplateConfig = templateConfig;
        }
      }
    }

    if (simpleBuilderConfig != null) {
      logger.debug("Built-in template @SimpleBuilder found in %s scope", scope);
      return simpleBuilderConfig;
    }
    return customTemplateConfig;
  }

  /**
   * Extracts the effective configuration for a concrete {@code @SimpleBuilder} usage. The built-in
   * template defaults defined on the {@code @SimpleBuilder} annotation type are merged with the
   * inline {@code options()} from the usage, so inline options override the template defaults.
   */
  private BuilderConfiguration extractSimpleBuilderConfiguration(
      AnnotationMirror simpleBuilderMirror) {
    TypeElement simpleBuilderType =
        (TypeElement) simpleBuilderMirror.getAnnotationType().asElement();
    BuilderConfiguration templateDefaults = extractTemplateConfigurationFromType(simpleBuilderType);
    BuilderConfiguration inlineOptions = extractOptionsFromAnnotationMirror(simpleBuilderMirror);
    return mergeNullable(templateDefaults, inlineOptions);
  }

  /**
   * Extracts the configuration for a custom template annotation usage. The configuration is read
   * from the {@code @SimpleBuilder.Template} meta-annotation on the custom annotation type.
   */
  private BuilderConfiguration extractCustomTemplateConfiguration(
      AnnotationMirror annotationMirror) {
    TypeElement annotationType = (TypeElement) annotationMirror.getAnnotationType().asElement();
    return extractTemplateConfigurationFromType(annotationType);
  }

  /**
   * Extracts the template configuration declared on an annotation type by reading the {@code
   * options} attribute of its {@code @SimpleBuilder.Template} meta-annotation.
   *
   * @return the configuration, or {@code null} if the type is not a template annotation
   */
  private BuilderConfiguration extractTemplateConfigurationFromType(TypeElement annotationType) {
    AnnotationMirror templateMetaMirror = findTemplateMetaMirror(annotationType);
    if (templateMetaMirror == null) {
      return null;
    }
    return extractOptionsFromAnnotationMirror(templateMetaMirror);
  }

  /**
   * Finds the {@code @SimpleBuilder.Template} meta-annotation on an annotation type.
   *
   * @return the template meta-annotation mirror, or {@code null} if not present
   */
  private AnnotationMirror findTemplateMetaMirror(TypeElement annotationType) {
    for (AnnotationMirror metaMirror : annotationType.getAnnotationMirrors()) {
      if (isTemplateAnnotation(metaMirror)) {
        return metaMirror;
      }
    }
    return null;
  }

  /**
   * Extracts configuration from the 'options' attribute of an annotation mirror. Used for
   * {@code @SimpleBuilder(options = ...)} and for {@code @SimpleBuilder.Template(options = ...)}.
   *
   * @param annotationMirror the annotation mirror (either @SimpleBuilder or @Template)
   * @return the configuration extracted from the options attribute, or {@code null} if no options
   *     are set
   */
  private BuilderConfiguration extractOptionsFromAnnotationMirror(
      AnnotationMirror annotationMirror) {
    if (annotationMirror == null) {
      return null;
    }

    // Find the 'options' attribute, including default values. This is important for @SimpleBuilder,
    // whose options() attribute defaults to @Options() even when it is not explicitly specified.
    AnnotationMirror optionsMirror = null;
    Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues =
        elementUtils.getElementValuesWithDefaults(annotationMirror);

    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
        elementValues.entrySet()) {
      if (entry.getKey().getSimpleName().toString().equals("options")) {
        Object value = entry.getValue().getValue();
        if (value instanceof AnnotationMirror mirrorValue) {
          optionsMirror = mirrorValue;
        }
        break;
      }
    }

    if (optionsMirror == null) {
      // No options specified, return empty configuration
      return null;
    }

    // Parse the options annotation using AnnotationMirror (can't use reflection here)
    return parseOptionsFromMirror(optionsMirror);
  }

  /** Merges two nullable configurations, preferring the override when both are present. */
  private BuilderConfiguration mergeNullable(
      BuilderConfiguration base, BuilderConfiguration override) {
    if (base == null) {
      return override;
    }
    if (override == null) {
      return base;
    }
    return base.merge(override);
  }

  /**
   * Parses SimpleBuilder.Options from AnnotationMirror. Only contains explicitly set values (not
   * defaults).
   */
  private BuilderConfiguration parseOptionsFromMirror(AnnotationMirror optionsMirror) {
    Map<? extends ExecutableElement, ? extends AnnotationValue> values =
        optionsMirror.getElementValues();

    BuilderConfiguration.Builder builder = BuilderConfiguration.builder();

    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
        values.entrySet()) {
      String name = entry.getKey().getSimpleName().toString();
      Object value = entry.getValue().getValue();
      String enumValue = extractEnumName(value);

      switch (name) {
        case "generateFieldSupplier" -> builder.generateSupplier(OptionState.valueOf(enumValue));
        case "generateFieldConsumer" -> builder.generateConsumer(OptionState.valueOf(enumValue));
        case "generateBuilderConsumer" ->
            builder.generateBuilderConsumer(OptionState.valueOf(enumValue));
        case "generateConditionalHelper" ->
            builder.generateConditionalLogic(OptionState.valueOf(enumValue));
        case "builderAccess" -> builder.builderAccess(AccessModifier.valueOf(enumValue));
        case "builderConstructorAccess" ->
            builder.builderConstructorAccess(AccessModifier.valueOf(enumValue));
        case "methodAccess" -> builder.methodAccess(AccessModifier.valueOf(enumValue));
        case "generateVarArgsHelpers" ->
            builder.generateVarArgsHelpers(OptionState.valueOf(enumValue));
        case "generateStringFormatHelpers" ->
            builder.generateStringFormatHelpers(OptionState.valueOf(enumValue));
        case "generateAddToCollectionHelpers" ->
            builder.generateAddToCollectionHelpers(OptionState.valueOf(enumValue));
        case "generateUnboxedOptional" ->
            builder.generateUnboxedOptional(OptionState.valueOf(enumValue));
        case "copyTypeAnnotations" -> builder.copyTypeAnnotations(OptionState.valueOf(enumValue));
        case "usingArrayListBuilder" ->
            builder.usingArrayListBuilder(OptionState.valueOf(enumValue));
        case "usingArrayListBuilderWithElementBuilders" ->
            builder.usingArrayListBuilderWithElementBuilders(OptionState.valueOf(enumValue));
        case "usingHashSetBuilder" -> builder.usingHashSetBuilder(OptionState.valueOf(enumValue));
        case "usingHashSetBuilderWithElementBuilders" ->
            builder.usingHashSetBuilderWithElementBuilders(OptionState.valueOf(enumValue));
        case "usingHashMapBuilder" -> builder.usingHashMapBuilder(OptionState.valueOf(enumValue));
        case "usingGeneratedAnnotation" ->
            builder.usingGeneratedAnnotation(OptionState.valueOf(enumValue));
        case "usingBuilderImplementationAnnotation" ->
            builder.usingBuilderImplementationAnnotation(OptionState.valueOf(enumValue));
        case "implementsBuilderBase" ->
            builder.implementsBuilderBase(OptionState.valueOf(enumValue));
        case "generateWithInterface" ->
            builder.generateWithInterface(OptionState.valueOf(enumValue));
        case "usingJacksonDeserializerAnnotation" ->
            builder.usingJacksonDeserializerAnnotation(OptionState.valueOf(enumValue));
        case "generateJacksonModule" ->
            builder.generateJacksonModule(OptionState.valueOf(enumValue));
        case "generateJavaDoc" -> builder.generateJavaDoc(OptionState.valueOf(enumValue));
        case "jacksonModulePackage" -> builder.jacksonModulePackage(value.toString());
        case "builderSuffix" -> builder.builderSuffix(value.toString());
        case "setterSuffix" -> builder.setterSuffix(value.toString());
        default ->
            logger.warning(
                "Unknown configuration option '%s' with value '%s' - ignoring", name, value);
      }
    }

    return builder.build();
  }

  private String extractEnumName(Object value) {
    String enumString = value.toString();
    return enumString.contains(".")
        ? enumString.substring(enumString.lastIndexOf('.') + 1)
        : enumString;
  }

  private enum AnnotationScope {
    DIRECT,
    INHERITED
  }

  private List<? extends AnnotationMirror> getAnnotationMirrors(
      Element element, AnnotationScope scope) {
    if (scope == AnnotationScope.DIRECT) {
      return element.getAnnotationMirrors();
    }

    Set<String> directAnnotationTypes = new HashSet<>();
    for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
      directAnnotationTypes.add(mirror.getAnnotationType().toString());
    }

    List<AnnotationMirror> inheritedMirrors = new ArrayList<>();
    for (AnnotationMirror mirror : elementUtils.getAllAnnotationMirrors(element)) {
      if (!directAnnotationTypes.contains(mirror.getAnnotationType().toString())) {
        inheritedMirrors.add(mirror);
      }
    }
    return inheritedMirrors;
  }

  /**
   * Checks whether an annotation mirror represents @SimpleBuilder.
   *
   * @param mirror the annotation mirror to check
   * @return true if this is @SimpleBuilder
   */
  private boolean isSimpleBuilderAnnotation(AnnotationMirror mirror) {
    String typeName = mirror.getAnnotationType().toString();
    return typeName.equals(SIMPLE_BUILDER_ANNOTATION);
  }

  /**
   * Checks if an annotation mirror represents @SimpleBuilder.Template.
   *
   * @param metaMirror the meta-annotation mirror to check
   * @return true if this is @SimpleBuilder.Template
   */
  private boolean isTemplateAnnotation(AnnotationMirror metaMirror) {
    String metaAnnotationName = metaMirror.getAnnotationType().toString();
    // Check both possible representations of nested annotation
    return metaAnnotationName.equals(SIMPLE_BUILDER_TEMPLATE_ANNOTATION)
        || metaAnnotationName.equals(SIMPLE_BUILDER_TEMPLATE_ANNOTATION_ALT);
  }

  /**
   * Validates access modifier configurations and throws exception for invalid settings.
   *
   * @param element the element being processed
   * @param config the resolved configuration
   * @throws BuilderException if access modifiers are invalid
   */
  private static void validateAccessModifiers(Element element, BuilderConfiguration config)
      throws BuilderException {
    String elementName = element.getSimpleName().toString();

    // Fail on PRIVATE builder access (makes builder completely unusable and causes Java compilation
    // error)
    if (config.builderAccess() == AccessModifier.PRIVATE) {
      throw new BuilderException(
          element,
          "Builder for '%s' has builderAccess=PRIVATE which makes the builder class "
              + "completely inaccessible and unusable (Java does not allow private top-level classes). "
              + "Use PUBLIC or PACKAGE_PRIVATE instead. "
              + "Note: Only builderConstructorAccess=PRIVATE is useful (for enforcing factory methods).",
          elementName);
    }

    // Fail on PRIVATE method access (makes all builder methods unusable)
    if (config.methodAccess() == AccessModifier.PRIVATE) {
      throw new BuilderException(
          element,
          "Builder for '%s' has methodAccess=PRIVATE which makes all setter methods "
              + "inaccessible and the builder unusable. "
              + "Use PUBLIC or PACKAGE_PRIVATE instead.",
          elementName);
    }
  }
}
