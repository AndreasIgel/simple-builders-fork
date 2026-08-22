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

package org.javahelpers.simple.builders.processor.classgen.roaster;

import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.javahelpers.simple.builders.processor.classgen.roaster.exceptions.RoasterMapperException;
import org.javahelpers.simple.builders.processor.model.annotation.AnnotationDto;
import org.javahelpers.simple.builders.processor.model.annotation.InterfaceName;
import org.javahelpers.simple.builders.processor.model.method.CodeTemplateDto;
import org.javahelpers.simple.builders.processor.model.method.MethodCodePlaceholder;
import org.javahelpers.simple.builders.processor.model.method.MethodCodeStringPlaceholder;
import org.javahelpers.simple.builders.processor.model.method.MethodCodeTypePlaceholder;
import org.javahelpers.simple.builders.processor.model.type.TypeName;
import org.javahelpers.simple.builders.processor.model.type.TypeNameArray;
import org.javahelpers.simple.builders.processor.model.type.TypeNameGeneric;
import org.javahelpers.simple.builders.processor.model.type.TypeNamePrimitive;
import org.javahelpers.simple.builders.processor.model.type.TypeNameVariable;

/** Helper functions to create Roaster-compatible source code strings from DTOs. */
public final class RoasterMapper {

  private RoasterMapper() {}

  /**
   * Maps a type model to Java source code using fully qualified names for robustness.
   *
   * @param typeName type to map
   * @return Java source representation of the type
   */
  public static String mapType(TypeName typeName) {
    String mappedType;
    if (typeName instanceof TypeNameVariable) {
      mappedType = typeName.getClassName();
    } else if (typeName instanceof TypeNamePrimitive primitive) {
      mappedType = primitive.getFullQualifiedName();
    } else if (typeName instanceof TypeNameArray arrayType) {
      mappedType = mapType(arrayType.getTypeOfArray()) + "[]";
    } else if (typeName instanceof TypeNameGeneric genericType) {
      mappedType = mapGenericType(genericType);
    } else {
      mappedType = typeName.getClassName();
    }

    return prependTypeUseAnnotations(mappedType, typeName.getAnnotations());
  }

  /**
   * Maps a type to a boxed Java source representation.
   *
   * @param typeName type to map
   * @return boxed Java source representation of the type
   */
  public static String mapBoxedType(TypeName typeName) {
    if (typeName instanceof TypeNamePrimitive primitive) {
      String boxedType =
          switch (primitive.getType()) {
            case BOOLEAN -> Boolean.class.getSimpleName();
            case BYTE -> Byte.class.getSimpleName();
            case CHAR -> Character.class.getSimpleName();
            case DOUBLE -> Double.class.getSimpleName();
            case FLOAT -> Float.class.getSimpleName();
            case INT -> Integer.class.getSimpleName();
            case LONG -> Long.class.getSimpleName();
            case SHORT -> Short.class.getSimpleName();
            case VOID -> Void.class.getSimpleName();
          };
      return prependTypeUseAnnotations(boxedType, typeName.getAnnotations());
    }
    return mapType(typeName);
  }

  private static String mapGenericType(TypeNameGeneric genericType) {
    if (genericType.getInnerTypeArguments().isEmpty()) {
      return genericType.getClassName();
    }
    String innerTypes =
        genericType.getInnerTypeArguments().stream()
            .map(RoasterMapper::mapBoxedType)
            .reduce((a, b) -> a + ", " + b)
            .orElse("");
    return genericType.getClassName() + "<" + innerTypes + ">";
  }

