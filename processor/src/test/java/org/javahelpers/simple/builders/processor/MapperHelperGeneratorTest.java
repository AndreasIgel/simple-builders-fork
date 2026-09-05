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

package org.javahelpers.simple.builders.processor;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static org.javahelpers.simple.builders.processor.testing.ProcessorTestUtils.loadGeneratedSource;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.testing.compile.Compilation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import javax.annotation.processing.Processor;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.javahelpers.simple.builders.processor.testing.ProcessorAsserts;
import org.javahelpers.simple.builders.processor.testing.ProcessorTestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for {@code mapX(UnaryOperator<T>)} helper generation. */
class MapperHelperGeneratorTest {

  @TempDir Path tempDirectory;

  @Test
  void mapperHelpers_DefaultEnabled_GeneratesMethods() {
    Compilation compilation = ProcessorTestUtils.createCompiler().compile(personSource());

    assertThat(compilation).succeeded();
    String generated = loadGeneratedSource(compilation, "PersonDtoBuilder");

    ProcessorAsserts.assertContaining(generated, "mapName(");
    ProcessorAsserts.assertContaining(generated, "mapQuantity(");
  }

  @Test
  void mapperHelpers_DisabledByCompilerOption_DoesNotGenerateMethods() {
    Compilation compilation =
        ProcessorTestUtils.createCompiler()
            .withOptions("-Asimplebuilder.generateMapperHelpers=DISABLED")
            .compile(personSource());

    assertThat(compilation).succeeded();
    String generated = loadGeneratedSource(compilation, "PersonDtoBuilder");

    ProcessorAsserts.assertNotContaining(generated, "mapName(");
    ProcessorAsserts.assertNotContaining(generated, "mapQuantity(");
  }

  @Test
  void mapperHelpers_CompilerOptionEnabled_GeneratesStringAndPrimitiveMethods() {
    Compilation compilation =
        ProcessorTestUtils.createCompiler()
            .withOptions("-Asimplebuilder.generateMapperHelpers=ENABLED")
            .compile(personSource());

    assertThat(compilation).succeeded();
    String generated = loadGeneratedSource(compilation, "PersonDtoBuilder");

    ProcessorAsserts.assertContaining(
        generated, "public PersonDtoBuilder mapName(UnaryOperator<String> nameMapper)");
    ProcessorAsserts.assertContaining(
        generated, "public PersonDtoBuilder mapQuantity(UnaryOperator<Integer> quantityMapper)");
    ProcessorAsserts.assertContaining(
        generated, "throw new IllegalStateException(\"Cannot map 'name' before it is set\")");
    ProcessorAsserts.assertContaining(
        generated, "this.quantity = changedValue(quantityMapper.apply(this.quantity.value()));");
    assertTrue(
        generated.contains(".mapName(String::trim);"),
        "String mapper methods should use a type-aware example");
    assertTrue(
        generated.contains(".mapQuantity(value -> value * 2);"),
        "Primitive mapper methods should use a type-aware example");
  }

  @Test
  void mapperHelpers_CompilerOptionEnabled_ContainsUnsetFieldGuard() {
    Compilation compilation =
        ProcessorTestUtils.createCompiler()
            .withOptions("-Asimplebuilder.generateMapperHelpers=ENABLED")
            .compile(personSource());

    assertThat(compilation).succeeded();
    String generated = loadGeneratedSource(compilation, "PersonDtoBuilder");

    ProcessorAsserts.assertContaining(generated, "if (!this.name.isSet())");
    ProcessorAsserts.assertContaining(generated, "Cannot map 'name' before it is set");
  }

  @Test
  void mapperHelpers_RuntimeMappingAndUnsetFieldFailure() throws Exception {
    try (URLClassLoader classLoader = compileRuntimePerson()) {
      Class<?> builderClass = classLoader.loadClass("test.PersonDtoBuilder");
      Method create = builderClass.getMethod("create");
      Method name = builderClass.getMethod("name", String.class);
      Method quantity = builderClass.getMethod("quantity", int.class);
      Method mapName = builderClass.getMethod("mapName", UnaryOperator.class);
      Method mapQuantity = builderClass.getMethod("mapQuantity", UnaryOperator.class);
      Method build = builderClass.getMethod("build");

      Object builder = create.invoke(null);
      InvocationTargetException exception =
          assertThrows(
              InvocationTargetException.class,
              () -> mapName.invoke(builder, (UnaryOperator<String>) String::trim));
      assertEquals("Cannot map 'name' before it is set", exception.getCause().getMessage());

      name.invoke(builder, "  bob ");
      mapName.invoke(builder, (UnaryOperator<String>) String::trim);
      mapName.invoke(builder, (UnaryOperator<String>) String::toUpperCase);
      quantity.invoke(builder, 10);
      mapQuantity.invoke(builder, (UnaryOperator<Integer>) value -> value * 2);

      Object person = build.invoke(builder);
      assertEquals("BOB", person.getClass().getMethod("name").invoke(person));
      assertEquals(20, person.getClass().getMethod("quantity").invoke(person));
    }
  }

