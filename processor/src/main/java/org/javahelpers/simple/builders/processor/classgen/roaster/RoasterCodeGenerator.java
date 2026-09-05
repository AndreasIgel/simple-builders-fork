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

import static org.javahelpers.simple.builders.processor.classgen.roaster.RoasterMapper.mapType;
import static org.javahelpers.simple.builders.processor.classgen.roaster.RoasterMapper.resolveCodeTemplate;
import static org.javahelpers.simple.builders.processor.processing.logging.PerformanceTracker.*;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.javahelpers.simple.builders.core.enums.AccessModifier;
import org.javahelpers.simple.builders.core.enums.FormattingMode;
import org.javahelpers.simple.builders.processor.exceptions.BuilderException;
import org.javahelpers.simple.builders.processor.model.annotation.AnnotationDto;
import org.javahelpers.simple.builders.processor.model.annotation.InterfaceName;
import org.javahelpers.simple.builders.processor.model.core.ClassFieldDto;
import org.javahelpers.simple.builders.processor.model.core.GenerationTargetClassDto;
import org.javahelpers.simple.builders.processor.model.imports.ImportStatement;
import org.javahelpers.simple.builders.processor.model.javadoc.JavadocCodeBlockDto;
import org.javahelpers.simple.builders.processor.model.javadoc.JavadocDto;
import org.javahelpers.simple.builders.processor.model.javadoc.JavadocTagDto;
import org.javahelpers.simple.builders.processor.model.method.ConstructorDto;
import org.javahelpers.simple.builders.processor.model.method.MethodCodeDto;
import org.javahelpers.simple.builders.processor.model.method.MethodDto;
import org.javahelpers.simple.builders.processor.model.method.MethodParameterDto;
import org.javahelpers.simple.builders.processor.model.type.NestedTypeDto;
import org.javahelpers.simple.builders.processor.model.type.TypeName;
import org.javahelpers.simple.builders.processor.model.type.TypeNameArray;
import org.javahelpers.simple.builders.processor.processing.logging.PerformanceTracker;
import org.javahelpers.simple.builders.processor.processing.logging.ProcessingLogger;
import org.javahelpers.simple.builders.processor.util.ImportCollector;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.AnnotationSource;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.jboss.forge.roaster.model.source.ParameterSource;
import org.jboss.forge.roaster.model.source.TypeVariableSource;

/** Roaster-based code generator for builder source files. */
public class RoasterCodeGenerator {
  /** Processing environment for accessing filer and element utilities. */
  private final ProcessingEnvironment processingEnv;

  /** Logger for debug output during code generation. */
  private final ProcessingLogger logger;

  /** Performance tracker for sub-phase timing (Source Construction, File Writing). */
  private final PerformanceTracker performanceTracker;

  /** Cached formatters per formatting mode (at most 3 instances, created lazily). */
  private final EnumMap<FormattingMode, RoasterSourceFormatter> formatterCache =
      new EnumMap<>(FormattingMode.class);

  /**
   * Constructor for RoasterCodeGenerator.
   *
   * @param processingEnv Processing environment for accessing filer and element utilities
   * @param logger Logger for debug output
   * @param tracker Performance tracker for sub-phase timing
   */
  public RoasterCodeGenerator(
      ProcessingEnvironment processingEnv, ProcessingLogger logger, PerformanceTracker tracker) {
    this.processingEnv = processingEnv;
    this.logger = logger;
    this.performanceTracker = tracker;
  }

  /**
   * Returns a cached or newly created {@link RoasterSourceFormatter} for the given mode.
   *
   * <p>At most 3 formatter instances exist (one per {@link FormattingMode} enum value), created
   * lazily on first use.
   *
   * @param mode the formatting mode
   * @return a cached or new formatter instance
   */
  private RoasterSourceFormatter getFormatter(FormattingMode mode) {
    return formatterCache.computeIfAbsent(mode, m -> new RoasterSourceFormatter(logger, m));
  }

