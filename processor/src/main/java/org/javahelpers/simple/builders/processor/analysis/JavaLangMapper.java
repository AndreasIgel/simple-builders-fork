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

import static javax.lang.model.element.Modifier.DEFAULT;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.type.TypeKind.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.SimpleTypeVisitor14;
import org.javahelpers.simple.builders.core.enums.AccessModifier;
import org.javahelpers.simple.builders.processor.model.annotation.AnnotationDto;
import org.javahelpers.simple.builders.processor.model.method.MethodParameterDto;
import org.javahelpers.simple.builders.processor.model.type.GenericParameterDto;
import org.javahelpers.simple.builders.processor.model.type.TypeName;
import org.javahelpers.simple.builders.processor.model.type.TypeNameArray;
import org.javahelpers.simple.builders.processor.model.type.TypeNameGeneric;
import org.javahelpers.simple.builders.processor.model.type.TypeNameList;
import org.javahelpers.simple.builders.processor.model.type.TypeNameMap;
import org.javahelpers.simple.builders.processor.model.type.TypeNamePrimitive;
import org.javahelpers.simple.builders.processor.model.type.TypeNameSet;
import org.javahelpers.simple.builders.processor.model.type.TypeNameVariable;
import org.javahelpers.simple.builders.processor.processing.ProcessingContext;

/** Helper functions to create simple builder types from java.lang types. */
public final class JavaLangMapper {
  /** Private constructor to prevent instantiation of utility class. */
  private JavaLangMapper() {
    // Utility class
  }

  /**
   * Mapper for {@code java.util.Set<javax.lang.model.element.Modifier>} to extract the relevant
   * modifier. If there is a public modifier, this is returned. If there is a protected modifier,
   * this will be returned. Default is returned in all other cases.
   *
   * @param modifier Set of modifiers to be checked
   * @return DEFAULT, PUBLIC or PROTECTED
   */
  public static Modifier mapRelevantModifier(Set<Modifier> modifier) {
    if (modifier.contains(PUBLIC)) {
      return PUBLIC;
    } else if (modifier.contains(PROTECTED)) {
      return PROTECTED;
    }
    return DEFAULT;
  }

  /**
   * Maps an AccessModifier enum value to a javax.lang.model.element.Modifier.
   *
   * @param accessModifier the access modifier to map
   * @return the corresponding Modifier, or null for package-private
   */
  public static Modifier mapAccessModifier(AccessModifier accessModifier) {
    if (accessModifier == null) {
      return null;
    }
    return switch (accessModifier) {
      case PUBLIC, DEFAULT -> Modifier.PUBLIC;
      case PROTECTED -> Modifier.PROTECTED;
      case PRIVATE -> Modifier.PRIVATE;
      case PACKAGE_PRIVATE -> null; // Package-private has no explicit modifier
    };
  }

  /**
   * Mapping a Java-Class to a TypeName. This method does not expact sealed or annonymous classes.
   *
   * @param clazz Class to be mapped
   * @return Typename
   */
  public static TypeName map2TypeName(Class<?> clazz) {
    return new TypeName(clazz.getPackageName(), clazz.getSimpleName());
  }

  /**
   * Maps the declared type parameters of the given type element into GenericParameterDto list.
   *
   * @param type the type element whose type parameters to map
   * @param context the processing context
   * @return list of GenericParameterDto representing the type parameters
   */
  public static List<GenericParameterDto> map2GenericParameterDtos(
      TypeElement type, ProcessingContext context) {
    return type.getTypeParameters().stream()
        .map(tp -> map2GenericParameterDto(tp, context))
        .toList();
  }

  /**
   * Maps a single {@code TypeParameterElement} to {@code GenericParameterDto}.
   *
   * @param tp the type parameter element to map
   * @param context the processing context
   * @return a GenericParameterDto representing the type parameter
   */
  public static GenericParameterDto map2GenericParameterDto(
      TypeParameterElement tp, ProcessingContext context) {
    GenericParameterDto g = new GenericParameterDto();
    g.setName(tp.getSimpleName().toString());
    tp.getBounds().stream()
        .filter(b -> !"java.lang.Object".equals(b.toString()))
        .map(b -> extractType(b, context))
        .forEach(g::addUpperBound);
    return g;
  }

