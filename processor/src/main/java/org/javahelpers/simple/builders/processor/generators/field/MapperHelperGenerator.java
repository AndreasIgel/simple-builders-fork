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
import static org.javahelpers.simple.builders.processor.generators.util.MethodGeneratorUtil.TRACKED_VALUE_TYPE;
import static org.javahelpers.simple.builders.processor.generators.util.MethodGeneratorUtil.addExampleChainFragmentTemplate;
import static org.javahelpers.simple.builders.processor.generators.util.MethodGeneratorUtil.getMethodAccessModifier;

import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;
import org.apache.commons.lang3.StringUtils;
import org.javahelpers.simple.builders.processor.generators.MethodGenerator;
import org.javahelpers.simple.builders.processor.generators.util.JavadocConstants;
import org.javahelpers.simple.builders.processor.model.core.FieldDto;
import org.javahelpers.simple.builders.processor.model.javadoc.JavadocDto;
import org.javahelpers.simple.builders.processor.model.method.BuilderMethodDto;
import org.javahelpers.simple.builders.processor.model.method.MethodParameterDto;
import org.javahelpers.simple.builders.processor.model.type.TypeName;
import org.javahelpers.simple.builders.processor.model.type.TypeNameGeneric;
import org.javahelpers.simple.builders.processor.processing.ProcessingContext;

/**
 * Generates opt-in mapper helper methods for builder fields.
 *
 * <p>Each generated method accepts a {@code UnaryOperator<T>} and applies it to the field's current
 * value. Throws {@link IllegalStateException} if the value has not been set yet.
 *
 * <h3>Example to demonstrate the generated methods</h3>
 *
 * <pre>{@code
 * var result = PersonDtoBuilder.create()
 *     .name("  bob ")
 *     .mapName(String::trim)
 *     .quantity(10)
 *     .mapQuantity(q -> q * 2)
 *     .build();
 * }</pre>
 */
public class MapperHelperGenerator implements MethodGenerator {

  private static final int PRIORITY = 59;

  @Override
  public int getPriority() {
    return PRIORITY;
  }

  @Override
  public boolean appliesTo(FieldDto field, TypeName dtoType, ProcessingContext context) {
    return context.getConfiguration().shouldGenerateMapperHelpers();
  }

  @Override
  public List<BuilderMethodDto> generateMethods(
      FieldDto field, TypeName builderType, ProcessingContext context) {
    String originalFieldName = field.getOriginalFieldName();
    String parameterName = originalFieldName + "Mapper";
    TypeNameGeneric mapperType =
        new TypeNameGeneric(map2TypeName(UnaryOperator.class), field.getFieldType());

    MethodParameterDto parameter = new MethodParameterDto();
    parameter.setParameterName(parameterName);
    parameter.setParameterTypeName(mapperType);

    BuilderMethodDto methodDto =
        new BuilderMethodDto("map" + StringUtils.capitalize(originalFieldName), builderType);
    methodDto.setModifier(getMethodAccessModifier(context));
    methodDto.addParameter(parameter);
    methodDto.setCode(
        """
        if (!this.$fieldName:N.isSet()) {
          throw new $illegalStateException:T("Cannot map '$originalFieldName:N' before it is set");
        }
        this.$fieldName:N = $builderFieldWrapper:T.changedValue($param:N.apply(this.$fieldName:N.value()));
        return this;
        """);
    methodDto.addArgument("fieldName", field.getFieldNameInBuilder());
    methodDto.addArgument("originalFieldName", originalFieldName);
    methodDto.addArgument("param", parameterName);
    methodDto.addArgument("builderFieldWrapper", TRACKED_VALUE_TYPE);
    methodDto.addArgument("illegalStateException", map2TypeName(IllegalStateException.class));
    methodDto.getMethodCodeDto().addCodeBlockImport(IllegalStateException.class);
    methodDto.setPriority(BuilderMethodDto.PRIORITY_LOW);
    methodDto.setJavadoc(
        new JavadocDto(
                "Transforms the current value of <code>%s</code> by applying the given operator.",
                originalFieldName)
            .addParam(parameterName, "operator used to transform the current value")
            .addReturn(JavadocConstants.RETURN_BUILDER_INSTANCE)
            .addThrows("IllegalStateException", "if the field is unset"));

    addExampleChainFragmentTemplate(methodDto, "#{methodName}(value -> value)");
    return Collections.singletonList(methodDto);
  }
}