  @Test
  void mapperHelpers_WithCopyInteraction_UsesInitialValueAsSet() {
    JavaFileObject source =
        ProcessorTestUtils.forSource(
            """
            package test;

            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder(options = @SimpleBuilder.Options(
                generateMapperHelpers = org.javahelpers.simple.builders.core.enums.OptionState.ENABLED,
                generateWithInterface = org.javahelpers.simple.builders.core.enums.OptionState.ENABLED))
            public record PersonWith(String name) implements PersonWithBuilder.With {}
            """);

    Compilation compilation = ProcessorTestUtils.createCompiler().compile(source);

    assertThat(compilation).succeeded();
    String generated = loadGeneratedSource(compilation, "PersonWithBuilder");

    ProcessorAsserts.assertContaining(
        generated, "public PersonWithBuilder mapName(UnaryOperator<String> nameMapper)");
    ProcessorAsserts.assertContaining(generated, "initialValue(instance.name())");
  }

  @Test
  void mapperHelpers_WithCopyInteraction_MapsCopiedValue() throws Exception {
    try (URLClassLoader classLoader = compileRuntimeWithPerson()) {
      Class<?> personClass = classLoader.loadClass("test.PersonWith");
      Object person = personClass.getConstructor(String.class).newInstance("  bob ");
      Method with = personClass.getMethod("with", java.util.function.Consumer.class);

      Object mapped =
          with.invoke(
              person,
              (java.util.function.Consumer<Object>)
                  builder -> invokeMapper(builder, (UnaryOperator<String>) String::toUpperCase));

      assertEquals("  BOB ", mapped.getClass().getMethod("name").invoke(mapped));
    }
  }

  @Test
  void mapperHelpers_AnnotationOptionEnabled_GeneratesMethod() {
    JavaFileObject source =
        ProcessorTestUtils.forSource(
            """
            package test;

            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
            import org.javahelpers.simple.builders.core.enums.OptionState;

            @SimpleBuilder(options = @SimpleBuilder.Options(
                generateMapperHelpers = OptionState.ENABLED))
            public record AnnotatedPerson(String name) {}
            """);

    Compilation compilation = ProcessorTestUtils.createCompiler().compile(source);

    assertThat(compilation).succeeded();
    String generated = loadGeneratedSource(compilation, "AnnotatedPersonBuilder");

    ProcessorAsserts.assertContaining(
        generated, "public AnnotatedPersonBuilder mapName(UnaryOperator<String> nameMapper)");
  }

  @Test
  void mapperHelpers_ComponentDeactivation_DisablesGeneration() {
    Compilation compilation =
        ProcessorTestUtils.createCompiler()
            .withOptions(
                "-Asimplebuilder.generateMapperHelpers=ENABLED",
                "-Asimplebuilder.deactivateGenerationComponents=MapperHelperGenerator")
            .compile(personSource());

    assertThat(compilation).succeeded();
    String generated = loadGeneratedSource(compilation, "PersonDtoBuilder");

    ProcessorAsserts.assertNotContaining(generated, "mapName(");
    ProcessorAsserts.assertNotContaining(generated, "mapQuantity(");
  }

  @Test
  void mapperHelpers_SameSignatureCollision_PlainSetterWins() {
    JavaFileObject source =
        ProcessorTestUtils.forSource(
            """
            package test;

            import java.util.function.UnaryOperator;
            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public record CollisionDto(String test, UnaryOperator<String> mapTest) {}
            """);

    Compilation compilation = ProcessorTestUtils.createCompiler().compile(source);

    assertThat(compilation).succeeded();
    String generated = loadGeneratedSource(compilation, "CollisionDtoBuilder");
    long conflictWarnings =
        compilation.diagnostics().stream()
            .filter(diagnostic -> diagnostic.getKind() == javax.tools.Diagnostic.Kind.WARNING)
            .filter(diagnostic -> diagnostic.getMessage(null).contains("Method conflict resolved"))
            .count();

    assertTrue(conflictWarnings > 0, "Expected a mapper/setter conflict warning");
    ProcessorAsserts.assertContaining(generated, "mapTest(UnaryOperator<String> mapTest)");
    ProcessorAsserts.assertNotContaining(generated, "mapTest(UnaryOperator<String> testMapper)");
    ProcessorAsserts.assertContaining(
        generated, "mapMapTest(UnaryOperator<UnaryOperator<String>> mapTestMapper)");
  }