  /**
   * Generates a class from the given class definition.
   *
   * @param classDef DTO containing all information to create the class
   * @throws BuilderException if there is an error in source code generation
   */
  public void generateClass(GenerationTargetClassDto classDef) throws BuilderException {
    logger.debugStartOperation(
        "Code generation for class: %s", classDef.getTypeName().getClassName());

    String sourceCode;
    try {
      performanceTracker.startPhase();
      JavaClassSource source = buildClassSource(classDef);
      performanceTracker.startPhase();
      String unformatted = source.toUnformattedString();
      performanceTracker.endPhase(PHASE_STRING_GENERATION);
      performanceTracker.startPhase();
      sourceCode = formatSource(unformatted, classDef.getFormattingMode());
      // Roaster renders some java.lang annotations (e.g. @SuppressWarnings, @Deprecated with
      // members) with their FQN (@java.lang.SuppressWarnings) even though java.lang types don't
      // need qualification. Fix this by replacing @java.lang.Xxx with @Xxx for known annotations.
      sourceCode = sourceCode.replace("@java.lang.SuppressWarnings", "@SuppressWarnings");
      sourceCode = sourceCode.replace("@java.lang.Deprecated", "@Deprecated");
      performanceTracker.endPhase(PHASE_FORMATTING);
      performanceTracker.endPhase(PHASE_SOURCE_CONSTRUCTION);
    } catch (RuntimeException ex) {
      // Rendering failures (e.g. RoasterMapperException) are RuntimeExceptions. Convert them into
      // a BuilderException so callers can isolate the failure to this single class and keep
      // generating the remaining builders instead of aborting the whole processing round.
      throw new BuilderException(null, ex);
    }
    performanceTracker.startPhase();
    writeClassToFile(sourceCode, classDef);
    performanceTracker.endPhase(PHASE_FILE_WRITING);

    logger.debugEndOperation(
        "Successfully generated class: %s", classDef.getTypeName().getClassName());
  }

  private JavaClassSource buildClassSource(GenerationTargetClassDto classDef) {
    performanceTracker.startPhase();
    JavaClassSource source = createJavaClassSource(classDef);
    addClassMetadata(source, classDef);
    appendFields(source, classDef);
    appendConstructors(source, classDef);
    appendMethods(source, classDef);
    appendNestedTypes(source, classDef);
    applyClassAnnotations(source, classDef);
    performanceTracker.endPhase(PHASE_ELEMENT_BUILDING);
    return source;
  }

  private void applyClassAnnotations(JavaClassSource source, GenerationTargetClassDto classDef) {
    performanceTracker.startPhase();
    if (CollectionUtils.isNotEmpty(classDef.getClassAnnotations())) {
      // Adding class annotations
      applyAnnotations(source, classDef.getClassAnnotations());
      logger.debug("Class-level annotations added");
    }
    performanceTracker.endPhase(PHASE_CLASS_ANNOTATIONS);
  }

  private JavaClassSource createJavaClassSource(GenerationTargetClassDto classDef) {
    performanceTracker.startPhase();
    if (CollectionUtils.isNotEmpty(classDef.getGenerics())) {
      logger.debug("Class has %d generic type parameter(s)", classDef.getGenerics().size());
    }

    JavaClassSource source = Roaster.create(JavaClassSource.class);
    String packageName = classDef.getTypeName().getPackageName();
    if (StringUtils.isNotBlank(packageName)) {
      source.setPackage(packageName);
    } else {
      source.setDefaultPackage();
    }
    source.setName(classDef.getTypeName().getClassName());
    addGenericDeclarations(source, classDef.getGenerics());

    // Collect and add imports early (before elements are added)
    Set<ImportStatement> importStmts = ImportCollector.collectAndSortImports(classDef);
    for (ImportStatement importStmt : importStmts) {
      if (importStmt.isStatic()) {
        source.addImport(importStmt.getFullyQualifiedName()).setStatic(true);
      } else {
        source.addImport(importStmt.getFullyQualifiedName());
      }
    }

    logger.debug("JavaClassSource created");
    performanceTracker.endPhase(PHASE_CLASS_CREATION);
    return source;
  }

  private void addClassMetadata(JavaClassSource source, GenerationTargetClassDto classDef) {
    performanceTracker.startPhase();
    applyJavadoc(source, classDef.getClassJavadoc());
    applyVisibility(source, classDef.getClassAccessModifier());
    applySuperType(source, classDef.getSuperType());

    // Add interfaces
    for (InterfaceName interfaceName : classDef.getInterfaces()) {
      source.addInterface(RoasterMapper.mapInterfaceToTypeName(interfaceName));
    }

    logger.debug("Class metadata added");
    performanceTracker.endPhase(PHASE_CLASS_METADATA);
  }

