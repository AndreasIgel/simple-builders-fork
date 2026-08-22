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

package org.javahelpers.simple.builders.processor.analysis;

import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import org.javahelpers.simple.builders.core.annotations.SimpleBuilder;
import org.javahelpers.simple.builders.processor.model.core.BuilderConfiguration;
import org.javahelpers.simple.builders.processor.model.type.TypeName;
import org.javahelpers.simple.builders.processor.processing.ProcessingContext;

/**
 * Resolves whether a builder type may be referenced for a given DTO type.
 *
 * <p>This resolver centralizes the decision so that generators and enhancers can keep relying on
 * {@link TypeName#getBuilderType()} without knowing about package scoping rules.
 */
public class BuilderScopeResolver {
  private final ProcessingContext context;

  public BuilderScopeResolver(ProcessingContext context) {
    this.context = context;
  }

  /**
   * Returns whether the given package is inside the configured builder generation scope.
   *
   * <p>An empty generation scope means "unscoped" and returns {@code true} for every package.
   */
  public boolean isInGenerationScope(String packageName) {
    return context
        .getConfigurationReader()
        .getGlobalConfiguration()
        .isInGenerationScope(packageName);
  }

  /**
   * Returns whether the given package is inside the configured builder usage scope.
   *
   * <p>An empty usage scope means "unscoped" and returns {@code true} for every package.
   */
  public boolean isInUsageScope(String packageName) {
    return context.getConfiguration().isInUsageScope(packageName);
  }

  /**
   * Resolves the builder type that may be used for the referenced type, if any.
   *
   * <p>The decision is based on the configured {@code builderGenerationPackages} and {@code
   * builderUsagePackages} lists and preserves the existing opt-out rules for types that must not
   * have a builder reference.
   *
   * @param referencedType the type for which a builder reference may be emitted
   * @param resolverContext the processing context used for type lookup
   * @return the builder type to use, or empty if no reference is allowed
   */
  public Optional<TypeName> resolveUsableBuilderType(
      TypeElement referencedType, ProcessingContext resolverContext) {
    if (JavaLangAnalyser.findAnnotation(
            referencedType,
            org.javahelpers.simple.builders.core.annotations.Ignore4BuilderGeneration.class)
        .isPresent()) {
      return Optional.empty();
    }

    if (!hasBuilderTemplateAnnotation(referencedType)) {
      return Optional.empty();
    }

    TypeName candidate = JavaLangMapper.createBuilderTypeName(referencedType, resolverContext);

    // Trust builders that the current processing round will actually generate, regardless of
    // package scoping. This must be the first decision so a filtered local type is never trusted.
    if (resolverContext.isGeneratedType(referencedType)) {
      return Optional.of(candidate);
    }

    String packageName =
        JavaLangMapper.extractPackageName(referencedType.getQualifiedName().toString());

    // For external/precompiled types, use the globally configured generation scope as the source
    // of truth and the per-target usage scope for optional references.
    BuilderConfiguration globalConfig =
        resolverContext.getConfigurationReader().getGlobalConfiguration();
    BuilderConfiguration config = resolverContext.getConfiguration();

    Set<String> generationPackages = globalConfig.getBuilderGenerationPackagesSet();
    Set<String> usagePackages = config.getBuilderUsagePackagesSet();

    if (generationPackages.isEmpty() && usagePackages.isEmpty()) {
      return Optional.of(candidate);
    }

    if (isPackageIn(generationPackages, packageName)) {
      return Optional.of(candidate);
    }

    if (isPackageIn(usagePackages, packageName)
        && resolverContext.getTypeElement(candidate.getFullQualifiedName()) != null) {
      return Optional.of(candidate);
    }

    return Optional.empty();
  }

  private boolean hasBuilderTemplateAnnotation(TypeElement typeElement) {
    for (AnnotationMirror mirror : context.getAllAnnotationMirrors(typeElement)) {
      TypeElement annotationTypeElement = (TypeElement) mirror.getAnnotationType().asElement();
      if (annotationTypeElement != null
          && JavaLangAnalyser.findAnnotation(annotationTypeElement, SimpleBuilder.Template.class)
              .isPresent()) {
        return true;
      }
    }
    return false;
  }

  private static boolean isPackageIn(Set<String> packages, String packageName) {
    if (packageName == null || packages.isEmpty()) {
      return false;
    }
    for (String p : packages) {
      if (packageName.equals(p) || packageName.startsWith(p + ".")) {
        return true;
      }
    }
    return false;
  }
}