  /**
   * Mapping a method parameter to {@code
   * org.javahelpers.simple.builders.processor.dtos.MethodParameterDto}.
   *
   * @param param Parameter variable to be mapped
   * @param context processing context
   * @return MethodParameterDto holding the information of the param.
   */
  public static MethodParameterDto map2MethodParameter(
      VariableElement param, ProcessingContext context) {
    MethodParameterDto result = new MethodParameterDto();
    result.setParameterName(param.getSimpleName().toString());
    TypeMirror typeMirror = param.asType();
    TypeName typeName = extractType(typeMirror, context);
    if (typeName == null) {
      return null;
    }

    // Set builder and constructor information on the TypeName (only if not already set)
    if (!typeName.getBuilderType().isPresent()) {
      setBuilderAndConstructorInfo(typeName, param, context);
    }

    result.setParameterTypeName(typeName);
    return result;
  }

  /**
   * Maps a {@code TypeElement} to a simple-builder {@code TypeName}.
   *
   * @param typeElement the TypeElement to map
   * @param context the processing context
   * @return TypeName holding the information of the type element, or null if mapping fails
   */
  public static TypeName map2TypeName(TypeElement typeElement, ProcessingContext context) {
    if (typeElement == null) {
      return null;
    }

    TypeMirror typeMirror = typeElement.asType();
    TypeName typeName = extractType(typeMirror, context);

    // Set builder and constructor information on the TypeName (only if not already set)
    if (typeName != null && !typeName.getBuilderType().isPresent()) {
      setBuilderAndConstructorInfo(typeName, typeElement, context);
    }

    return typeName;
  }

  /**
   * Sets builder and constructor information on the TypeName.
   *
   * @param typeName the TypeName to enhance with builder/constructor info
   * @param typeElement the TypeElement representing the type
   * @param context the processing context
   */
  private static void setBuilderAndConstructorInfo(
      TypeName typeName, TypeElement typeElement, ProcessingContext context) {
    setBuilderTypeIfAnnotated(typeName, typeElement, context);
    setEmptyConstructorInfoIfAvailable(typeName, typeElement, context);
    setElementBuilderTypeForGenericCollections(typeName, context);
  }

  /**
   * Sets the builder type if the type element has @SimpleBuilder annotation.
   *
   * @param typeName the TypeName to enhance
   * @param typeElement the type element to check
   * @param context the processing context
   */
  private static void setBuilderTypeIfAnnotated(
      TypeName typeName, TypeElement typeElement, ProcessingContext context) {
    context
        .getBuilderScopeResolver()
        .resolveUsableBuilderType(typeElement, context)
        .ifPresent(typeName::setBuilderType);
  }

  /**
   * Sets empty constructor info for concrete classes with empty constructors.
   *
   * @param typeName the TypeName to enhance
   * @param typeElement the type element to check
   * @param context the processing context
   */
  private static void setEmptyConstructorInfoIfAvailable(
      TypeName typeName, TypeElement typeElement, ProcessingContext context) {
    if (isConcreteClass(typeElement)
        && !TypeNameAnalyser.isJavaClass(typeName)
        && JavaLangAnalyser.hasEmptyConstructor(typeElement, context)) {
      typeName.setHasEmptyConstructor(true);
    }
  }

  /**
   * Sets element builder type for generic collections with @SimpleBuilder annotated elements.
   *
   * @param typeName the TypeName to enhance
   * @param context the processing context
   */
  private static void setElementBuilderTypeForGenericCollections(
      TypeName typeName, ProcessingContext context) {
    // Only process generic types
    if (!(typeName instanceof TypeNameGeneric genericType)) {
      return;
    }

    // Only process single-element generics (List<T>, Set<T>, etc.)
    List<TypeName> innerTypeArguments = genericType.getInnerTypeArguments();
    if (innerTypeArguments.size() != 1) {
      return;
    }

    TypeName elementType = innerTypeArguments.get(0);
    TypeElement elementTypeElement = retrieveTypeElementIfExists(elementType, context);

    // Element type must be resolvable
    if (elementTypeElement == null) {
      return;
    }

    // Resolve usable element builder type through the scope resolver
    context
        .getBuilderScopeResolver()
        .resolveUsableBuilderType(elementTypeElement, context)
        .ifPresent(genericType::setElementBuilderType);
  }

