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

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.javahelpers.simple.builders.core.enums.AccessModifier;
import org.javahelpers.simple.builders.core.enums.FormattingMode;
import org.javahelpers.simple.builders.processor.model.annotation.AnnotationDto;
import org.javahelpers.simple.builders.processor.model.annotation.InterfaceName;
import org.javahelpers.simple.builders.processor.model.imports.ImportStatement;
import org.javahelpers.simple.builders.processor.model.imports.RegularImport;
import org.javahelpers.simple.builders.processor.model.imports.StaticImport;
import org.javahelpers.simple.builders.processor.model.javadoc.JavadocDto;
import org.javahelpers.simple.builders.processor.model.method.ConstructorDto;
import org.javahelpers.simple.builders.processor.model.method.MethodDto;
import org.javahelpers.simple.builders.processor.model.type.GenericParameterDto;
import org.javahelpers.simple.builders.processor.model.type.NestedTypeDto;
import org.javahelpers.simple.builders.processor.model.type.TypeName;
import org.javahelpers.simple.builders.processor.model.type.TypeNameArray;
import org.javahelpers.simple.builders.processor.model.type.TypeNameGeneric;

/**
 * Generic representation of a class to generate.
 *
 * <p>This base class contains all information needed by the code generator to render a class,
 * independent of whether it's a builder, Jackson module, or other generated class type.
 */
public class GenerationTargetClassDto {
  /** Generated class FQN. */
  private TypeName typeName;

  /** Class visibility. */
  private AccessModifier classAccessModifier;

  /** Superclass (e.g., SimpleModule), null if none. */
  private TypeName superType;

  /** Field declarations. */
  private final List<ClassFieldDto> classFields = new LinkedList<>();

  /** Constructor definitions. */
  private final List<ConstructorDto> constructors = new LinkedList<>();

  /** All methods (unresolved — conflict resolution in generator). */
  private final List<MethodDto> methods = new LinkedList<>();

  /** Generic parameters declared on the target DTO (e.g., {@code <T extends Number, U>}). */
  private final List<GenericParameterDto> generics = new LinkedList<>();

  /** Imports (both regular and static imports). */
  private final Set<ImportStatement> imports = new LinkedHashSet<>();

  /** Nested types (interfaces or classes) to be generated inside the generated class. */
  private final List<NestedTypeDto> nestedTypes = new LinkedList<>();

  /**
   * Class-level annotations to be added to the generated class.
   *
   * <p>Uses a Set to ensure annotation uniqueness and prevent duplicates.
   */
  private final Set<AnnotationDto> classAnnotations = new LinkedHashSet<>();

  /**
   * Interfaces to be implemented by the generated class.
   *
   * <p>Uses a Set to ensure interface uniqueness and prevent duplicates.
   */
  private final Set<InterfaceName> interfaces = new LinkedHashSet<>();

  /** Class-level JavaDoc for the generated class. */
  private JavadocDto classJavadoc;

  /** Formatting mode for this generated class (null = use global default). */
  private FormattingMode formattingMode;

  public TypeName getTypeName() {
    return typeName;
  }

  public void setTypeName(TypeName typeName) {
    this.typeName = typeName;
  }

  public AccessModifier getClassAccessModifier() {
    return classAccessModifier;
  }

  public void setClassAccessModifier(AccessModifier classAccessModifier) {
    this.classAccessModifier = classAccessModifier;
  }

  public TypeName getSuperType() {
    return superType;
  }

  public void setSuperType(TypeName superType) {
    this.superType = superType;
  }

  public List<ClassFieldDto> getClassFields() {
    return classFields;
  }

  public void addClassField(ClassFieldDto classField) {
    this.classFields.add(classField);
  }

  public List<ConstructorDto> getConstructors() {
    return constructors;
  }

  public void addConstructor(ConstructorDto constructor) {
    this.constructors.add(constructor);
  }

  public List<MethodDto> getMethods() {
    return methods;
  }

  public void addMethod(MethodDto method) {
    this.methods.add(method);
  }

  public Set<ImportStatement> getImports() {
    return imports;
  }