  private void appendFields(JavaClassSource source, GenerationTargetClassDto classDef) {
    performanceTracker.startPhase();
    logger.debugStartOperation("Generating %d fields", classDef.getClassFields().size());

    for (ClassFieldDto fieldDto : classDef.getClassFields()) {
      appendField(source, fieldDto);
    }

    logger.debugEndOperation("Fields added: %d fields", source.getFields().size());
    performanceTracker.endPhase(PHASE_FIELDS);
  }

  private void appendField(JavaClassSource source, ClassFieldDto fieldDto) {
    FieldSource<JavaClassSource> field = source.addField();
    field.setName(fieldDto.getFieldName());
    field.setType(mapType(fieldDto.getFieldType()));
    applyVisibility(field, fieldDto.getVisibility());
    applyLiteralInitializer(field, fieldDto.getLiteralInitializer());
    applyJavadoc(field, fieldDto.getJavadoc());
  }

  private void appendConstructors(JavaClassSource source, GenerationTargetClassDto classDef) {
    performanceTracker.startPhase();
    logger.debugStartOperation("Generating %d constructors", classDef.getConstructors().size());

    for (ConstructorDto constructor : classDef.getConstructors()) {
      appendConstructor(source, constructor);
    }

    logger.debugEndOperation("Constructors added: %d", classDef.getConstructors().size());
    performanceTracker.endPhase(PHASE_CONSTRUCTORS);
  }

  private void appendConstructor(JavaClassSource source, ConstructorDto constructor) {
    MethodSource<JavaClassSource> method = source.addMethod();
    method.setConstructor(true);
    applyVisibility(method, constructor.getVisibility());
    for (MethodParameterDto param : constructor.getParameters()) {
      method.addParameter(mapType(param.getParameterType()), param.getParameterName());
    }
    applyJavadoc(method, constructor.getJavadoc());
    applyAnnotations(method, constructor.getAnnotations());
    applyCodeBlock(method, constructor.getMethodCodeDto());
  }

  private void appendMethods(JavaClassSource source, GenerationTargetClassDto classDef) {
    performanceTracker.startPhase();
    logger.debugStartOperation("Generating %d method candidates", classDef.getMethods().size());

    // Resolve method conflicts by signature and priority
    List<MethodDto> resolvedMethods = resolveMethodConflicts(classDef.getMethods());
    logger.debug("Resolved to %d methods after conflict resolution", resolvedMethods.size());

    for (MethodDto methodDto : resolvedMethods) {
      appendMethod(source, methodDto, false, false);
    }

    logger.debugEndOperation("Methods added: %d", resolvedMethods.size());
    performanceTracker.endPhase(PHASE_METHODS);
  }

  /**
   * Generic safety net for method conflict resolution, preventing the code generator from producing
   * invalid output (duplicate method signatures).
   *
   * <p>This is a generation-level check that operates on {@link MethodDto} (rendering-side DTO)
   * which no longer carries field-origin metadata. Builder-specific conflict resolution with
   * field-origin logging is performed earlier in {@link
   * org.javahelpers.simple.builders.processor.processing.BuilderDefinitionCreator#resolveMethodConflicts}.
   *
   * <p>Since conflicts should already be resolved by the builder-specific step, this safety net
   * simply keeps the first occurrence for any remaining duplicate signatures and logs a generic
   * warning.
   */
  private List<MethodDto> resolveMethodConflicts(List<MethodDto> methods) {
    MethodDto.MethodComparator comparator = new MethodDto.MethodComparator();

    // Sort methods by comparator for consistent ordering
    List<MethodDto> sortedMethods = methods.stream().sorted(comparator).toList();

    Map<String, MethodDto> signatureToMethod = new java.util.LinkedHashMap<>();

    for (MethodDto method : sortedMethods) {
      String signature = method.getSignatureKey();

      MethodDto existing = signatureToMethod.get(signature);
      if (existing == null) {
        signatureToMethod.put(signature, method);
      } else {
        logger.warning(
            "  Unexpected duplicate method signature: '%s' — keeping first occurrence (safety net)",
            signature);
      }
    }

    // Sort the final result for reproducible output
    return signatureToMethod.values().stream().sorted(comparator).toList();
  }