  /**
   * Gets the TypeElement for a given TypeName if it exists.
   *
   * @param typeName the type name to resolve
   * @param context the processing context
   * @return the TypeElement, or null if not found or not a TypeElement
   */
  private static TypeElement retrieveTypeElementIfExists(
      TypeName typeName, ProcessingContext context) {
    Element element = context.getTypeElement(typeName.getFullQualifiedName());
    return element instanceof TypeElement typeElement ? typeElement : null;
  }

  /**
   * Creates a TypeName for the builder of a given TypeElement.
   *
   * @param typeElement the type element to create builder name for
   * @param context the processing context
   * @return the TypeName for the builder
   */
  static TypeName createBuilderTypeName(TypeElement typeElement, ProcessingContext context) {
    String builderClassName =
        typeElement.getSimpleName().toString() + context.getConfiguration().getBuilderSuffix();
    String packageName = extractPackageName(typeElement.getQualifiedName().toString());
    return new TypeName(packageName, builderClassName);
  }

  /**
   * Extracts the package name from a qualified class name.
   *
   * @param qualifiedName the fully qualified class name (e.g., "com.example.MyClass")
   * @return the package name (e.g., "com.example"), or empty string if no package
   */
  static String extractPackageName(String qualifiedName) {
    int lastDot = qualifiedName.lastIndexOf('.');
    return lastDot > 0 ? qualifiedName.substring(0, lastDot) : "";
  }

  /**
   * Sets builder and constructor information on the TypeName.
   *
   * @param typeName the TypeName to enhance with builder/constructor info
   * @param param the VariableElement representing the parameter
   * @param context the processing context
   */
  private static void setBuilderAndConstructorInfo(
      TypeName typeName, VariableElement param, ProcessingContext context) {
    Element element = context.asElement(param.asType());
    if (element instanceof TypeElement typeElement) {
      setBuilderAndConstructorInfo(typeName, typeElement, context);
    }
  }

  /**
   * Maps a list of {@code TypeMirror} to a list of simple-builder {@code TypeName}s using {@link
   * #extractType(TypeMirror, ProcessingContext)}.
   */
  private static List<TypeName> extractTypeForList(
      List<TypeMirror> typeMirrors, ProcessingContext context) {
    List<TypeName> result = new ArrayList<>(typeMirrors.size());
    for (TypeMirror tm : typeMirrors) {
      result.add(extractType(tm, context));
    }
    return result;
  }

  /**
   * Checks if the given type implements List, Set, or Map interfaces and wraps it in the
   * appropriate specialized TypeName class.
   *
   * <p>This method correctly extracts the type arguments used by the collection interface, not the
   * class's declared type parameters. For example, {@code CustomList<X,Y> extends ArrayList<Y>}
   * will extract Y as the List element type, not both X and Y.
   *
   * @param rawType the base TypeName (preserves concrete class information)
   * @param argTypes the generic type arguments from the class declaration (may not match interface)
   * @param typeMirror the TypeMirror to check for interface implementation
   * @param context the processing context
   * @return a specialized TypeName (TypeNameList, TypeNameSet, TypeNameMap) if applicable, or a
   *     TypeNameGeneric/rawType otherwise
   */
  private static TypeName wrapInCollectionTypeIfApplicable(
      TypeName rawType, List<TypeName> argTypes, TypeMirror typeMirror, ProcessingContext context) {
    TypeName listWrapper = tryWrapAsList(rawType, argTypes, typeMirror, context);
    if (listWrapper != null) {
      return listWrapper;
    }

    TypeName setWrapper = tryWrapAsSet(rawType, argTypes, typeMirror, context);
    if (setWrapper != null) {
      return setWrapper;
    }

    TypeName mapWrapper = tryWrapAsMap(rawType, argTypes, typeMirror, context);
    if (mapWrapper != null) {
      return mapWrapper;
    }

    // Not a collection type - return generic or raw type
    return createFallbackTypeName(rawType, argTypes);
  }

