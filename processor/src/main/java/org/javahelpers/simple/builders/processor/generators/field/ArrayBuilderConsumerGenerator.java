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

package org.javahelpers.simple.builders.processor.generators.field;

import static org.javahelpers.simple.builders.processor.analysis.JavaLangMapper.map2TypeName;
import static org.javahelpers.simple.builders.processor.generators.util.MethodGeneratorUtil.*;

import java.util.List;
import org.javahelpers.simple.builders.core.builders.ArrayListBuilder;
import org.javahelpers.simple.builders.processor.generators.MethodGenerator;
import org.javahelpers.simple.builders.processor.generators.util.JavadocConstants;
import org.javahelpers.simple.builders.processor.model.core.FieldDto;
import org.javahelpers.simple.builders.processor.model.javadoc.JavadocDto;
import org.javahelpers.simple.builders.processor.model.method.BuilderMethodDto;
import org.javahelpers.simple.builders.processor.model.method.MethodParameterDto;
import org.javahelpers.simple.builders.processor.model.type.TypeName;
import org.javahelpers.simple.builders.processor.model.type.TypeNameArray;
import org.javahelpers.simple.builders.processor.model.type.TypeNameGeneric;
import org.javahelpers.simple.builders.processor.model.type.TypeNamePrimitive;
import org.javahelpers.simple.builders.processor.processing.ProcessingContext;

/**
 * Generates ArrayListBuilder consumer methods for array fields.
 *
 * <p>This generator creates methods that accept a {@code Consumer<ArrayListBuilder<T>>} to build
 * array fields using a fluent builder API. The consumer configures the list builder, which is then
 * converted to an array and assigned to the field.
 *
 * <p><b>Important behavior:</b> An {@code ArrayListBuilder} is created (preserving existing array
 * elements if the field is already set), passed to the consumer for configuration, then built and
 * converted to an array. This allows fluent array construction with the convenience of list
 * operations.
 *
 * <p><b>Requirements:</b> Only applies to array fields (e.g., {@code String[]}, {@code Integer[]},
 * {@code int[]}, {@code boolean[]}).
 *
 * <p>This generator can be deactivated by setting the configuration flag {@code
 * shouldGenerateBuilderConsumer()} to {@code false}. See the configuration documentation for
 * details.
 *
 * <h3>Example to demonstrate the generated methods</h3>
 *
 * <pre>{@code
 * // ExampleDto for demonstration
 * import org.javahelpers.simple.builders.annotation.SimpleBuilder;
 * import java.util.function.Consumer;
 *
 * @SimpleBuilder
 * public record BookDto(String[] keywords, String[] tags) {}
 *
 * // Usage of generated Builder:
 * var result = BookDtoBuilder.builder()
 *     .keywords(k -> k.add("java").add("builder").add("pattern"))
 *     .tags(t -> t.add("programming").add("design"))
 *     .build();
 * }</pre>
 */
public class ArrayBuilderConsumerGenerator implements MethodGenerator {

  private static final int PRIORITY = 25;

  @Override
  public int getPriority() {
    return PRIORITY;
  }

  @Override
  public boolean appliesTo(FieldDto field, TypeName dtoType, ProcessingContext context) {
    if (!context.getConfiguration().shouldGenerateBuilderConsumer()) {
      return false;
    }

    return field.getFieldType() instanceof TypeNameArray;
  }

  @Override
  public List<BuilderMethodDto> generateMethods(
      FieldDto field, TypeName builderType, ProcessingContext context) {

    TypeName fieldType = field.getFieldType();

    if (!(fieldType instanceof TypeNameArray arrayType)) {
      return List.of();
    }

    TypeName elementType = arrayType.getTypeOfArray();
    TypeName collectionBuilderType = map2TypeName(ArrayListBuilder.class);

    BuilderMethodDto method =
        createFieldConsumerWithArrayBuilder(
            field, collectionBuilderType, elementType, builderType, context);

    return List.of(method);
  }

  private BuilderMethodDto createFieldConsumerWithArrayBuilder(
      FieldDto field,
      TypeName collectionBuilderType,
      TypeName elementType,
      TypeName returnBuilderType,
      ProcessingContext context) {
    String fieldName = field.getOriginalFieldName();
    String fieldNameInBuilder = field.getFieldNameInBuilder();
    TypeNameGeneric builderTypeGeneric = new TypeNameGeneric(collectionBuilderType, elementType);
    TypeNameGeneric consumerType = createConsumerType(builderTypeGeneric);

    MethodParameterDto parameter = new MethodParameterDto();
    parameter.setParameterName(fieldName + BUILDER_SUFFIX + SUFFIX_CONSUMER);
    parameter.setParameterTypeName(consumerType);

    BuilderMethodDto methodDto = createBuilderMethod(fieldName, returnBuilderType, context);
    methodDto.addParameter(parameter);
    if (elementType instanceof TypeNamePrimitive) {
      methodDto.setCode(
          """
          $helperType:T builder;
          if (this.$fieldName:N.isSet()) {
            java.util.List<$elementType:B> $fieldName:N__existing = new java.util.ArrayList<>($fieldName:N.value().length);
            for (int $fieldName:N__i = 0; $fieldName:N__i < $fieldName:N.value().length; $fieldName:N__i++) {
              $fieldName:N__existing.add($fieldName:N.value()[$fieldName:N__i]);
            }
            builder = new $helperType:T($fieldName:N__existing);
          } else {
            builder = new $helperType:T();
          }
          $dtoMethodParam:N.accept(builder);
          var $fieldName:N__list = builder.build();
          $elementType:T[] $fieldName:N__array = new $elementType:T[$fieldName:N__list.size()];
          for (int $fieldName:N__i = 0; $fieldName:N__i < $fieldName:N__array.length; $fieldName:N__i++) {
            $fieldName:N__array[$fieldName:N__i] = $fieldName:N__list.get($fieldName:N__i);
          }
          this.$fieldName:N = $builderFieldWrapper:T.changedValue($fieldName:N__array);
          return this;
          """);
    } else {
      methodDto.setCode(
          """
          $helperType:T builder = this.$fieldName:N.isSet()
            ? new $helperType:T(java.util.List.of(this.$fieldName:N.value()))
            : new $helperType:T();
          $dtoMethodParam:N.accept(builder);
          this.$fieldName:N = $builderFieldWrapper:T.changedValue(builder.build().toArray(new $elementType:T[0]));
          return this;
          """);
    }

    // Add code block import for java.util.List.of
    methodDto.getMethodCodeDto().addCodeBlockImport(List.class);

    methodDto.addArgument("fieldName", fieldNameInBuilder);
    methodDto.addArgument("dtoMethodParam", parameter.getParameterName());
    methodDto.addArgument("helperType", builderTypeGeneric);
    methodDto.addArgument("builderFieldWrapper", TRACKED_VALUE_TYPE);
    methodDto.addArgument("elementType", elementType);
    methodDto.setPriority(BuilderMethodDto.PRIORITY_MEDIUM);
    methodDto.setJavadoc(
        new JavadocDto(
                "Sets the value for <code>%s</code> using the fluent builder consumer.", fieldName)
            .addParam(parameter.getParameterName(), "consumer for %s", fieldName)
            .addReturn(JavadocConstants.RETURN_BUILDER_INSTANCE));
    return methodDto;
  }
}