  private void appendMethod(
      JavaClassSource source,
      MethodDto methodDto,
      boolean nestedTypeMethod,
      boolean interfaceMethod) {
    MethodSource<JavaClassSource> method = source.addMethod();
    configureMethod(method, methodDto, nestedTypeMethod, interfaceMethod);
    applyCodeBlock(method, methodDto.getMethodCodeDto());
  }

  private void appendNestedTypes(JavaClassSource source, GenerationTargetClassDto classDef) {
    performanceTracker.startPhase();
    if (CollectionUtils.isNotEmpty(classDef.getNestedTypes())) {
      logger.debugStartOperation("Generating %d nested type(s)", classDef.getNestedTypes().size());
      for (NestedTypeDto nestedType : classDef.getNestedTypes()) {
        appendNestedType(source, nestedType);
        logger.debug("Generated nested type: %s", nestedType.getTypeName());
      }
      logger.debugEndOperation("Nested types added");
    }
    performanceTracker.endPhase(PHASE_NESTED_TYPES);
  }

  private void appendNestedType(JavaClassSource source, NestedTypeDto nestedType) {
    JavaSource<?> nestedSource =
        nestedType.getKind() == NestedTypeDto.NestedTypeKind.INTERFACE
            ? source.addNestedType(JavaInterfaceSource.class)
            : source.addNestedType(JavaClassSource.class);
    nestedSource.setName(nestedType.getTypeName());
    applyVisibility(nestedSource, nestedType.getVisibility());
    applyAnnotations(nestedSource, nestedType.getAnnotations());
    applyJavadoc(nestedSource, nestedType.getJavadoc());
    boolean isInterface = nestedType.getKind() == NestedTypeDto.NestedTypeKind.INTERFACE;
    for (MethodDto methodDto : nestedType.getMethods()) {
      appendNestedMethod(nestedSource, methodDto, isInterface);
    }
  }

  private void appendNestedMethod(JavaSource<?> source, MethodDto methodDto, boolean isInterface) {
    org.jboss.forge.roaster.model.source.MethodHolderSource<?> methodHolder =
        (org.jboss.forge.roaster.model.source.MethodHolderSource<?>) source;
    MethodSource<?> method = methodHolder.addMethod();
    configureMethod(method, methodDto, true, isInterface);
    if (methodDto.getMethodCodeDto() != null
        && StringUtils.isNotBlank(methodDto.getMethodCodeDto().getCodeFormat())) {
      method.setBody(resolveCodeTemplate(methodDto.getMethodCodeDto()));
    } else {
      method.setAbstract(true);
      method.setBody("");
    }
  }

  private void applyVisibility(
      org.jboss.forge.roaster.model.source.VisibilityScopedSource<?> source,
      AccessModifier modifier) {
    if (modifier == null) {
      source.setPackagePrivate();
      return;
    }
    switch (modifier) {
      case PUBLIC -> source.setPublic();
      case PROTECTED -> source.setProtected();
      case PRIVATE -> source.setPrivate();
      case PACKAGE_PRIVATE -> source.setPackagePrivate();
      case DEFAULT -> source.setPackagePrivate(); // DEFAULT maps to PACKAGE_PRIVATE
    }
  }

  private void applySuperType(JavaClassSource source, TypeName superType) {
    if (superType != null) {
      source.setSuperType(mapType(superType));
    }
  }

  private void applyLiteralInitializer(
      FieldSource<JavaClassSource> field, String literalInitializer) {
    if (literalInitializer != null) {
      field.setLiteralInitializer(literalInitializer);
    }
  }