  /**
   * Attempts to wrap the type as a TypeNameList if it implements List and has appropriate
   * constructor.
   *
   * @return TypeNameList if applicable, null otherwise
   */
  private static TypeName tryWrapAsList(
      TypeName rawType, List<TypeName> argTypes, TypeMirror typeMirror, ProcessingContext context) {
    TypeElement listElement = context.getTypeElement("java.util.List");
    if (listElement == null) {
      return null;
    }

    TypeMirror listType = context.erasure(listElement.asType());
    if (!context.isAssignable(context.erasure(typeMirror), listType)) {
      return null;
    }

    TypeElement typeElement = (TypeElement) context.asElement(typeMirror);
    if (!shouldWrapAsCollectionType(typeElement, listElement, context)) {
      return createFallbackTypeName(rawType, argTypes);
    }

    List<TypeName> interfaceTypeArgs =
        extractInterfaceTypeArguments(typeMirror, listElement, context);
    TypeName elementType = interfaceTypeArgs.isEmpty() ? null : interfaceTypeArgs.get(0);
    return new TypeNameList(rawType, argTypes, elementType);
  }

  /**
   * Attempts to wrap the type as a TypeNameSet if it implements Set and has appropriate
   * constructor.
   *
   * @return TypeNameSet if applicable, null otherwise
   */
  private static TypeName tryWrapAsSet(
      TypeName rawType, List<TypeName> argTypes, TypeMirror typeMirror, ProcessingContext context) {
    TypeElement setElement = context.getTypeElement("java.util.Set");
    if (setElement == null) {
      return null;
    }

    TypeMirror setType = context.erasure(setElement.asType());
    if (!context.isAssignable(context.erasure(typeMirror), setType)) {
      return null;
    }

    TypeElement typeElement = (TypeElement) context.asElement(typeMirror);
    if (!shouldWrapAsCollectionType(typeElement, setElement, context)) {
      return createFallbackTypeName(rawType, argTypes);
    }

    List<TypeName> interfaceTypeArgs =
        extractInterfaceTypeArguments(typeMirror, setElement, context);
    TypeName elementType = interfaceTypeArgs.isEmpty() ? null : interfaceTypeArgs.get(0);
    return new TypeNameSet(rawType, argTypes, elementType);
  }

  /**
   * Attempts to wrap the type as a TypeNameMap if it implements Map and has appropriate
   * constructor.
   *
   * @return TypeNameMap if applicable, null otherwise
   */
  private static TypeName tryWrapAsMap(
      TypeName rawType, List<TypeName> argTypes, TypeMirror typeMirror, ProcessingContext context) {
    TypeElement mapElement = context.getTypeElement("java.util.Map");
    if (mapElement == null) {
      return null;
    }

    TypeMirror mapType = context.erasure(mapElement.asType());
    if (!context.isAssignable(context.erasure(typeMirror), mapType)) {
      return null;
    }

    TypeElement typeElement = (TypeElement) context.asElement(typeMirror);
    boolean isInterface = typeElement.equals(mapElement);
    boolean hasConstructor =
        !isInterface && hasConstructorWithParameterOfType(typeElement, "java.util.Map", context);

    if (!isInterface && !hasConstructor) {
      return createFallbackTypeName(rawType, argTypes);
    }

    List<TypeName> interfaceTypeArgs =
        extractInterfaceTypeArguments(typeMirror, mapElement, context);
    TypeName keyType = interfaceTypeArgs.isEmpty() ? null : interfaceTypeArgs.get(0);
    TypeName valueType = interfaceTypeArgs.size() < 2 ? null : interfaceTypeArgs.get(1);
    return new TypeNameMap(rawType, argTypes, keyType, valueType);
  }

  /**
   * Checks if a type should be wrapped as a specialized collection type (List or Set).
   *
   * @param typeElement the type to check
   * @param interfaceElement the collection interface (List or Set)
   * @param context the processing context
   * @return true if the type is the interface itself or has a Collection constructor
   */
  private static boolean shouldWrapAsCollectionType(
      TypeElement typeElement, TypeElement interfaceElement, ProcessingContext context) {
    boolean isInterface = typeElement.equals(interfaceElement);
    boolean hasConstructor =
        !isInterface
            && hasConstructorWithParameterOfType(typeElement, "java.util.Collection", context);
    return isInterface || hasConstructor;
  }

