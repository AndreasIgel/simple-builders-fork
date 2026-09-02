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

package org.javahelpers.simple.builders.example.scoping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.function.Consumer;
import org.javahelpers.simple.builders.example.SponsorDto;
import org.javahelpers.simple.builders.example.SponsorDtoBuilder;
import org.javahelpers.simple.builders.example.library.LibraryHelperDto;
import org.junit.jupiter.api.Test;

class ScopedOwnerDtoBuilderTest {

  @Test
  void exposesScopedBuilderConsumerOverloads() {
    assertTrue(hasBuilderConsumerMethod("trusted", TrustedHelperDtoBuilder.class.getName()));
    assertFalse(
        hasBuilderConsumerMethod(
            "library", "org.javahelpers.simple.builders.example.library.LibraryHelperDtoBuilder"));
    assertFalse(hasBuilderConsumerMethod("sponsor", SponsorDtoBuilder.class.getName()));

    assertTrue(hasMethod("trusted", TrustedHelperDto.class));
    assertTrue(hasMethod("library", LibraryHelperDto.class));
    assertTrue(hasMethod("sponsor", SponsorDto.class));
  }

  @Test
  void buildsValuesThroughScopedApi() {
    LibraryHelperDto library = new LibraryHelperDto();
    library.setCode("library-code");
    SponsorDto sponsor = new SponsorDto();
    sponsor.setName("sponsor-name");

    ScopedOwnerDto result =
        ScopedOwnerDtoBuilder.create()
            .trusted(builder -> builder.name("trusted-name"))
            .library(library)
            .sponsor(sponsor)
            .build();

    assertEquals("trusted-name", result.getTrusted().getName());
    assertEquals("library-code", result.getLibrary().getCode());
    assertEquals("sponsor-name", result.getSponsor().getName());
  }

  private static boolean hasMethod(String name, Class<?> parameterType) {
    return Arrays.stream(ScopedOwnerDtoBuilder.class.getMethods())
        .anyMatch(
            method ->
                method.getName().equals(name)
                    && method.getParameterCount() == 1
                    && method.getParameterTypes()[0].equals(parameterType));
  }

  private static boolean hasBuilderConsumerMethod(String name, String builderTypeName) {
    return Arrays.stream(ScopedOwnerDtoBuilder.class.getMethods())
        .anyMatch(
            method ->
                method.getName().equals(name)
                    && method.getParameterCount() == 1
                    && hasConsumerType(method.getGenericParameterTypes()[0], builderTypeName));
  }

  private static boolean hasConsumerType(Type parameterType, String builderTypeName) {
    if (!(parameterType instanceof ParameterizedType parameterizedType)
        || !Consumer.class.equals(parameterizedType.getRawType())) {
      return false;
    }
    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
    return actualTypeArguments.length == 1
        && actualTypeArguments[0].getTypeName().equals(builderTypeName);
  }
}
