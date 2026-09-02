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

package org.javahelpers.simple.builders.processor.model.type;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.javahelpers.simple.builders.processor.model.annotation.AnnotationDto;

/**
 * TypeName is the unambiguously definition of a type. Holding name of class and package. Could be
 * extended to be a type with generic parts.
 */
public class TypeName {
  /** Name of package. */
  private final String packageName;

  /** Name of class. */
  private final String className;

  /** Annotations on this type (TYPE_USE). */
  private final java.util.List<AnnotationDto> annotations = new java.util.ArrayList<>();

  /** Builder type for this type if it has @SimpleBuilder annotation. */
  private TypeName builderType;

  /** Whether this type has an empty constructor. */
  private boolean hasEmptyConstructor = false;

  /** Whether this type represents an enum. */
  private boolean enumType = false;

  /**
   * Constructor for TypeName.
   *
   * @param packageName name of package
   * @param className name of class, could not be null
   */
  public TypeName(String packageName, String className) {
    requireNonNull(className);
    this.packageName = packageName;
    this.className = className;
  }

  /**
   * Returns name of package
   *
   * @return package name of type {@code java.lang.String}
   */
  public String getPackageName() {
    return packageName;
  }

  /**
   * Returns name of class
   *
   * @return class name of type {@code java.lang.String}
   */
  public String getClassName() {
    return className;
  }

  /**
   * Returns the simple name with generic type arguments (if any), without package prefixes.
   *
   * <p>For a plain {@code TypeName} this is equivalent to {@link #getClassName()}. Subclasses like
   * {@link TypeNameGeneric} override this to include generic parameters, e.g. {@code List<String>}
   * instead of just {@code List}.
   *
   * @return simple name with generics, e.g. {@code String} or {@code List<String>}
   */
  public String getSimpleNameWithGenerics() {
    return getClassName();
  }

  /**
   * Returns the full qualified name (package + class name).
   *
   * @return full qualified name of type {@code java.lang.String}
   */
  public String getFullQualifiedName() {
    return packageName.isEmpty() ? className : packageName + "." + className;
  }

  /**
   * Returns the list of annotations on this type.
   *
   * @return list of annotations
   */
  public java.util.List<AnnotationDto> getAnnotations() {
    return annotations;
  }

  /**
   * Adds an annotation to this type.
   *
   * @param annotation the annotation to add
   */
  public void addAnnotation(AnnotationDto annotation) {
    this.annotations.add(annotation);
  }

  /**
   * Returns the builder type for this type if it has @SimpleBuilder annotation.
   *
   * @return optional builder type
   */
  public Optional<TypeName> getBuilderType() {
    return Optional.ofNullable(builderType);
  }

  /**
   * Sets the builder type for this type.
   *
   * @param builderType the builder type
   */
  public void setBuilderType(TypeName builderType) {
    this.builderType = builderType;
  }

  /**
   * Returns whether this type has an empty constructor.
   *
   * @return true if the type has an empty constructor
   */
  public boolean hasEmptyConstructor() {
    return hasEmptyConstructor;
  }

  /**
   * Sets whether this type has an empty constructor.
   *
   * @param hasEmptyConstructor true if the type has an empty constructor
   */
  public void setHasEmptyConstructor(boolean hasEmptyConstructor) {
    this.hasEmptyConstructor = hasEmptyConstructor;
  }

  /**
   * Returns whether this type represents an enum.
   *
   * @return true if this type is an enum
   */
  public boolean isEnumType() {
    return enumType;
  }

  /**
   * Sets whether this type represents an enum.
   *
   * @param enumType true if this type is an enum
   */
  public void setEnumType(boolean enumType) {
    this.enumType = enumType;
  }

  /**
   * Helper function to hold a specific inner type.Is empty if this is a class without generic parts
   * or a class has multiple generics.
   *
   * @return {@code java.util.Optional<TypeName>} if there is a single inner generic type, otherwise
   *     {@code java.util.Optional.EMPTY}
   */
  public Optional<TypeName> getInnerType() {
    return Optional.empty();
  }

  /**
   * Creates a new instance of {@code TypeName} from an existing {@code Class}.
   *
   * @param clazz existing class to retrieve package and class-name from
   * @return a new TypeName instance representing the given class
   */
  public static TypeName of(Class<?> clazz) {
    return new TypeName(clazz.getPackage().getName(), clazz.getSimpleName());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TypeName typeName = (TypeName) o;

    return new EqualsBuilder()
        .append(packageName, typeName.packageName)
        .append(className, typeName.className)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(packageName).append(className).toHashCode();
  }

  /**
   * Checks if this type has an annotation with the given fully qualified name.
   *
   * @param annotationFqn the fully qualified name of the annotation to check for
   * @return true if the type has the specified annotation
   */
  public boolean hasAnnotation(String annotationFqn) {
    if (annotationFqn == null || annotations.isEmpty()) {
      return false;
    }
    return annotations.stream()
        .anyMatch(
            annotation ->
                annotation.getAnnotationType() != null
                    && annotationFqn.equals(annotation.getAnnotationType().getFullQualifiedName()));
  }
}
