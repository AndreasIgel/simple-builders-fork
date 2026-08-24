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

package org.javahelpers.simple.builders.processor.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import org.javahelpers.simple.builders.processor.model.annotation.AnnotationDto;
import org.javahelpers.simple.builders.processor.model.type.TypeName;
import org.javahelpers.simple.builders.processor.model.type.TypeNamePrimitive;
import org.javahelpers.simple.builders.processor.processing.ProcessingContext;

/** Extractor for field annotations, converting them from Java model elements to DTOs. */
public final class FieldAnnotationExtractor {

  /** Fully qualified name of {@link Deprecated}. */
  private static final String DEPRECATED_FQN = "java.lang.Deprecated";

  /**
   * List of predicates that determine which annotations should be skipped (not copied to the
   * builder). Each predicate receives the fully qualified annotation name and returns true if the
   * annotation should be filtered out.
   *
   * <p>To add a new filter, simply add a new predicate to this list with a descriptive comment.
   */
  private static final java.util.List<java.util.function.Predicate<String>> ANNOTATION_FILTERS =
      java.util.List.of(
          // Skip SimpleBuilder framework annotations
          name -> name.startsWith("org.javahelpers.simple.builders."),
          // Skip code generation metadata annotations
          name -> name.equals("javax.annotation.Generated"),
          name -> name.equals("javax.annotation.processing.Generated"),
          // Skip compiler-only annotations not relevant for builder parameters
          name -> name.equals("java.lang.SuppressWarnings"),
          // Skip @Deprecated on parameters — deprecation is propagated to the builder method
          // itself via DeprecationInfoDto, not to the builder method's parameter (which is a
          // new declaration, not the original deprecated source parameter)
          name -> name.equals(DEPRECATED_FQN),
          // Skip @Valid annotation for cascading validation (jakarta.validation.Valid /
          // javax.validation.Valid) - only meaningful on fields or method return types, not on
          // builder method parameters where individual values are set
          name -> name.equals("jakarta.validation.Valid"),
          name -> name.equals("javax.validation.Valid"));

  private FieldAnnotationExtractor() {
    // Private constructor to prevent instantiation
  }