  /**
   * Maps an annotation to source code.
   *
   * @param annotationDto annotation DTO
   * @return Java source representation of the annotation
   */
  public static String mapAnnotation(AnnotationDto annotationDto) {
    try {
      String annotationType = mapType(annotationDto.getAnnotationType());
      if (annotationDto.getMembers().isEmpty()) {
        return "@" + annotationType;
      }
      String members =
          annotationDto.getMembers().entrySet().stream()
              .map(RoasterMapper::mapAnnotationMember)
              .reduce((a, b) -> a + ", " + b)
              .orElse("");
      return "@" + annotationType + "(" + members + ")";
    } catch (Exception e) {
      throw new RoasterMapperException(
          e,
          "Failed to map annotation %s: %s",
          annotationDto.getAnnotationType().getClassName(),
          e.getMessage());
    }
  }

  private static String mapAnnotationMember(Map.Entry<String, String> member) {
    if ("value".equals(member.getKey())) {
      return member.getValue();
    }
    return member.getKey() + " = " + member.getValue();
  }

  /**
   * Maps an interface name to Java source code.
   *
   * @param interfaceName interface model
   * @return source representation of the interface type
   */
  public static String mapInterfaceToTypeName(InterfaceName interfaceName) {
    try {
      String qualifiedName = interfaceName.getSimpleName();
      if (interfaceName.hasTypeParameters()) {
        String typeParameters =
            interfaceName.getTypeParameters().stream()
                .map(RoasterMapper::mapType)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        qualifiedName = qualifiedName + "<" + typeParameters + ">";
      }
      return prependTypeUseAnnotations(qualifiedName, interfaceName.getAnnotations());
    } catch (Exception e) {
      throw new RoasterMapperException(
          e, "Failed to map interface %s: %s", interfaceName.toString(), e.getMessage());
    }
  }

  /**
   * Resolves the JavaPoet-style named template used in CodeTemplateDto to plain Java source code.
   *
   * <p>Type placeholders support {@code $label:T} for the plain type and {@code $label:B} for its
   * boxed representation.
   *
   * @param codeDto code template DTO
   * @return resolved Java source code
   */
  public static String resolveCodeTemplate(CodeTemplateDto codeDto) {
    String code = codeDto.getCodeFormat();
    for (MethodCodePlaceholder<?> placeHolderValue : codeDto.getCodeArguments()) {
      String label = placeHolderValue.getLabel();
      if (placeHolderValue instanceof MethodCodeStringPlaceholder stringPlaceholder) {
        code = code.replace("$" + label + ":N", stringPlaceholder.getValue());
        code = code.replace("$" + label + ":L", stringPlaceholder.getValue());
        code = code.replace("$" + label + ":S", quote(stringPlaceholder.getValue()));
      } else if (placeHolderValue instanceof MethodCodeTypePlaceholder typePlaceholder) {
        code = code.replace("$" + label + ":T", mapType(typePlaceholder.getValue()));
        code = code.replace("$" + label + ":B", mapBoxedType(typePlaceholder.getValue()));
      } else {
        throw new RoasterMapperException(
            "Unsupported placeholder type: %s", placeHolderValue.getClass().getName());
      }
    }
    code = code.replace("TrackedValue.initialValue", "initialValue");
    code = code.replace("TrackedValue.changedValue", "changedValue");
    code = code.replace("TrackedValue.unsetValue", "unsetValue");
    return code;
  }

  /**
   * Converts plain text to a Java string literal.
   *
   * @param value text value
   * @return quoted and escaped Java string literal
   */
  public static String quote(String value) {
    String escaped =
        value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    return "\"" + escaped + "\"";
  }

  /**
   * Extracts package name from fully qualified name.
   *
   * @param fqn fully qualified name
   * @return package name or empty string if no package
   */
  public static String packageNameOf(String fqn) {
    int idx = fqn.lastIndexOf('.');
    return idx < 0 ? "" : fqn.substring(0, idx);
  }

  private static String prependTypeUseAnnotations(
      String baseType, List<AnnotationDto> annotations) {
    if (CollectionUtils.isEmpty(annotations)) {
      return baseType;
    }
    String prefix =
        annotations.stream()
            .map(RoasterMapper::mapAnnotation)
            .reduce((a, b) -> a + " " + b)
            .orElse("");
    return prefix + " " + baseType;
  }
}
