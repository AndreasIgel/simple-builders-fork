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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.javahelpers.simple.builders.core.enums.FormattingMode;
import org.javahelpers.simple.builders.processor.processing.logging.ProcessingLogger;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.util.FormatterProfileReader;

/**
 * Handles formatting of Java source code produced by Roaster's {@code toUnformattedString()}.
 *
 * <p>This formatter is specifically designed for Roaster's output format and addresses its quirks
 * (tab indentation, concatenated imports/javadoc, missing javadoc asterisk prefixes). It is not a
 * general-purpose source code formatter.
 *
 * <p>Supports three modes controlled by {@link FormattingMode}: full Eclipse JDT formatting,
 * lightweight cosmetic post-processing, or no formatting at all. See the {@link FormattingMode}
 * enum for details on each mode.
 *
 * <p>The Eclipse formatter profile can be overridden with a file system path or classpath resource.
 */
public final class RoasterSourceFormatter {

  static final String DEFAULT_FORMATTER_PROFILE_RESOURCE = "eclipse-java-format.xml";
  private static final int SPACES_PER_TAB = 2;

  private final ProcessingLogger logger;
  private final FormattingMode formattingMode;
  private final Properties formatterProperties;
  private final boolean formatterProfileAvailable;
  private final String formatterProfileResource;

  /**
   * Creates a formatter instance.
   *
   * @param logger logger for warnings (e.g. formatter profile load failures)
   * @param formattingMode the formatting mode to use for source code post-processing
   * @throws NullPointerException if logger or formattingMode is null
   */
  public RoasterSourceFormatter(ProcessingLogger logger, FormattingMode formattingMode) {
    this(logger, formattingMode, null);
  }

  /**
   * Creates a formatter instance with an optional Eclipse formatter profile override.
   *
   * @param logger logger for warnings (e.g. formatter profile load failures)
   * @param formattingMode the formatting mode to use for source code post-processing
   * @param formatterProfile file system path or classpath resource for the Eclipse formatter
   *     profile; blank values use the bundled default
   * @throws NullPointerException if logger or formattingMode is null
   */
  public RoasterSourceFormatter(
      ProcessingLogger logger, FormattingMode formattingMode, String formatterProfile) {
    this.logger = Objects.requireNonNull(logger, "logger must not be null");
    this.formattingMode = Objects.requireNonNull(formattingMode, "formattingMode must not be null");
    this.formatterProfileResource =
        StringUtils.defaultIfBlank(formatterProfile, DEFAULT_FORMATTER_PROFILE_RESOURCE);
    this.formatterProperties = loadFormatterProperties();
    this.formatterProfileAvailable = !formatterProperties.isEmpty();
    if (formattingMode == FormattingMode.JDT && !formatterProfileAvailable) {
      logger.warning(
          "simple-builders: JDT formatting requested but Eclipse formatter profile is unavailable; falling back to lightweight formatting.");
    }
  }

  /**
   * Formats the given raw source code according to the configured {@link FormattingMode}.
   *
   * <p>If {@link FormattingMode#NONE}, the raw source is returned as-is. If {@link
   * FormattingMode#LIGHTWEIGHT} or if the Eclipse formatter profile is unavailable, the lightweight
   * formatter is used. Otherwise the full Eclipse JDT formatter is applied.
   *
   * @param rawSource the unformatted Java source code from Roaster's {@code toUnformattedString()}
   * @return the formatted source code
   */
  public String format(String rawSource) {
    if (formattingMode == FormattingMode.NONE) {
      return rawSource;
    }
    if (formattingMode == FormattingMode.LIGHTWEIGHT) {
      return lightweightFormat(rawSource);
    }
    if (!formatterProfileAvailable) {
      return lightweightFormat(rawSource);
    }
    return Roaster.format(formatterProperties, rawSource);
  }