  public void addImport(ImportStatement importStatement) {
    this.imports.add(importStatement);
  }

  // Convenience methods for adding imports without creating DTOs

  /**
   * Adds a regular import (convenience method accepting Class).
   *
   * @param clazz the class to import
   */
  public void addImport(Class<?> clazz) {
    addImport(new RegularImport(TypeName.of(clazz)));
  }

  /**
   * Adds a regular import (convenience method accepting TypeName).
   *
   * @param typeName the type to import
   */
  public void addImport(TypeName typeName) {
    if (typeName instanceof TypeNameGeneric generic) {
      addImport(generic.getRawType());
      generic.getInnerTypeArguments().forEach(this::addImport);
    } else if (typeName instanceof TypeNameArray array) {
      addImport(array.getTypeOfArray());
    } else {
      addImport(new RegularImport(typeName));
    }
  }

  /**
   * Adds a static import (convenience method accepting Class and member name).
   *
   * @param clazz the class containing the static member
   * @param memberName the name of the static member
   */
  public void addStaticImport(Class<?> clazz, String memberName) {
    addImport(new StaticImport(TypeName.of(clazz), memberName));
  }

  /**
   * Adds a static import (convenience method accepting TypeName and member name).
   *
   * @param type the type containing the static member
   * @param memberName the name of the static member
   */
  public void addStaticImport(TypeName type, String memberName) {
    TypeName typeRaw = new TypeName(type.getPackageName(), type.getClassName());
    addImport(new StaticImport(typeRaw, memberName));
  }

  /**
   * Adds a generic parameter definition.
   *
   * @param generic the generic parameter to add
   */
  public void addGeneric(GenericParameterDto generic) {
    generics.add(generic);
  }

  /**
   * Returns the list of generic parameters of the target DTO, in declared order.
   *
   * @return the list of generic parameters
   */
  public List<GenericParameterDto> getGenerics() {
    return generics;
  }

  /**
   * Returns the list of nested types (interfaces or classes) to be generated inside the generated
   * class.
   *
   * @return the list of nested types
   */
  public List<NestedTypeDto> getNestedTypes() {
    return nestedTypes;
  }

  /**
   * Adds a nested type definition to the generated class.
   *
   * @param nestedType the nested type to add
   */
  public void addNestedType(NestedTypeDto nestedType) {
    this.nestedTypes.add(nestedType);
  }

  /**
   * Returns the set of class-level annotations.
   *
   * @return the set of class annotations (unique, no duplicates)
   */
  public Set<AnnotationDto> getClassAnnotations() {
    return classAnnotations;
  }

  /**
   * Adds a class-level annotation to be generated in the generated class.
   *
   * @param annotation the class annotation to add
   */
  public void addClassAnnotation(AnnotationDto annotation) {
    this.classAnnotations.add(annotation);
  }

  /**
   * Returns the set of interfaces to be implemented by the generated class.
   *
   * @return the set of interfaces (unique, no duplicates)
   */
  public Set<InterfaceName> getInterfaces() {
    return interfaces;
  }

  /**
   * Adds an interface to be implemented by the generated class.
   *
   * @param interfaceType the interface to add
   */
  public void addInterface(InterfaceName interfaceType) {
    this.interfaces.add(interfaceType);
  }

  /**
   * Returns the class-level JavaDoc for the generated class.
   *
   * @return the class JavaDoc, or null if not set
   */
  public JavadocDto getClassJavadoc() {
    return classJavadoc;
  }

  /**
   * Sets the class-level JavaDoc for the generated class.
   *
   * @param classJavadoc the class JavaDoc to set
   */
  public void setClassJavadoc(JavadocDto classJavadoc) {
    this.classJavadoc = classJavadoc;
  }

  /**
   * Returns the formatting mode for this generated class.
   *
   * @return the formatting mode, or null to use the global default
   */
  public FormattingMode getFormattingMode() {
    return formattingMode;
  }

  /**
   * Sets the formatting mode for this generated class.
   *
   * @param formattingMode the formatting mode, or null to use the global default
   */
  public void setFormattingMode(FormattingMode formattingMode) {
    this.formattingMode = formattingMode;
  }
}