  /**
   * Creates a fallback TypeName when a type cannot be wrapped as a specialized collection type.
   *
   * @param rawType the raw type
   * @param argTypes the type arguments
   * @return TypeNameGeneric if argTypes is not empty, otherwise rawType
   */
  private static TypeName createFallbackTypeName(TypeName rawType, List<TypeName> argTypes) {
    return argTypes.isEmpty() ? rawType : new TypeNameGeneric(rawType, argTypes);
  }

  /**
   * Checks if a type has a constructor that accepts a parameter of the specified type.
   *
   * <p>This allows us to detect if we can generate helper methods for collection/map types by
   * checking if it has a constructor like {@code ArrayList(Collection<? extends E>)} or {@code
   * HashMap(Map<? extends K, ? extends V>)}.
   *
   * @param typeElement the type to check
   * @param parameterTypeName the fully qualified name of the parameter type (e.g.,
   *     "java.util.Collection")
   * @param context the processing context
   * @return true if the type has a constructor accepting the specified parameter type
   */
  private static boolean hasConstructorWithParameterOfType(
      TypeElement typeElement, String parameterTypeName, ProcessingContext context) {
    TypeElement parameterElement = context.getTypeElement(parameterTypeName);
    if (parameterElement == null) {
      return false;
    }

    TypeMirror parameterType = context.erasure(parameterElement.asType());

    return typeElement.getEnclosedElements().stream()
        .filter(element -> element.getKind() == javax.lang.model.element.ElementKind.CONSTRUCTOR)
        .map(element -> (javax.lang.model.element.ExecutableElement) element)
        .anyMatch(
            constructor -> {
              if (constructor.getParameters().size() != 1) {
                return false;
              }
              TypeMirror constructorParamType = constructor.getParameters().get(0).asType();
              TypeMirror erasedConstructorParamType = context.erasure(constructorParamType);
              // Parameter should match the specified type or be assignable to it
              return context.isSameType(erasedConstructorParamType, parameterType)
                  || context.isAssignable(erasedConstructorParamType, parameterType);
            });
  }

  /**
   * Extracts the type arguments used by a specific interface from a type's supertype hierarchy.
   *
   * <p>For example, given {@code CustomList<X,Y> extends ArrayList<Y>} and the {@code List}
   * interface, this returns [Y], not [X,Y].
   *
   * @param typeMirror the type to examine
   * @param targetInterface the interface element (e.g., List, Set, Map)
   * @param context the processing context
   * @return list of type arguments used by the interface, or empty list if raw type
   */
  public static List<TypeName> extractInterfaceTypeArguments(
      TypeMirror typeMirror, TypeElement targetInterface, ProcessingContext context) {
    // Walk the supertype hierarchy to find the specific instantiation of the target interface
    TypeMirror found = findSupertype(typeMirror, targetInterface, context);

    if (found instanceof DeclaredType declaredType) {
      List<? extends TypeMirror> typeArgs = declaredType.getTypeArguments();
      if (!typeArgs.isEmpty()) {
        return extractTypeForList(new ArrayList<>(typeArgs), context);
      }
    }

    return List.of(); // Raw type
  }

  /**
   * Finds the specific supertype that matches the target interface in the type hierarchy.
   *
   * @param typeMirror the type to search from
   * @param targetInterface the interface to find
   * @param context the processing context
   * @return the matching supertype, or null if not found
   */
  private static TypeMirror findSupertype(
      TypeMirror typeMirror, TypeElement targetInterface, ProcessingContext context) {
    if (!(typeMirror instanceof DeclaredType)) {
      return null;
    }

    TypeElement typeElement = (TypeElement) context.asElement(typeMirror);
    if (typeElement.equals(targetInterface)) {
      return typeMirror;
    }

    // Check direct supertypes (superclass and interfaces)
    for (TypeMirror supertype : context.directSupertypes(typeMirror)) {
      TypeMirror found = findSupertype(supertype, targetInterface, context);
      if (found != null) {
        return found;
      }
    }

    return null;
  }

