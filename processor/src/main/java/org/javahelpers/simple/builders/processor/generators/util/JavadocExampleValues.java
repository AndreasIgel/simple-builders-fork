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

package org.javahelpers.simple.builders.processor.generators.util;

import java.util.Map;
import java.util.Optional;
import org.javahelpers.simple.builders.processor.model.type.TypeName;
import org.javahelpers.simple.builders.processor.model.type.TypeNameList;
import org.javahelpers.simple.builders.processor.model.type.TypeNameMap;
import org.javahelpers.simple.builders.processor.model.type.TypeNamePrimitive;
import org.javahelpers.simple.builders.processor.model.type.TypeNamePrimitive.PrimitiveTypeEnum;
import org.javahelpers.simple.builders.processor.model.type.TypeNameSet;

/**
 * Provides default example values for Javadoc code examples.
 *
 * <p>This class provides sensible default values for common types to be used in generated Javadoc
 * code examples. These values are designed to be realistic yet generic enough to work in most
 * contexts.
 */
public final class JavadocExampleValues {

  private static final String STRING_EXAMPLE = "\"example value\"";
  private static final String INT_EXAMPLE = "42";
  private static final String LONG_EXAMPLE = "42L";
  private static final String DOUBLE_EXAMPLE = "3.14";
  private static final String FLOAT_EXAMPLE = "3.14f";
  private static final String BOOLEAN_EXAMPLE = "true";
  private static final String CHAR_EXAMPLE = "'x'";
  private static final String BIGINTEGER_EXAMPLE = "BigInteger.valueOf(42)";
  private static final String BIGDECIMAL_EXAMPLE = "BigDecimal.valueOf(3.14)";
  private static final String UUID_EXAMPLE = "UUID.randomUUID()";
  private static final String LOCALDATE_EXAMPLE = "LocalDate.now()";
  private static final String LOCALTIME_EXAMPLE = "LocalTime.now()";
  private static final String LOCALDATETIME_EXAMPLE = "LocalDateTime.now()";

  /** Map of primitive type enums to their example values. */
  private static final Map<PrimitiveTypeEnum, String> PRIMITIVE_EXAMPLES =
      Map.of(
          PrimitiveTypeEnum.INT, INT_EXAMPLE,
          PrimitiveTypeEnum.LONG, LONG_EXAMPLE,
          PrimitiveTypeEnum.DOUBLE, DOUBLE_EXAMPLE,
          PrimitiveTypeEnum.FLOAT, FLOAT_EXAMPLE,
          PrimitiveTypeEnum.BOOLEAN, BOOLEAN_EXAMPLE,
          PrimitiveTypeEnum.CHAR, CHAR_EXAMPLE);

  /** Map of primitive type enums to mapper expressions. */
  private static final Map<PrimitiveTypeEnum, String> PRIMITIVE_MAPPER_EXAMPLES =
      Map.of(
          PrimitiveTypeEnum.INT, "value -> value * 2",
          PrimitiveTypeEnum.LONG, "value -> value * 2",
          PrimitiveTypeEnum.DOUBLE, "value -> value * 2",
          PrimitiveTypeEnum.FLOAT, "value -> value * 2",
          PrimitiveTypeEnum.BOOLEAN, "value -> !value",
          PrimitiveTypeEnum.CHAR, "Character::toUpperCase");

  /** Map of wrapper and common JDK type FQNs to their example values. */
  private static final Map<String, String> COMMON_TYPE_EXAMPLES =
      Map.ofEntries(
          // Wrapper types (mirror primitives)
          Map.entry("java.lang.Integer", INT_EXAMPLE),
          Map.entry("java.lang.Long", LONG_EXAMPLE),
          Map.entry("java.lang.Double", DOUBLE_EXAMPLE),
          Map.entry("java.lang.Float", FLOAT_EXAMPLE),
          Map.entry("java.lang.Boolean", BOOLEAN_EXAMPLE),
          Map.entry("java.lang.Character", CHAR_EXAMPLE),
          // Common JDK types
          Map.entry("java.math.BigInteger", BIGINTEGER_EXAMPLE),
          Map.entry("java.math.BigDecimal", BIGDECIMAL_EXAMPLE),
          Map.entry("java.util.UUID", UUID_EXAMPLE),
          Map.entry("java.time.LocalDate", LOCALDATE_EXAMPLE),
          Map.entry("java.time.LocalTime", LOCALTIME_EXAMPLE),
          Map.entry("java.time.LocalDateTime", LOCALDATETIME_EXAMPLE));

  /** Map of wrapper and common JDK type FQNs to mapper expressions. */
  private static final Map<String, String> COMMON_TYPE_MAPPER_EXAMPLES =
      Map.ofEntries(
          Map.entry("java.lang.String", "String::trim"),
          Map.entry("java.lang.Integer", "value -> value * 2"),
          Map.entry("java.lang.Long", "value -> value * 2"),
          Map.entry("java.lang.Double", "value -> value * 2"),
          Map.entry("java.lang.Float", "value -> value * 2"),
          Map.entry("java.lang.Boolean", "value -> !value"),
          Map.entry("java.lang.Character", "Character::toUpperCase"),
          Map.entry("java.math.BigInteger", "value -> value.add(BigInteger.ONE)"),
          Map.entry("java.math.BigDecimal", "value -> value.add(BigDecimal.ONE)"),
          Map.entry("java.time.LocalDate", "value -> value.plusDays(1)"),
          Map.entry("java.time.LocalTime", "value -> value.plusHours(1)"),
          Map.entry("java.time.LocalDateTime", "value -> value.plusHours(1)"));

