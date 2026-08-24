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
import javax.lang.model.element.TypeElement;
import org.javahelpers.simple.builders.core.annotations.Ignore4BuilderGeneration;
import org.javahelpers.simple.builders.processor.analysis.JavaLangAnalyser;
import org.javahelpers.simple.builders.processor.model.core.BuilderConfiguration;
import org.javahelpers.simple.builders.processor.model.type.TypeName;
import org.javahelpers.simple.builders.processor.processing.ProcessingContext;

/**
 * Central resolver that decides whether a builder may be referenced for a given type.
 *
 * <p>This resolver is independent of generator/enhancer code; it only relies on the configured
 * {@code builderGenerationPackages} and {@code builderUsagePackages} scopes and on the availability
 * of the builder type on the classpath.
 */
public final class BuilderScopeResolver {

  /**
   * Creates a new resolver for the given processing context.
   *
   * @param context the processing context providing configuration and type utilities
   */
  public BuilderScopeResolver(ProcessingContext context) {
    // Resolver is stateless; context is passed per resolution call to stay keyed to the
    // current target configuration.
  }

  /**
   * Resolves the builder type to use for the given referenced type, if any.
   *
   * <p>The decision follows these rules:
   *
   * <ol>
   *   <li>If the referenced type is opted out with {@code @Ignore4BuilderGeneration}, or is not
   *       annotated with {@code @SimpleBuilder}, no builder may be used.
   *   <li>If both scopes are empty/unset, the candidate builder is returned for full backward
   *       compatibility (current behavior, no type search).
   *   <li>If the referenced type's package is in {@code builderGenerationPackages}, the candidate
   *       builder is returned without a type-existence search.
   *   <li>If the referenced type's package is in {@code builderUsagePackages} (but not in the
   *       generation scope), the candidate builder is returned only if the builder type can be
   *       resolved on the classpath.
   *   <li>Otherwise no builder may be referenced.
   * </ol>
   *
   * @param referencedType the type element being referenced as a field or collection element
   * @param context the processing context for type lookup
   * @return the builder type to reference, or empty if no builder should be referenced
   */
  public Optional<TypeName> resolveUsableBuilderType(
      TypeElement referencedType, ProcessingContext context) {
    if (!hasSimpleBuilderAnnotation(referencedType)
        || JavaLangAnalyser.findAnnotation(referencedType, Ignore4BuilderGeneration.class)
            .isPresent()) {
      return Optional.empty();
    }

    BuilderConfiguration config = context.getConfiguration();
    TypeName candidate = JavaLangMapper.createBuilderTypeName(referencedType, context);
    String packageName = JavaLangMapper.extractPackageName(referencedType.getQualifiedName().toString());

    Set<String> generationPackages = config.getBuilderGenerationPackagesSet();
    Set<String> usagePackages = config.getBuilderUsagePackagesSet();

    // Both scopes unset → full backward compatibility, no type search.
    if (generationPackages.isEmpty() && usagePackages.isEmpty()) {
      return Optional.of(candidate);
    }

    // Generation scope: trusted types whose builders are generated in this compilation.
    if (!generationPackages.isEmpty() && isInScope(packageName, generationPackages)) {
      return Optional.of(candidate);
    }

    // Usage scope: types whose builders may already be compiled; verify existence.
    if (!usagePackages.isEmpty() && isInScope(packageName, usagePackages)) {
      return context.getTypeElement(candidate.getFullQualifiedName()) != null
          ? Optional.of(candidate)
          : Optional.empty();
    }

    return Optional.empty();
  }

  private boolean hasSimpleBuilderAnnotation(TypeElement typeElement) {
    if (typeElement == null) {
      return false;
    }
    return JavaLangAnalyser.findAnnotation(
            typeElement, org.javahelpers.simple.builders.core.annotations.SimpleBuilder.class)
        .isPresent();
  }

  private static boolean isInScope(String packageName, Set<String> scopes) {
    if (scopes.isEmpty()) {
      return false;
    }
    for (String scope : scopes) {
      if (packageName.equals(scope) || packageName.startsWith(scope + ".")) {
        return true;
      }
    }
    return false;
  }
}