  private void addGenericDeclarations(
      JavaClassSource source,
      List<org.javahelpers.simple.builders.processor.model.type.GenericParameterDto> generics) {
    if (CollectionUtils.isEmpty(generics)) {
      return;
    }
    for (org.javahelpers.simple.builders.processor.model.type.GenericParameterDto generic :
        generics) {
      TypeVariableSource<JavaClassSource> typeVariable = source.addTypeVariable(generic.getName());
      if (CollectionUtils.isNotEmpty(generic.getUpperBounds())) {
        typeVariable.setBounds(
            generic.getUpperBounds().stream().map(RoasterMapper::mapType).toArray(String[]::new));
      }
    }
  }

  private void addGenericDeclarations(
      MethodSource<?> source,
      List<org.javahelpers.simple.builders.processor.model.type.GenericParameterDto> generics) {
    if (CollectionUtils.isEmpty(generics)) {
      return;
    }
    for (org.javahelpers.simple.builders.processor.model.type.GenericParameterDto generic :
        generics) {
      TypeVariableSource<?> typeVariable = source.addTypeVariable(generic.getName());
      if (CollectionUtils.isNotEmpty(generic.getUpperBounds())) {
        typeVariable.setBounds(
            generic.getUpperBounds().stream().map(RoasterMapper::mapType).toArray(String[]::new));
      }
    }
  }

  private void applyCodeBlock(MethodSource<?> method, MethodCodeDto codeDto) {
    if (!codeDto.hasCode()) {
      // If implementation is missing, an empty body needs to be set
      method.setBody("");
      return;
    }
    method.setBody(resolveCodeTemplate(codeDto));
  }

  private void applyJavadoc(
      org.jboss.forge.roaster.model.source.JavaDocCapableSource<?> source, JavadocDto javadoc) {
    if (javadoc == null || !javadoc.hasContent()) {
      return;
    }

    // Note: source.getJavaDoc() never returns null - Roaster creates the JavaDoc if it doesn't
    // exist
    // The early return above ensures we only create JavaDoc when there's actual content to set

    // Set description text
    if (StringUtils.isNotBlank(javadoc.getDescription())) {
      source.getJavaDoc().setText(javadoc.getDescription());
    }

    // Remove existing tags and add new ones
    source.getJavaDoc().removeAllTags();
    for (JavadocTagDto tag : javadoc.getTags()) {
      if (tag.hasValue()) {
        source.getJavaDoc().addTagValue(tag.getFullTagName(), tag.tagValue());
      } else {
        source.getJavaDoc().addTagValue(tag.getFullTagName(), "");
      }
    }

    // Render the code example block
    JavadocCodeBlockDto codeBlock = javadoc.getExampleUsageCodeBlock();
    if (codeBlock != null && codeBlock.hasCode()) {
      // Resolve placeholders in the code block
      String resolvedCode = resolveCodeTemplate(codeBlock);
      // Pre-prefix every line of the code body with " * " so it survives Roaster's
      // preformatted-block handling (Roaster does not auto-add asterisk prefix inside <pre>).
      String prefixedCode =
          Arrays.stream(resolvedCode.split("\n", -1))
              .map(line -> " * " + line)
              .collect(Collectors.joining("\n"));
      // Add the code example to the Javadoc with a blank line separator before <h4>.
      String currentText = source.getJavaDoc().getText();
      String exampleText = "<h4>Example:</h4><pre>{@code\n" + prefixedCode + "\n * }</pre>";
      if (StringUtils.isNotBlank(currentText)) {
        source.getJavaDoc().setText(currentText + "\n\n" + exampleText);
      } else {
        source.getJavaDoc().setText(exampleText);
      }
    }
  }

  private void applyAnnotations(
      org.jboss.forge.roaster.model.source.AnnotationTargetSource<?, ?> source,
      java.util.Collection<AnnotationDto> annotations) {
    if (CollectionUtils.isEmpty(annotations)) {
      return;
    }
    for (AnnotationDto annotationDto : annotations) {
      TypeName type = annotationDto.getAnnotationType();
      // Use the simple name for java.lang annotations (e.g. @SuppressWarnings, @Deprecated) so
      // Roaster renders them without the java.lang prefix. Other annotations use their FQN.
      String annotationName =
          "java.lang".equals(type.getPackageName())
              ? type.getClassName()
              : type.getFullQualifiedName();
      AnnotationSource<?> annotation = source.addAnnotation(annotationName);
      for (Map.Entry<String, String> member : annotationDto.getMembers().entrySet()) {
        if ("value".equals(member.getKey())) {
          annotation.setLiteralValue(member.getValue());
        } else {
          annotation.setLiteralValue(member.getKey(), member.getValue());
        }
      }
    }
  }

