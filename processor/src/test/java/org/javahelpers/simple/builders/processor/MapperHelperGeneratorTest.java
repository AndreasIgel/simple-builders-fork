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
import static org.junit.jupiter.api.Assertions.assertEquals;
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

/** Tests for opt-in {@code mapX(UnaryOperator<T>)} helper generation. */
class MapperHelperGeneratorTest {

  @TempDir Path tempDirectory;

  @Test
  void mapperHelpers_DefaultDisabled_DoesNotGenerateMethods() {
    Compilation compilation = ProcessorTestUtils.createCompiler().compile(personSource());

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
        generated.contains(".mapName(value -> value);"),
        "Mapper methods should contribute an example chain fragment");
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

  private URLClassLoader compileRuntimeSource(String fileName, String source) throws Exception {
    Path sourceDirectory = Files.createDirectories(tempDirectory.resolve("src/test"));
    Path classDirectory = Files.createDirectories(tempDirectory.resolve("classes"));
    Path sourceFile = sourceDirectory.resolve(fileName);
    Files.writeString(sourceFile, source);

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
      fileManager.setLocation(
          javax.tools.StandardLocation.CLASS_OUTPUT, List.of(classDirectory.toFile()));
      Iterable<? extends JavaFileObject> sources =
          fileManager.getJavaFileObjects(sourceFile.toFile());
      List<String> options =
          List.of(
              "-classpath",
              System.getProperty("java.class.path"),
              "-processorpath",
              System.getProperty("java.class.path"),
              "-Asimplebuilder.generateMapperHelpers=ENABLED");
      JavaCompiler.CompilationTask task =
          compiler.getTask(null, fileManager, null, options, null, sources);
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