  /**
   * Lightweight post-processing of Roaster's unformatted output.
   *
   * <p>Applies minimal cosmetic fixes that are much cheaper than the full Eclipse JDT formatter:
   *
   * <ul>
   *   <li>Convert tab indentation to 2-space indentation
   *   <li>Remove duplicate blank lines (collapse 2+ consecutive blanks to 1)
   *   <li>Insert newline between a trailing import and an adjacent {@code /**} javadoc opening
   *   <li>Add missing {@code " * "} prefixes to javadoc body lines
   *   <li>Normalize javadoc body indentation to match the enclosing member
   * </ul>
   *
   * @param source the raw source from Roaster's {@code toUnformattedString()}
   * @return the lightly post-processed source
   */
  String lightweightFormat(String source) {
    String[] rawLines = source.split("\n", -1);
    StringBuilder output = new StringBuilder(source.length());
    JavadocState javadocState = new JavadocState();
    boolean prevBlank = false;

    for (String line : rawLines) {
      // 1. Convert leading tabs to spaces
      String converted = convertTabsToSpaces(line);

      // 2. Split concatenated lines (import;class, }})
      for (String splitLine : splitConcatenatedLines(converted)) {
        processLine(splitLine, output, javadocState, prevBlank);
        prevBlank = splitLine.isBlank();
      }
    }
    return output.toString();
  }

  private void processLine(
      String converted, StringBuilder output, JavadocState javadocState, boolean prevBlank) {
    // Split import/code concatenated with /**
    if (!javadocState.inJavadoc) {
      converted = splitConcatenatedJavadocOpen(converted, output, javadocState, prevBlank);
    }

    // Fix javadoc asterisk prefixes and indentation
    if (javadocState.inJavadoc) {
      converted = fixJavadocLine(converted, javadocState);
    }

    // Collapse consecutive blank lines and append
    boolean isBlank = converted.isBlank();
    if (isBlank && prevBlank) {
      return;
    }
    if (!output.isEmpty()) {
      output.append('\n');
    }
    output.append(converted);
  }

  /**
   * Splits a single line that Roaster concatenated without newlines.
   *
   * <p>Roaster's {@code toUnformattedString()} sometimes glues together:
   *
   * <ul>
   *   <li>The last import and the class declaration: {@code "import x.Y;public class Foo {"}
   *       <li>Closing braces at end of file: {@code " } }"}
   * </ul>
   *
   * <p>This method splits such lines at:
   *
   * <ol>
   *   <li>Semicolon followed by a Java declaration keyword ({@code public}, {@code private}, {@code
   *       protected}, {@code class}, {@code interface}, {@code enum}, {@code record}, {@code
   *       abstract}, {@code final}, {@code import}, {@code package})
   *   <li>A closing brace ({@code }}) followed by another closing brace (with optional whitespace)
   * </ol>
   *
   * @param line the potentially concatenated line
   * @return a list of split lines (or a singleton list if no splitting was needed)
   */
  private java.util.List<String> splitConcatenatedLines(String line) {
    // Fast path: no semicolons or closing braces, nothing to split
    if (!line.contains(";") && !line.contains("}")) {
      return java.util.List.of(line);
    }

    java.util.List<String> result = new java.util.ArrayList<>();
    String remaining = line;

    while (true) {
      int splitPos = findSplitPosition(remaining);
      if (splitPos < 0) {
        result.add(remaining);
        break;
      }
      result.add(remaining.substring(0, splitPos).stripTrailing());
      remaining = remaining.substring(splitPos).strip();
    }
    return result;
  }

  /**
   * Finds the position at which a line should be split for the next concatenated segment.
   *
   * @return the start index of the next segment, or -1 if no split is needed
   */
  private int findSplitPosition(String line) {
    int pos = findSemicolonKeywordSplit(line);
    if (pos >= 0) {
      return pos;
    }
    return findClosingBraceSplit(line);
  }

  /**
   * Finds a semicolon followed by a Java declaration keyword and returns the position of the
   * keyword.
   */
  private int findSemicolonKeywordSplit(String line) {
    int semiIdx = 0;
    while ((semiIdx = line.indexOf(';', semiIdx)) >= 0) {
      int afterSemi = skipWhitespace(line, semiIdx + 1);
      if (matchesDeclarationKeyword(line, afterSemi)) {
        return afterSemi;
      }
      semiIdx++;
    }
    return -1;
  }