  private static TypeName extractType(TypeMirror typeOfParameter, ProcessingContext context) {
    TypeName typeName =
        typeOfParameter.accept(
            new SimpleTypeVisitor14<TypeName, Void>() {

              @Override
              public TypeName visitPrimitive(PrimitiveType t, Void p) {
                return switch (t.getKind()) {
                  case BOOLEAN ->
                      new TypeNamePrimitive(TypeNamePrimitive.PrimitiveTypeEnum.BOOLEAN);
                  case BYTE -> new TypeNamePrimitive(TypeNamePrimitive.PrimitiveTypeEnum.BYTE);
                  case SHORT -> new TypeNamePrimitive(TypeNamePrimitive.PrimitiveTypeEnum.SHORT);
                  case INT -> new TypeNamePrimitive(TypeNamePrimitive.PrimitiveTypeEnum.INT);
                  case LONG -> new TypeNamePrimitive(TypeNamePrimitive.PrimitiveTypeEnum.LONG);
                  case CHAR -> new TypeNamePrimitive(TypeNamePrimitive.PrimitiveTypeEnum.CHAR);
                  case FLOAT -> new TypeNamePrimitive(TypeNamePrimitive.PrimitiveTypeEnum.FLOAT);
                  case DOUBLE -> new TypeNamePrimitive(TypeNamePrimitive.PrimitiveTypeEnum.DOUBLE);
                  default -> throw new IllegalStateException("Unsupported Primitive type");
                };
              }

              @Override
              public TypeName visitDeclared(DeclaredType t, Void p) {
                TypeElement elementOfParameter = (TypeElement) context.asElement(typeOfParameter);
                String simpleClassName = elementOfParameter.getSimpleName().toString();
                String packageName = context.getPackageName(elementOfParameter);
                TypeName rawType = new TypeName(packageName, simpleClassName);
                if (elementOfParameter.getKind() == ElementKind.ENUM) {
                  rawType.setEnumType(true);
                }
                TypeMirror enclosingType = t.getEnclosingType();
                TypeName enclosing =
                    (enclosingType.getKind() != NONE)
                            && !t.asElement().getModifiers().contains(Modifier.STATIC)
                        ? enclosingType.accept(this, null)
                        : null;
                if (t.getTypeArguments().isEmpty() && !(enclosing instanceof TypeNameGeneric)) {
                  return wrapInCollectionTypeIfApplicable(
                      rawType, List.of(), typeOfParameter, context);
                }

                List<TypeMirror> typesExtracted = new ArrayList<>(t.getTypeArguments());
                if (typesExtracted.isEmpty()) {
                  return wrapInCollectionTypeIfApplicable(
                      rawType, List.of(), typeOfParameter, context);
                } else {
                  List<TypeName> argTypes = extractTypeForList(typesExtracted, context);
                  return wrapInCollectionTypeIfApplicable(
                      rawType, argTypes, typeOfParameter, context);
                }
              }

              @Override
              public TypeNameArray visitArray(ArrayType t, Void p) {
                return new TypeNameArray(extractType(t.getComponentType(), context));
              }

              @Override
              public TypeName visitTypeVariable(TypeVariable t, Void p) {
                String name = t.asElement().getSimpleName().toString();
                return new TypeNameVariable(name);
              }

              @Override
              protected TypeName defaultAction(TypeMirror e, Void p) {
                throw new IllegalArgumentException("Unexpected type mirror: " + e);
              }
            },
            null);

    if (typeName != null && context.getConfiguration().shouldCopyTypeAnnotations()) {
      List<AnnotationDto> annotations =
          FieldAnnotationExtractor.extractAnnotations(typeOfParameter, context);
      annotations.forEach(typeName::addAnnotation);
    }

    return typeName;
  }

  /**
   * Checks if the type element represents a concrete class (not abstract).
   *
   * @param typeElement the type element to check
   * @return true if it's a concrete class
   */
  private static boolean isConcreteClass(TypeElement typeElement) {
    return typeElement != null
        && typeElement.getKind() == javax.lang.model.element.ElementKind.CLASS
        && !typeElement.getModifiers().contains(javax.lang.model.element.Modifier.ABSTRACT);
  }
}
