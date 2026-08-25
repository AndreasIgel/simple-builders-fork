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

package org.javahelpers.simple.builders.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.javahelpers.simple.builders.core.enums.OptionState;

/**
 * Built-in minimal builder template annotation.
 *
 * <p>Generates the smallest possible fluent builder by disabling all optional features. Use this
 * when you only need {@code create()}, field setters and {@code build()} without suppliers,
 * consumers, varargs, collection helpers, {@code With} interface, Jackson integration or generated
 * annotations.
 *
 * <p>This annotation is implemented purely as a {@link SimpleBuilder.Template} with every optional
 * feature set to {@link OptionState#DISABLED}. Because the template is {@link Inherited},
 * subclasses of an annotated type also receive a minimal builder unless explicitly excluded by
 * {@link Ignore4BuilderGeneration}.
 *
 * <p>Example:
 *
 * <pre>{@code
 * @SimpleMinimalBuilder
 * public class PersonDto {
 *     private String name;
 *
 *     public String getName() { return name; }
 *     public void setName(String name) { this.name = name; }
 * }
 * }</pre>
 *
 * <p>Generated builder usage:
 *
 * <pre>{@code
 * PersonDto person = PersonDtoBuilder.create()
 *     .name("John")
 *     .build();
 * }</pre>
 *
 * @see SimpleBuilder
 * @see SimpleBuilder.Template
 * @see SimpleBuilder.Options
 * @see Ignore4BuilderGeneration
 */
@SimpleBuilder.Template(
    options =
        @SimpleBuilder.Options(
            generateFieldSupplier = OptionState.DISABLED,
            generateFieldConsumer = OptionState.DISABLED,
            generateBuilderConsumer = OptionState.DISABLED,
            generateConditionalHelper = OptionState.DISABLED,
            generateVarArgsHelpers = OptionState.DISABLED,
            generateStringFormatHelpers = OptionState.DISABLED,
            generateAddToCollectionHelpers = OptionState.DISABLED,
            generateUnboxedOptional = OptionState.DISABLED,
            generateWithInterface = OptionState.DISABLED,
            usingArrayListBuilder = OptionState.DISABLED,
            usingArrayListBuilderWithElementBuilders = OptionState.DISABLED,
            usingHashSetBuilder = OptionState.DISABLED,
            usingHashSetBuilderWithElementBuilders = OptionState.DISABLED,
            usingHashMapBuilder = OptionState.DISABLED,
            usingGeneratedAnnotation = OptionState.DISABLED,
            usingBuilderImplementationAnnotation = OptionState.DISABLED,
            usingJacksonDeserializerAnnotation = OptionState.DISABLED,
            generateJacksonModule = OptionState.DISABLED,
            generateJavaDoc = OptionState.DISABLED,
            copyTypeAnnotations = OptionState.DISABLED,
            implementsBuilderBase = OptionState.DISABLED,
            builderSuffix = "Builder",
            setterSuffix = ""))
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Inherited
public @interface SimpleMinimalBuilder {}