  /**
   * Finds a closing brace followed by another closing brace (with optional whitespace between) and
   * returns the position of the second brace.
   */
  private int findClosingBraceSplit(String line) {
    int braceIdx = 0;
    while ((braceIdx = line.indexOf('}', braceIdx)) >= 0) {
      int afterBrace = skipWhitespace(line, braceIdx + 1);
      if (afterBrace < line.length() && line.charAt(afterBrace) == '}') {
        return afterBrace;
      }
      braceIdx++;
    }
    return -1;
  }

  /** Skips whitespace starting at the given index and returns the first non-whitespace position. */
  private int skipWhitespace(String line, int start) {
    int pos = start;
    while (pos < line.length() && Character.isWhitespace(line.charAt(pos))) {
      pos++;
    }
    return pos;
  }

  /** Checks whether the text at the given position starts with a Java declaration keyword. */
  private boolean matchesDeclarationKeyword(String text, int pos) {
    if (pos >= text.length()) {
      return false;
    }
    String[] keywords = {
      "public",
      "private",
      "protected",
      "class",
      "interface",
      "enum",
      "record",
      "abstract",
      "final",
      "import",
      "package"
    };
    for (String kw : keywords) {
      if (text.startsWith(kw, pos)) {
        int endPos = pos + kw.length();
        // Ensure the keyword is a complete word (followed by whitespace or other non-word char)
        if (endPos >= text.length() || !Character.isJavaIdentifierPart(text.charAt(endPos))) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Handles a line that may contain a {@code /**} javadoc opening concatenated with preceding code
   * (e.g. {@code "import ...;/**"}). If found, emits the preceding part as its own line and updates
   * the javadoc state. Returns the remaining line to process (either the original or just the
   * {@code /**} part).
   */
  private String splitConcatenatedJavadocOpen(
      String converted, StringBuilder output, JavadocState javadocState, boolean prevBlank) {
    int jdStart = converted.indexOf("/**");
    if (jdStart < 0) {
      return converted;
    }
    String afterOpen = converted.substring(jdStart + 3);
    if (afterOpen.contains("*/")) {
      return converted;
    }
    String before = converted.substring(0, jdStart).stripTrailing();
    String indent = getLeadingIndent(converted);
    javadocState.inJavadoc = true;
    javadocState.indent = indent.length();
    if (!before.isEmpty()) {
      boolean beforeBlank = before.isBlank();
      if (!(beforeBlank && prevBlank)) {
        if (!output.isEmpty()) {
          output.append('\n');
        }
        output.append(before);
      }
      return indent + "/**";
    }
    return converted;
  }

  /**
   * Fixes javadoc body lines by adding missing {@code " * "} prefixes and normalizing indentation.
   * Updates the javadoc state when the closing javadoc delimiter is encountered.
   */
  private String fixJavadocLine(String converted, JavadocState javadocState) {
    String stripped = converted.strip();
    if (stripped.startsWith("/**")) {
      return converted;
    }
    if (stripped.endsWith("*/")) {
      if (!stripped.equals("*/") && !stripped.startsWith("*")) {
        String content = converted.substring(0, converted.indexOf("*/")).strip();
        converted = " ".repeat(javadocState.indent) + " * " + content + " */";
      }
      javadocState.inJavadoc = false;
      return converted;
    }
    if (stripped.isBlank()) {
      return " ".repeat(javadocState.indent) + " *";
    }
    if (!stripped.startsWith("*")) {
      return " ".repeat(javadocState.indent) + " * " + stripped;
    }
    return converted;
  }

  /** Mutable state for javadoc processing within {@link #lightweightFormat(String)}. */
  private static final class JavadocState {
    boolean inJavadoc = false;
    int indent = 0;
  }

  /**
   * Convert leading tab characters to spaces (2 spaces per tab).
   *
   * @param line the line to convert
   * @return the line with leading tabs replaced by spaces, or the original line if no tabs
   */
  private String convertTabsToSpaces(String line) {
    int wsEnd = 0;
    while (wsEnd < line.length() && (line.charAt(wsEnd) == ' ' || line.charAt(wsEnd) == '\t')) {
      wsEnd++;
    }
    if (wsEnd == 0) {
      return line;
    }
    boolean hasTab = false;
    for (int j = 0; j < wsEnd; j++) {
      if (line.charAt(j) == '\t') {
        hasTab = true;
        break;
      }
    }
    if (!hasTab) {
      return line;
    }
    StringBuilder sb = new StringBuilder(line.length() + wsEnd);
    for (int j = 0; j < wsEnd; j++) {
      char c = line.charAt(j);
      if (c == '\t') {
        sb.append(" ".repeat(SPACES_PER_TAB));
      } else {
        sb.append(c);
      }
    }
    sb.append(line, wsEnd, line.length());
    return sb.toString();
  }

  /**
   * Extract leading whitespace (spaces and tabs) from a line.
   *
   * @param line the line to extract indent from
   * @return the leading whitespace string
   */
  private static String getLeadingIndent(String line) {
    int end = 0;
    while (end < line.length() && (line.charAt(end) == ' ' || line.charAt(end) == '\t')) {
      end++;
    }
    return line.substring(0, end);
  }

  private Properties loadFormatterProperties() {
    if (DEFAULT_FORMATTER_PROFILE_RESOURCE.equals(formatterProfileResource)) {
      return loadBundledProfile();
    }
    return loadConfiguredProfile(formatterProfileResource).orElseGet(this::loadBundledProfile);
  }

  /**
   * Loads the user-configured profile; warns and returns empty when it cannot be loaded.
   *
   * @param location file system path or classpath resource
   * @return the loaded formatter properties, or empty when loading fails
   */
  private Optional<Properties> loadConfiguredProfile(String location) {
    try {
      Optional<Properties> properties = readProfile(location);
      if (properties.isEmpty()) {
        logger.warning(
            "simple-builders: Eclipse formatter profile '%s' was not found as a file or classpath resource; falling back to the bundled profile.",
            location);
        return Optional.empty();
      }
      return properties;
    } catch (IOException | RuntimeException ex) {
      logger.warning(
          "simple-builders: Failed to load Eclipse formatter profile '%s': %s; falling back to the bundled profile.",
          location, describe(ex));
      return Optional.empty();
    }
  }

  /** Loads the bundled profile; warns and returns empty properties when it cannot be loaded. */
  private Properties loadBundledProfile() {
    try {
      Optional<Properties> properties = readProfile(DEFAULT_FORMATTER_PROFILE_RESOURCE);
      if (properties.isPresent()) {
        return properties.get();
      }
      logger.warning(
          "simple-builders: Bundled Eclipse formatter profile '%s' was not found on the processor classpath.",
          DEFAULT_FORMATTER_PROFILE_RESOURCE);
    } catch (IOException | RuntimeException ex) {
      logger.warning(
          "simple-builders: Failed to load bundled Eclipse formatter profile '%s': %s.",
          DEFAULT_FORMATTER_PROFILE_RESOURCE, describe(ex));
    }
    return new Properties();
  }

  /** Opens location as file, then classpath resource, and parses it; empty when not found. */
  private Optional<Properties> readProfile(String location) throws IOException {
    try (InputStream inputStream = openProfileStream(location)) {
      if (inputStream == null) {
        return Optional.empty();
      }
      return Optional.of(FormatterProfileReader.fromEclipseXml(inputStream).getDefaultProperties());
    }
  }

  private String describe(Exception ex) {
    return StringUtils.defaultIfBlank(ex.getMessage(), ex.getClass().getSimpleName());
  }

  private InputStream openProfileStream(String location) throws IOException {
    try {
      Path path = Path.of(location);
      if (Files.isRegularFile(path)) {
        return Files.newInputStream(path);
      }
    } catch (InvalidPathException ignored) {
      // Treat invalid paths as classpath resource names.
    }
    return RoasterSourceFormatter.class.getClassLoader().getResourceAsStream(location);
  }
}
