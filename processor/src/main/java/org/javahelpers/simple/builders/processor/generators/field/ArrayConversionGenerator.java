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
 * Generates array-from-List conversion methods for array fields.
 *
 * <p>This generator creates setter methods that accept a {@code List<T>} parameter and convert it
 * to an array for array fields. This provides a more convenient way to set array values using the
 * List API instead of manually creating arrays.
 *
 * <p><b>Important behavior:</b> The List is converted to an array using {@code toArray()}, then
 * assigned to the field. This allows using List operations and utilities before converting to the
 * required array type.
 *
 * <p><b>Requirements:</b> Only applies to array fields (e.g., {@code String[]}, {@code Integer[]},
 * {@code int[]}, {@code boolean[]}).
 *
 * <p>This generator cannot be deactivated as it provides essential convenience for array fields.
 *
 * <h3>Example to demonstrate the generated methods</h3>
 *
 * <pre>{@code
 * // ExampleDto for demonstration
 * import org.javahelpers.simple.builders.annotation.SimpleBuilder;
 * import java.util.List;
 *
 * @SimpleBuilder
 * public record BookDto(String[] keywords, String[] tags) {}
 *
 * // Usage of generated Builder:
 * var result = BookDtoBuilder.builder()
 *     .keywords(List.of("java", "builder", "pattern"))
 *     .tags(List.of("programming", "design"))
 *     .build();
 * }</pre>
 */
public class ArrayConversionGenerator implements MethodGenerator {

  private static final int PRIORITY = 35;

  @Override
  public int getPriority() {
    return PRIORITY;
  }

  @Override
  public boolean appliesTo(FieldDto field, TypeName dtoType, ProcessingContext context) {
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
    TypeNameGeneric listType = new TypeNameGeneric(map2TypeName(List.class), elementType);

    BuilderMethodDto method =
        createFieldSetterForArrayFromList(field, listType, elementType, builderType, context);

    return List.of(method);
  }

  private BuilderMethodDto createFieldSetterForArrayFromList(
      FieldDto field,
      TypeName listType,
      TypeName elementType,
      TypeName builderType,
      ProcessingContext context) {
    String fieldName = field.getOriginalFieldName();
    String fieldNameInBuilder = field.getFieldNameInBuilder();
    MethodParameterDto parameter = new MethodParameterDto();
    parameter.setParameterName(fieldName);
    parameter.setParameterTypeName(listType);

    BuilderMethodDto methodDto = createBuilderMethod(fieldName, builderType, context);
    methodDto.addParameter(parameter);
    if (elementType instanceof TypeNamePrimitive) {
      methodDto.setCode(
          """
          this.$fieldName:N = $builderFieldWrapper:T.changedValue(
              ArrayUtils.toPrimitive($dtoMethodParams:N.toArray(new $elementType:B[0])));
          return this;
          """);
      methodDto.getMethodCodeDto().addCodeBlockImport(org.apache.commons.lang3.ArrayUtils.class);
    } else {
      methodDto.setCode(
          """
          this.$fieldName:N = $builderFieldWrapper:T.changedValue($dtoMethodParams:N.toArray(new $elementType:T[0]));
          return this;
          """);
    }
    methodDto.addArgument("fieldName", fieldNameInBuilder);
    methodDto.addArgument("dtoMethodParams", fieldName);
    methodDto.addArgument("builderFieldWrapper", TRACKED_VALUE_TYPE);
    methodDto.addArgument("elementType", elementType);
    methodDto.setPriority(BuilderMethodDto.PRIORITY_HIGH);
    String fieldJavadocDesc = field.getJavaDocDescriptionOrFieldName();
    methodDto.setJavadoc(
        new JavadocDto("Sets the value for <code>%s</code>.", fieldName)
            .addParam(parameter.getParameterName(), fieldJavadocDesc)
            .addReturn(JavadocConstants.RETURN_BUILDER_INSTANCE));

    // Add example fragment for array conversion method
    addExampleChainFragment(methodDto, elementType);

    return methodDto;
  }
}