  @Test
  void mapperHelpers_SameSignatureCollision_PlainSetterCanBeCalled() throws Exception {
    try (URLClassLoader classLoader = compileRuntimeCollision()) {
      Class<?> builderClass = classLoader.loadClass("test.CollisionDtoBuilder");
      Object builder = builderClass.getMethod("create").invoke(null);
      UnaryOperator<String> mapper = String::trim;

      builderClass.getMethod("mapTest", UnaryOperator.class).invoke(builder, mapper);

      Object dto = builderClass.getMethod("build").invoke(builder);
      assertEquals(mapper, dto.getClass().getMethod("mapTest").invoke(dto));
    }
  }

  @Test
  void mapperHelpers_DifferentTypeCollision_GeneratesBothMethods() {
    JavaFileObject source =
        ProcessorTestUtils.forSource(
            """
            package test;

            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

            @SimpleBuilder
            public record DifferentTypeDto(String test, String mapTest) {}
            """);

    Compilation compilation = ProcessorTestUtils.createCompiler().compile(source);

    assertThat(compilation).succeeded();
    String generated = loadGeneratedSource(compilation, "DifferentTypeDtoBuilder");

    ProcessorAsserts.assertContaining(generated, "mapTest(String mapTest)");
    ProcessorAsserts.assertContaining(generated, "mapTest(UnaryOperator<String> testMapper)");
  }

  @Test
  void mapperHelpers_DifferentTypeCollision_BothMethodsCanBeCalled() throws Exception {
    try (URLClassLoader classLoader = compileRuntimeDifferentType()) {
      Class<?> builderClass = classLoader.loadClass("test.DifferentTypeDtoBuilder");
      Object builder = builderClass.getMethod("create").invoke(null);
      builderClass.getMethod("test", String.class).invoke(builder, "  bob ");
      builderClass.getMethod("mapTest", String.class).invoke(builder, "mapped");
      builderClass
          .getMethod("mapTest", UnaryOperator.class)
          .invoke(builder, (UnaryOperator<String>) String::trim);

      Object dto = builderClass.getMethod("build").invoke(builder);
      assertEquals("bob", dto.getClass().getMethod("test").invoke(dto));
      assertEquals("mapped", dto.getClass().getMethod("mapTest").invoke(dto));
    }
  }

  @Test
  void mapperHelpers_NullResult_NonNullFieldFailsAtBuild() throws Exception {
    try (URLClassLoader classLoader = compileRuntimeNullResult()) {
      Class<?> builderClass = classLoader.loadClass("test.NullResultDtoBuilder");
      Object builder = builderClass.getMethod("create").invoke(null);
      Method name = builderClass.getMethod("name", String.class);
      Method mapName = builderClass.getMethod("mapName", UnaryOperator.class);
      Method build = builderClass.getMethod("build");

      name.invoke(builder, "x");
      assertDoesNotThrow(() -> mapName.invoke(builder, (UnaryOperator<String>) value -> null));

      InvocationTargetException exception =
          assertThrows(InvocationTargetException.class, () -> build.invoke(builder));
      assertTrue(exception.getCause().getMessage().contains("marked as non-null"));
    }
  }

  @Test
  void mapperHelpers_NullResult_PrimitiveFieldFailsAtBuild() throws Exception {
    try (URLClassLoader classLoader = compileRuntimeNullResult()) {
      Class<?> builderClass = classLoader.loadClass("test.NullResultDtoBuilder");
      Object builder = builderClass.getMethod("create").invoke(null);
      Method quantity = builderClass.getMethod("quantity", int.class);
      Method mapQuantity = builderClass.getMethod("mapQuantity", UnaryOperator.class);
      Method build = builderClass.getMethod("build");

      quantity.invoke(builder, 1);
      assertDoesNotThrow(() -> mapQuantity.invoke(builder, (UnaryOperator<Integer>) value -> null));

      InvocationTargetException exception =
          assertThrows(InvocationTargetException.class, () -> build.invoke(builder));
      assertTrue(exception.getCause().getMessage().contains("marked as non-null"));
    }
  }

  @Test
  void mapperHelpers_NullResult_NullableFieldBuildsWithNull() throws Exception {
    try (URLClassLoader classLoader = compileRuntimeNullResult()) {
      Class<?> builderClass = classLoader.loadClass("test.NullResultDtoBuilder");
      Object builder = builderClass.getMethod("create").invoke(null);
      Method description = builderClass.getMethod("description", String.class);
      Method mapDescription = builderClass.getMethod("mapDescription", UnaryOperator.class);
      Method build = builderClass.getMethod("build");

      description.invoke(builder, "x");
      assertDoesNotThrow(
          () -> mapDescription.invoke(builder, (UnaryOperator<String>) value -> null));

      Object dto = build.invoke(builder);
      assertNull(dto.getClass().getMethod("getDescription").invoke(dto));
    }
  }