  private JavadocExampleValues() {
    // Utility class - prevent instantiation
  }

  /**
   * Returns an example value for the given type name, if available.
   *
   * <p>This method returns example values for:
   *
   * <ul>
   *   <li>Primitive types (int, long, double, float, boolean, char)
   *   <li>String type (returns {@value STRING_EXAMPLE})
   *   <li>Wrapper types (Integer, Long, Double, Float, Boolean, Character)
   *   <li>Common JDK types (BigInteger, BigDecimal, UUID, LocalDate, LocalTime, LocalDateTime)
   *   <li>Collection types (List, Set) of any supported element type (e.g., {@code List.of("example
   *       value")}, {@code Set.of(42)})
   *   <li>Map types where both key and value types have supported examples (e.g., {@code Map.of(42,
   *       "example value")})
   *   <li>Optional types with a supported inner type (e.g., {@code Optional.of("example value")})
   *   <li>Types with an empty constructor (e.g., {@code new AddressDto()})
   *   <li>Types with a {@code @SimpleBuilder} annotation but no empty constructor (e.g., {@code
   *       AddressDtoBuilder.create().build()})
   * </ul>
   *
   * @param typeName the type name to get an example value for
   * @return an Optional containing the example value, or empty if no example is available
   */
  public static Optional<String> getExampleValue(TypeName typeName) {
    return resolvePrimitive(typeName)
        .or(() -> resolveCollection(typeName))
        .or(() -> resolveOptional(typeName))
        .or(() -> resolveCommonType(typeName))
        .or(() -> resolveString(typeName))
        .or(() -> resolveEmptyConstructor(typeName))
        .or(() -> resolveBuilderType(typeName));
  }

  /**
   * Returns a type-aware mapper expression for the given type, if available.
   *
   * <p>The expression is valid as an argument to {@code UnaryOperator<T>} for the given type and
   * demonstrates a useful transformation where possible.
   *
   * @param typeName the type name to get a mapper expression for
   * @return an Optional containing the mapper expression, or empty if no expression is available
   */
  public static Optional<String> getMapperExample(TypeName typeName) {
    if (typeName instanceof TypeNamePrimitive primitive) {
      return Optional.ofNullable(PRIMITIVE_MAPPER_EXAMPLES.get(primitive.getType()));
    }
    String fqn = typeName.getFullQualifiedName();
    if (fqn != null && COMMON_TYPE_MAPPER_EXAMPLES.containsKey(fqn)) {
      return Optional.of(COMMON_TYPE_MAPPER_EXAMPLES.get(fqn));
    }
    if ("String".equals(typeName.getClassName())) {
      return Optional.of("String::trim");
    }
    return Optional.empty();
  }

  private static Optional<String> resolvePrimitive(TypeName typeName) {
    if (typeName instanceof TypeNamePrimitive primitive) {
      return Optional.ofNullable(PRIMITIVE_EXAMPLES.get(primitive.getType()));
    }
    return Optional.empty();
  }

  private static Optional<String> resolveCollection(TypeName typeName) {
    if (typeName instanceof TypeNameList listType && listType.isParameterized()) {
      return getExampleValue(listType.getElementType())
          .map(elementExample -> "List.of(" + elementExample + ")");
    }
    if (typeName instanceof TypeNameSet setType && setType.isParameterized()) {
      return getExampleValue(setType.getElementType())
          .map(elementExample -> "Set.of(" + elementExample + ")");
    }
    if (typeName instanceof TypeNameMap mapType && mapType.isParameterized()) {
      return resolveMap(mapType);
    }
    return Optional.empty();
  }

  private static Optional<String> resolveMap(TypeNameMap mapType) {
    Optional<String> keyExample = getExampleValue(mapType.getKeyType());
    Optional<String> valueExample = getExampleValue(mapType.getValueType());
    if (keyExample.isPresent() && valueExample.isPresent()) {
      return Optional.of("Map.of(" + keyExample.get() + ", " + valueExample.get() + ")");
    }
    return Optional.empty();
  }

  private static Optional<String> resolveOptional(TypeName typeName) {
    if ("java.util.Optional".equals(typeName.getFullQualifiedName())
        || "Optional".equals(typeName.getClassName())) {
      return typeName
          .getInnerType()
          .flatMap(JavadocExampleValues::getExampleValue)
          .map(inner -> "Optional.of(" + inner + ")");
    }
    return Optional.empty();
  }

  private static Optional<String> resolveCommonType(TypeName typeName) {
    String fqn = typeName.getFullQualifiedName();
    if (fqn != null && COMMON_TYPE_EXAMPLES.containsKey(fqn)) {
      return Optional.of(COMMON_TYPE_EXAMPLES.get(fqn));
    }
    return Optional.empty();
  }

  private static Optional<String> resolveString(TypeName typeName) {
    if ("java.lang.String".equals(typeName.getFullQualifiedName())
        || "String".equals(typeName.getClassName())) {
      return Optional.of(STRING_EXAMPLE);
    }
    return Optional.empty();
  }

  private static Optional<String> resolveEmptyConstructor(TypeName typeName) {
    if (typeName.hasEmptyConstructor()) {
      return Optional.of("new " + typeName.getClassName() + "()");
    }
    return Optional.empty();
  }

  private static Optional<String> resolveBuilderType(TypeName typeName) {
    return typeName
        .getBuilderType()
        .map(builderType -> builderType.getClassName() + ".create().build()");
  }
}