  /**
   * Extracts the {@code @Deprecated} annotation from the given element, preserving its members
   * (e.g. {@code since} and {@code forRemoval}).
   *
   * <p>Unlike {@link #extractAnnotations(VariableElement, ProcessingContext)}, this method targets
   * a single annotation and works on any {@link Element} (method, field, parameter, record
   * component, type), not just {@link VariableElement}s.
   *
   * @param element the element to inspect, or {@code null}
   * @param context processing context
   * @return an {@link Optional} containing the {@code @Deprecated} annotation DTO, or empty if the
   *     element is not deprecated
   */
  public static Optional<AnnotationDto> extractDeprecatedAnnotation(
      Element element, ProcessingContext context) {
    if (element == null) {
      return Optional.empty();
    }
    for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
      Element annotationElement = mirror.getAnnotationType().asElement();
      if (!(annotationElement instanceof TypeElement annotationType)) {
        continue;
      }
      if (DEPRECATED_FQN.equals(annotationType.getQualifiedName().toString())) {
        // Extract the @Deprecated annotation directly, WITHOUT applying ANNOTATION_FILTERS.
        // The filters are for annotations copied to builder parameters; deprecation detection
        // must always see @Deprecated regardless of whether it is copied to parameters.
        return extractAnnotationWithoutFiltering(mirror, context);
      }
    }
    return Optional.empty();
  }

  /**
   * Extracts annotations from a field parameter. Filters out annotations that should not be copied
   * to the builder.
   *
   * @param param the parameter element containing annotations
   * @param context processing context
   * @return list of annotations to be copied to the builder field
   */
  public static List<AnnotationDto> extractAnnotations(
      VariableElement param, ProcessingContext context) {
    List<AnnotationDto> annotations = new ArrayList<>();
    List<? extends AnnotationMirror> annotationMirrors = param.getAnnotationMirrors();

    for (AnnotationMirror mirror : annotationMirrors) {
      extractAnnotation(mirror, context).ifPresent(annotations::add);
    }

    return annotations;
  }

  /**
   * Extracts annotations from a type mirror. Filters out annotations that should not be copied to
   * the builder.
   *
   * @param typeMirror the type mirror containing annotations
   * @param context processing context
   * @return list of annotations to be copied to the builder field
   */
  public static List<AnnotationDto> extractAnnotations(
      javax.lang.model.type.TypeMirror typeMirror, ProcessingContext context) {
    List<AnnotationDto> annotations = new ArrayList<>();
    List<? extends AnnotationMirror> annotationMirrors = typeMirror.getAnnotationMirrors();

    for (AnnotationMirror mirror : annotationMirrors) {
      extractAnnotation(mirror, context).ifPresent(annotations::add);
    }

    return annotations;
  }

  /**
   * Extracts a single annotation from an AnnotationMirror. Filters out annotations that should not
   * be copied to the builder.
   *
   * @param mirror the annotation mirror to process
   * @param context processing context
   * @return Optional containing the extracted annotation, or empty if it should be skipped
   */
  private static Optional<AnnotationDto> extractAnnotation(
      AnnotationMirror mirror, ProcessingContext context) {
    // Get the annotation type element
    Element annotationElement = mirror.getAnnotationType().asElement();
    if (!(annotationElement instanceof TypeElement annotationType)) {
      return Optional.empty();
    }

    String annotationQualifiedName = annotationType.getQualifiedName().toString();

    // Filter out annotations that should not be copied to the builder
    if (shouldSkipAnnotation(annotationQualifiedName)) {
      context.debug("    -> Skipping annotation: %s", annotationQualifiedName);
      return Optional.empty();
    }

    return extractAnnotationWithoutFiltering(mirror, context);
  }

  /**
   * Extracts a single annotation from an AnnotationMirror without applying {@link
   * #shouldSkipAnnotation} filters. Used by {@link #extractDeprecatedAnnotation} which needs to
   * detect @Deprecated even though @Deprecated is filtered from parameter annotations.
   *
   * @param mirror the annotation mirror to process
   * @param context processing context
   * @return Optional containing the extracted annotation
   */
  private static Optional<AnnotationDto> extractAnnotationWithoutFiltering(
      AnnotationMirror mirror, ProcessingContext context) {
    Element annotationElement = mirror.getAnnotationType().asElement();
    if (!(annotationElement instanceof TypeElement annotationType)) {
      return Optional.empty();
    }

    // Create AnnotationDto
    AnnotationDto annotationDto = new AnnotationDto();

    // Extract package and class name
    String packageName = context.getPackageName(annotationType);
    String simpleName = annotationType.getSimpleName().toString();
    annotationDto.setAnnotationType(new TypeName(packageName, simpleName));

    // Extract annotation members (parameters)
    Map<? extends ExecutableElement, ? extends javax.lang.model.element.AnnotationValue>
        elementValues = mirror.getElementValues();
    for (Map.Entry<? extends ExecutableElement, ? extends javax.lang.model.element.AnnotationValue>
        entry : elementValues.entrySet()) {
      String memberName = entry.getKey().getSimpleName().toString();
      String memberValue = formatAnnotationValue(entry.getValue());
      annotationDto.addMember(memberName, memberValue);
    }

    context.debug("    -> Added annotation: %s", annotationType.getQualifiedName());
    return Optional.of(annotationDto);
  }

  /**
   * Formats an annotation value as a code string suitable for code generation. Handles different
   * types of annotation values including primitives, strings, enums, classes, arrays, and nested
   * annotations.
   *
   * @param value the annotation value to format
   * @return formatted code string suitable for source code
   */
  private static String formatAnnotationValue(javax.lang.model.element.AnnotationValue value) {
    Object actualValue = value.getValue();

    // Handle enum constants - need to fully qualify them
    if (actualValue instanceof VariableElement varElement
        && varElement.getKind() == ElementKind.ENUM_CONSTANT) {
      return formatEnumConstant(varElement);
    }

    // Handle class literals - already properly formatted by toString()
    if (actualValue instanceof DeclaredType) {
      return value.toString();
    }

    // Handle arrays - need to recursively format elements
    if (actualValue instanceof List<?> list) {
      return formatArray(list);
    }

    // For all other types (primitives, strings, nested annotations), toString() works correctly
    return value.toString();
  }

  /**
   * Formats an enum constant with its fully qualified name.
   *
   * @param varElement the variable element representing the enum constant
   * @return fully qualified enum constant name
   */
  private static String formatEnumConstant(VariableElement varElement) {
    Element enumClass = varElement.getEnclosingElement();
    if (enumClass instanceof TypeElement enumType) {
      return enumType.getQualifiedName() + "." + varElement.getSimpleName();
    }
    return varElement.getSimpleName().toString();
  }

  /**
   * Formats an array of annotation values.
   *
   * @param list the list of array elements
   * @return formatted array string
   */
  private static String formatArray(List<?> list) {
    if (list.isEmpty()) {
      return "{}";
    }
    StringBuilder sb = new StringBuilder("{");
    for (int i = 0; i < list.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      if (list.get(i) instanceof javax.lang.model.element.AnnotationValue annotValue) {
        sb.append(formatAnnotationValue(annotValue));
      } else {
        sb.append(list.get(i).toString());
      }
    }
    sb.append("}");
    return sb.toString();
  }

  /**
   * Determines if an annotation should be skipped (not copied to the builder).
   *
   * @param qualifiedName the fully qualified name of the annotation
   * @return true if the annotation should be skipped, false otherwise
   */
  private static boolean shouldSkipAnnotation(String qualifiedName) {
    return ANNOTATION_FILTERS.stream().anyMatch(filter -> filter.test(qualifiedName));
  }

  /**
   * Checks if a parameter has a non-null constraint annotation.
   *
   * <p>Detects annotations named "NotNull" or "NonNull" (case-sensitive) from any package, making
   * it framework-agnostic and future-proof.
   *
   * @param param the parameter element to check
   * @return true if the parameter has a non-null constraint annotation
   */
  public static boolean hasNonNullConstraint(VariableElement param) {
    return param.getAnnotationMirrors().stream()
        .map(am -> am.getAnnotationType().asElement())
        .filter(TypeElement.class::isInstance)
        .map(el -> ((TypeElement) el).getSimpleName().toString())
        .anyMatch(FieldAnnotationExtractor::isNonNullAnnotation);
  }

  /**
   * Checks if the given annotation simple name represents a non-null constraint.
   *
   * @param simpleName the simple name of the annotation (e.g., "NotNull", "NonNull")
   * @return true if it's a recognized non-null annotation name
   */
  private static boolean isNonNullAnnotation(String simpleName) {
    return "NotNull".equals(simpleName) || "NonNull".equals(simpleName);
  }

  /**
   * Extracts the {@code value()} member from any annotation on the given element whose simple name
   * matches one of the provided names, regardless of package.
   *
   * <p>For example, if {@code annotationNames} contains {@code "Default"} and {@code
   * "DefaultValue"}, this will detect annotations from any package (e.g. {@code
   * org.javahelpers.simple.builders.core.annotations.Default}, {@code jakarta.ws.rs.DefaultValue})
   * as long as they have a {@code String value()} member.
   *
   * @param element the element to check for annotations
   * @param annotationNames the set of annotation simple names to look for
   * @return an {@link Optional} containing the raw value string, or empty if no matching annotation
   *     with a {@code value()} member is present
   */
  public static Optional<String> extractAnnotationValue(
      Element element, Set<String> annotationNames) {
    return element.getAnnotationMirrors().stream()
        .filter(mirror -> isAnnotationWithName(mirror, annotationNames))
        .map(FieldAnnotationExtractor::getValueMember)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }

  /**
   * Checks if the given annotation mirror's simple name matches one of the provided names.
   *
   * @param mirror the annotation mirror to check
   * @param annotationNames the set of annotation simple names to match against
   * @return {@code true} if the annotation's simple name is in the provided set
   */
  private static boolean isAnnotationWithName(
      AnnotationMirror mirror, Set<String> annotationNames) {
    if (!(mirror.getAnnotationType().asElement() instanceof TypeElement type)) {
      return false;
    }
    return annotationNames.contains(type.getSimpleName().toString());
  }

  /**
   * Extracts the {@code value()} member from an annotation mirror as a string.
   *
   * @param mirror the annotation mirror to extract from
   * @return an {@link Optional} containing the value string, or empty if no {@code value()} member
   *     is present
   */
  private static Optional<String> getValueMember(AnnotationMirror mirror) {
    return mirror.getElementValues().entrySet().stream()
        .filter(e -> "value".equals(e.getKey().getSimpleName().toString()))
        .map(e -> e.getValue().getValue().toString())
        .findFirst();
  }

  /**
   * Formats a raw string default value as a Java expression based on the field type.
   *
   * <p>Interpretation rules:
   *
   * <ul>
   *   <li>{@code String} — wrapped in double quotes, e.g. {@code "GENERAL"}
   *   <li>{@code char} — wrapped in single quotes, e.g. {@code 'A'}
   *   <li>numeric/boolean primitives — used as-is, e.g. {@code 0.0}, {@code true}
   *   <li>enum types — a simple constant name is qualified with the enum class name, e.g. {@code
   *       GOOD} becomes {@code ItemCondition.GOOD}; already-qualified names or complex expressions
   *       are left as-is
   *   <li>complex types (List, custom objects) — used as a raw Java expression, e.g. {@code
   *       List.of()}
   * </ul>
   *
   * @param rawValue the raw string from the annotation's {@code value()} member
   * @param fieldType the {@link TypeName} of the target field
   * @return a formatted Java expression string suitable for code generation
   */
  public static String formatDefaultExpression(String rawValue, TypeName fieldType) {
    if (fieldType.equals(TypeName.of(String.class))) {
      return "\"%s\"".formatted(rawValue);
    }
    if (fieldType.equals(TypeNamePrimitive.CHAR)) {
      return "'%s'".formatted(rawValue);
    }
    if (fieldType.isEnumType() && isSimpleIdentifier(rawValue)) {
      return fieldType.getClassName() + "." + rawValue;
    }
    return rawValue;
  }

  /**
   * Checks whether the given raw value is a simple Java identifier (e.g. an enum constant name like
   * {@code GOOD}).
   *
   * <p>Values containing dots (e.g. {@code ItemCondition.GOOD}), parentheses (e.g. {@code new
   * IntegerDto(12)}), spaces, or other non-identifier characters are not simple identifiers and
   * will not be auto-qualified.
   *
   * @param rawValue the raw string to check
   * @return {@code true} if the value is a simple Java identifier
   */
  private static boolean isSimpleIdentifier(String rawValue) {
    return SourceVersion.isIdentifier(rawValue);
  }
}