  private static JavaFileObject personSource() {
    return ProcessorTestUtils.forSource(
        """
        package test;

        import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

        @SimpleBuilder
        public record PersonDto(String name, int quantity) {}
        """);
  }

  private URLClassLoader compileRuntimePerson() throws Exception {
    return compileRuntimeSource(
        "PersonDto.java",
        """
        package test;

        import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

        @SimpleBuilder
        public record PersonDto(String name, int quantity) {}
        """);
  }

  private URLClassLoader compileRuntimeWithPerson() throws Exception {
    return compileRuntimeSource(
        "PersonWith.java",
        """
        package test;

        import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
        import org.javahelpers.simple.builders.core.enums.OptionState;

        @SimpleBuilder(options = @SimpleBuilder.Options(
            generateMapperHelpers = OptionState.ENABLED,
            generateWithInterface = OptionState.ENABLED))
        public record PersonWith(String name) implements PersonWithBuilder.With {}
        """);
  }

  private URLClassLoader compileRuntimeCollision() throws Exception {
    return compileRuntimeSource(
        "CollisionDto.java",
        """
        package test;

        import java.util.function.UnaryOperator;
        import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

        @SimpleBuilder
        public record CollisionDto(String test, UnaryOperator<String> mapTest) {}
        """);
  }

  private URLClassLoader compileRuntimeDifferentType() throws Exception {
    return compileRuntimeSource(
        "DifferentTypeDto.java",
        """
        package test;

        import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;

        @SimpleBuilder
        public record DifferentTypeDto(String test, String mapTest) {}
        """);
  }

  private URLClassLoader compileRuntimeNullResult() throws Exception {
    return compileRuntimeSources(
        Map.of(
            "NotNull.java",
            """
            package jakarta.validation.constraints;

            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.FIELD, ElementType.PARAMETER})
            public @interface NotNull {}
            """,
            "NullResultDto.java",
            """
            package test;

            import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
            import jakarta.validation.constraints.NotNull;

            @SimpleBuilder
            public class NullResultDto {
              private String name;
              private int quantity;
              private String description;

              public String getName() { return name; }
              public void setName(@NotNull String name) { this.name = name; }
              public int getQuantity() { return quantity; }
              public void setQuantity(int quantity) { this.quantity = quantity; }
              public String getDescription() { return description; }
              public void setDescription(String description) { this.description = description; }
            }
            """));
  }

  private URLClassLoader compileRuntimeSource(String fileName, String source) throws Exception {
    return compileRuntimeSources(Map.of(fileName, source));
  }

  private URLClassLoader compileRuntimeSources(Map<String, String> sources) throws Exception {
    Path sourceDirectory = Files.createDirectories(tempDirectory.resolve("src/test"));
    Path classDirectory = Files.createDirectories(tempDirectory.resolve("classes"));
    for (Map.Entry<String, String> source : sources.entrySet()) {
      Files.writeString(sourceDirectory.resolve(source.getKey()), source.getValue());
    }

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
      fileManager.setLocation(
          javax.tools.StandardLocation.CLASS_OUTPUT, List.of(classDirectory.toFile()));
      Iterable<? extends JavaFileObject> sourceFiles =
          fileManager.getJavaFileObjects(
              sources.keySet().stream()
                  .map(sourceDirectory::resolve)
                  .map(Path::toFile)
                  .toArray(java.io.File[]::new));
      List<String> options =
          List.of(
              "-classpath",
              System.getProperty("java.class.path"),
              "-processorpath",
              System.getProperty("java.class.path"),
              "-Asimplebuilder.generateMapperHelpers=ENABLED");
      JavaCompiler.CompilationTask task =
          compiler.getTask(null, fileManager, null, options, null, sourceFiles);
      task.setProcessors(List.<Processor>of(new BuilderProcessor()));
      assertTrue(task.call(), "Runtime test source should compile");
    }

    return new URLClassLoader(
        new URL[] {classDirectory.toUri().toURL()},
        MapperHelperGeneratorTest.class.getClassLoader());
  }

  private static void invokeMapper(Object builder, UnaryOperator<String> mapper) {
    try {
      builder.getClass().getMethod("mapName", UnaryOperator.class).invoke(builder, mapper);
    } catch (ReflectiveOperationException ex) {
      throw new RuntimeException(ex);
    }
  }
}