  private void configureMethod(
      MethodSource<?> method,
      MethodDto methodDto,
      boolean nestedTypeMethod,
      boolean interfaceMethod) {
    method.setName(methodDto.getMethodName());
    if (methodDto.getReturnType() == null) {
      method.setReturnTypeVoid();
    } else {
      method.setReturnType(mapType(methodDto.getReturnType()));
    }

    if (nestedTypeMethod) {
      boolean hasBody =
          interfaceMethod
              && methodDto.getMethodCodeDto() != null
              && StringUtils.isNotBlank(methodDto.getMethodCodeDto().getCodeFormat());
      if (hasBody) {
        method.setDefault(true);
      } else {
        method.setPublic();
      }
    } else {
      applyVisibility(method, methodDto.getModifier().orElse(null));
      method.setStatic(methodDto.isStatic());
    }

    addGenericDeclarations(method, methodDto.getGenericParameters());
    applyJavadoc(method, methodDto.getJavadoc());
    applyAnnotations(method, methodDto.getAnnotations());

    for (int i = 0; i < methodDto.getParameters().size(); i++) {
      MethodParameterDto paramDto = methodDto.getParameters().get(i);
      boolean lastParameter = i == methodDto.getParameters().size() - 1;
      addParameter(method, paramDto, lastParameter);
    }
  }

  private void addParameter(
      MethodSource<?> method, MethodParameterDto paramDto, boolean lastParameter) {
    boolean varArgs = lastParameter && paramDto.getParameterType() instanceof TypeNameArray;
    TypeName parameterType =
        varArgs
            ? ((TypeNameArray) paramDto.getParameterType()).getTypeOfArray()
            : paramDto.getParameterType();
    ParameterSource<?> parameter =
        method.addParameter(mapType(parameterType), paramDto.getParameterName());
    if (varArgs) {
      parameter.setVarArgs(true);
    }
    applyAnnotations(parameter, paramDto.getAnnotations());
  }

  private String formatSource(String rawSource, FormattingMode mode) {
    return getFormatter(mode).format(rawSource);
  }

  private void writeClassToFile(String sourceCode, GenerationTargetClassDto classDef)
      throws BuilderException {
    logger.debug(
        "Writing class to file: %s.%s",
        classDef.getTypeName().getPackageName(), classDef.getTypeName().getClassName());

    String qualifiedName = classDef.getTypeName().getFullQualifiedName();
    if (builderClassAlreadyExists(qualifiedName)) {
      throw new BuilderException(
          null,
          """
              Builder class '%s' already exists. This may be a manually written builder or a previously generated builder.
              To resolve this:
              1. If you have a manual builder, consider renaming it or removing @SimpleBuilder from the DTO
              2. If this is from a previous compilation, clean and rebuild the project
              3. Check that you're not trying to generate multiple builders for the same DTO
              """
              .formatted(qualifiedName));
    }

    try {
      JavaFileObject file = processingEnv.getFiler().createSourceFile(qualifiedName);
      try (Writer writer = file.openWriter()) {
        writer.write(sourceCode);
      }
    } catch (IOException ex) {
      String message = ex.getMessage();
      String errorMessage =
          """
          Unable to create builder class '%s': %s.
          Check the build environment and ensure all necessary directories are accessible.
          """
              .formatted(
                  qualifiedName, StringUtils.isNotBlank(message) ? message : "Unknown error");
      throw new BuilderException(null, errorMessage);
    }
  }

  /**
   * Checks if a builder class already exists by attempting to find the type element.
   *
   * @param qualifiedName the fully qualified name of the class to check
   * @return true if the class already exists, false otherwise
   */
  private boolean builderClassAlreadyExists(String qualifiedName) {
    try {
      TypeElement existingType = processingEnv.getElementUtils().getTypeElement(qualifiedName);
      return existingType != null;
    } catch (Exception e) {
      logger.debug(
          "Error checking if builder class '%s' already exists: %s",
          qualifiedName, StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : "No message");
      return false;
    }
  }
}
