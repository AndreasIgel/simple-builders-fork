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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import org.javahelpers.simple.builders.core.enums.FormattingMode;
import org.javahelpers.simple.builders.processor.processing.logging.ProcessingLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link RoasterSourceFormatter}.
 *
 * <p>Tests cover:
 *
 * <ul>
 *   <li>{@code lightweightFormat()} edge cases: tab conversion, blank line collapsing, javadoc
 *       fixup, concatenated import/javadoc splitting
 *   <li>{@code format()} dispatch logic for NONE, LIGHTWEIGHT, and JDT modes
 *   <li>Fallback behavior when JDT formatter profile is unavailable
 * </ul>
 */
class RoasterSourceFormatterTest {

  /** A minimal ProcessingEnvironment stub that provides a no-op Messager. */
  private static final class TestProcessingEnv implements ProcessingEnvironment {
    final TestMessager messager = new TestMessager();

    @Override
    public Messager getMessager() {
      return messager;
    }

    @Override
    public Map<String, String> getOptions() {
      return Collections.emptyMap();
    }

    @Override
    public Elements getElementUtils() {
      return null;
    }

    @Override
    public Types getTypeUtils() {
      return null;
    }

    @Override
    public Filer getFiler() {
      return null;
    }

    @Override
    public SourceVersion getSourceVersion() {
      return SourceVersion.RELEASE_17;
    }

    @Override
    public Locale getLocale() {
      return Locale.getDefault();
    }
  }

  /** A minimal Messager that captures warnings. */
  private static final class TestMessager implements Messager {
    final List<String> warnings = new ArrayList<>();

    @Override
    public void printMessage(Diagnostic.Kind kind, CharSequence msg) {
      if (kind == Diagnostic.Kind.WARNING) {
        warnings.add(msg.toString());
      }
    }

    @Override
    public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e) {
      if (kind == Diagnostic.Kind.WARNING) {
        warnings.add(msg.toString());
      }
    }

    @Override
    public void printMessage(
        Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a) {
      if (kind == Diagnostic.Kind.WARNING) {
        warnings.add(msg.toString());
      }
    }

    @Override
    public void printMessage(
        Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a, AnnotationValue v) {
      if (kind == Diagnostic.Kind.WARNING) {
        warnings.add(msg.toString());
      }
    }
  }

  private TestProcessingEnv createProcessingEnv() {
    return new TestProcessingEnv();
  }

  private RoasterSourceFormatter createFormatter(FormattingMode mode) {
    return new RoasterSourceFormatter(new ProcessingLogger(createProcessingEnv()), mode);
  }

  @Test
  void format_noneMode_returnsRawSourceUnchanged() {
    RoasterSourceFormatter formatter = createFormatter(FormattingMode.NONE);
    // Input is intentionally misformatted so that any active formatter would change it;
    // NONE mode must return it verbatim.
    String raw =
        """
        package   test;
        public    class   Foo   {
        }""";
    assertEquals(raw, formatter.format(raw));
  }

  @Test
  void lightweightFormat_convertsTabsToSpaces() {
    RoasterSourceFormatter formatter = createFormatter(FormattingMode.LIGHTWEIGHT);
    String input =
        """
        package test;
        public class Foo {
        \tpublic void bar() {
        \t\treturn;
        \t}
        }
        """;
    String result = formatter.lightweightFormat(input);
    assertFalse(result.contains("\t"), "No tabs should remain in output");
    String expected =
        """
        package test;
        public class Foo {
          public void bar() {
            return;
          }
        }
        """;
    assertEquals(expected, result);
  }

  @Test
  void lightweightFormat_collapsesConsecutiveBlankLines() {
    RoasterSourceFormatter formatter = createFormatter(FormattingMode.LIGHTWEIGHT);
    String input =
        """
        package test;



        public class Foo {
        }
        """;
    String result = formatter.lightweightFormat(input);
    long blankCount = result.lines().filter(String::isBlank).count();
    assertEquals(1, blankCount, "Multiple consecutive blank lines should collapse to 1");
  }

  @Test
  void lightweightFormat_preservesSingleBlankLine() {
    RoasterSourceFormatter formatter = createFormatter(FormattingMode.LIGHTWEIGHT);
    String input =
        """
        package test;

        public class Foo {
        }
        """;
    String result = formatter.lightweightFormat(input);
    long blankCount = result.lines().filter(String::isBlank).count();
    assertEquals(1, blankCount, "Single blank line should be preserved");
  }

  @ParameterizedTest(name = "{2}")
  @MethodSource("javadocFormattingCases")
  void lightweightFormat_javadocFormatting(String input, String expected, String description) {
    RoasterSourceFormatter formatter = createFormatter(FormattingMode.LIGHTWEIGHT);
    String result = formatter.lightweightFormat(input);
    assertEquals(expected, result.strip(), "Case: " + description);
  }

  static Stream<Arguments> javadocFormattingCases() {
    return Stream.of(
        // Multi-line javadoc: body lines get ' * ' prefix, blank lines get ' *'
        Arguments.of(
            """
            package test;
            /**
            First line.

            Second line.
             */
            public class Foo {
            }
            """,
            """
            package test;
            /**
             * First line.
             *
             * Second line.
             */
            public class Foo {
            }""",
            "multi-line javadoc with blank line gets ' * ' prefixes"),
        // Already-prefixed lines are preserved
        Arguments.of(
            """
            package test;
            /**
             * Already has prefix.
             */
            public class Foo {
            }
            """,
            """
            package test;
            /**
             * Already has prefix.
             */
            public class Foo {
            }""",
            "already-prefixed lines keep ' * ' prefix"),
        // Star-only prefix (no space) is preserved as-is
        Arguments.of(
            """
            package test;
            /**
            *Body line without space.
             */
            public class Foo {
            }
            """,
            """
            package test;
            /**
            *Body line without space.
             */
            public class Foo {
            }""",
            "star-only prefix without space is preserved"),
        // Line ending with */ gets ' * ' prefix
        Arguments.of(
            """
            package test;
            /**
            Some description text */
            public class Foo {
            }
            """,
            """
            package test;
            /**
             * Some description text */
            public class Foo {
            }""",
            "line ending with */ gets ' * ' prefix"),
        // Inline one-line javadoc is preserved as-is
        Arguments.of(
            """
            package test;
            /** This is a one-line javadoc. */
            public class Foo {
            }
            """,
            """
            package test;
            /** This is a one-line javadoc. */
            public class Foo {
            }""",
            "inline one-line javadoc is preserved"),
        Arguments.of(
            """
            package test;
            /** Short. */
            public class Foo {
            }
            """,
            """
            package test;
            /** Short. */
            public class Foo {
            }""",
            "short inline javadoc is preserved"),
        Arguments.of(
            """
            package test;
            /** Multi word inline javadoc with several words. */
            public class Foo {
            }
            """,
            """
            package test;
            /** Multi word inline javadoc with several words. */
            public class Foo {
            }""",
            "multi-word inline javadoc is preserved"),
        // Javadoc tag lines get ' * ' prefix
        Arguments.of(
            """
            package test;
            /**
            Description.
            @param value the value
            @return the result
             */
            public class Foo {
            }
            """,
            """
            package test;
            /**
             * Description.
             * @param value the value
             * @return the result
             */
            public class Foo {
            }""",
            "javadoc tag lines get ' * ' prefix"),
        // Multiple javadoc blocks at different indentation levels
        Arguments.of(
            """
            package test;
            /**
            Class-level javadoc.
             */
            public class Foo {
              /**
              Method javadoc.
              */
              public void bar() {
              }
            }
            """,
            """
            package test;
            /**
             * Class-level javadoc.
             */
            public class Foo {
              /**
               * Method javadoc.
              */
              public void bar() {
              }
            }""",
            "multiple javadoc blocks at different indentation levels"),
        // Javadoc inside a method gets indented to match enclosing member
        Arguments.of(
            """
            package test;
            public class Foo {
              /**
              Body line.
              */
              public void bar() {
              }
            }
            """,
            """
            package test;
            public class Foo {
              /**
               * Body line.
              */
              public void bar() {
              }
            }""",
            "javadoc body lines indented to match enclosing member"),
        // Concatenated import and javadoc are split into separate lines
        Arguments.of(
            """
            package test;import java.util.List;/**
             * This is a javadoc.
             */
            public class Foo {
            }
            """,
            """
            package test;
            import java.util.List;
            /**
             * This is a javadoc.
             */
            public class Foo {
            }""",
            "concatenated import and javadoc are split"));
  }

  @Test
  void lightweightFormat_handlesEmptyInput() {
    RoasterSourceFormatter formatter = createFormatter(FormattingMode.LIGHTWEIGHT);
    String result = formatter.lightweightFormat("");
    assertEquals("", result, "Empty input should produce empty output");
  }

  @Test
  void lightweightFormat_handlesMixedTabsAndSpaces() {
    RoasterSourceFormatter formatter = createFormatter(FormattingMode.LIGHTWEIGHT);
    String input =
        """
        package test;
        public class Foo {
        \t  public void bar() {
        \t  \treturn;
        \t  }
        }
        """;
    String result = formatter.lightweightFormat(input);
    assertTrue(
        result.contains("    public void bar()"),
        "Mixed tab+space indentation should be converted (tab=2 spaces, then existing spaces)");
    assertTrue(!result.contains("\t"), "No tabs should remain in output");
  }

  @Test
  void lightweightFormat_splitsConcatenatedImportAndClassDeclaration() {
    RoasterSourceFormatter formatter = createFormatter(FormattingMode.LIGHTWEIGHT);
    String input =
        """
        package test;
        import java.util.List;public class Foo {
          private List<String> items;
        }
        """;
    String result = formatter.lightweightFormat(input);
    String[] lines = result.split("\n", -1);
    int importIdx = -1;
    int classIdx = -1;
    for (int i = 0; i < lines.length; i++) {
      if (lines[i].equals("import java.util.List;")) {
        importIdx = i;
      }
      if (lines[i].equals("public class Foo {")) {
        classIdx = i;
      }
    }
    assertTrue(importIdx >= 0, "Import should be on its own line");
    assertTrue(classIdx >= 0, "Class declaration should be on its own line");
    assertEquals(
        importIdx + 1,
        classIdx,
        "Class declaration should be on the line immediately after the import");
  }

  @Test
  void lightweightFormat_splitsConcatenatedClosingBraces() {
    RoasterSourceFormatter formatter = createFormatter(FormattingMode.LIGHTWEIGHT);
    String input =
        """
        package test;
        public class Foo {
          public void bar() {
            return;
          } }""";
    String result = formatter.lightweightFormat(input);
    String[] lines = result.split("\n", -1);
    int methodCloseIdx = -1;
    int classCloseIdx = -1;
    for (int i = 0; i < lines.length; i++) {
      if (lines[i].equals("  }")) {
        methodCloseIdx = i;
      }
      if (lines[i].equals("}")) {
        classCloseIdx = i;
      }
    }
    assertTrue(methodCloseIdx >= 0, "Method closing brace should be on its own line");
    assertTrue(classCloseIdx >= 0, "Class closing brace should be on its own line");
    assertEquals(
        methodCloseIdx + 1,
        classCloseIdx,
        "Class closing brace should be on the line immediately after method closing brace");
  }

  @Test
  void lightweightFormat_splitsConcatenatedPackageAndImport() {
    RoasterSourceFormatter formatter = createFormatter(FormattingMode.LIGHTWEIGHT);
    String input =
        """
        package test;import java.util.List;
        public class Foo {
        }
        """;
    String result = formatter.lightweightFormat(input);
    String[] lines = result.split("\n", -1);
    int pkgIdx = -1;
    int importIdx = -1;
    for (int i = 0; i < lines.length; i++) {
      if (lines[i].equals("package test;")) {
        pkgIdx = i;
      }
      if (lines[i].equals("import java.util.List;")) {
        importIdx = i;
      }
    }
    assertTrue(pkgIdx >= 0, "Package declaration should be on its own line");
    assertTrue(importIdx >= 0, "Import should be on its own line");
    assertEquals(
        pkgIdx + 1,
        importIdx,
        "Import should be on the line immediately after package declaration");
  }

  @Test
  void lightweightFormat_splitsTripleConcatenatedClosingBraces() {
    RoasterSourceFormatter formatter = createFormatter(FormattingMode.LIGHTWEIGHT);
    String input =
        """
        package test;
        public class Foo {
          public void bar() {
            if (true) {
              return;
            } } }""";
    String result = formatter.lightweightFormat(input);
    // After splitting, each brace ends up on its own line. The first brace
    // retains its original indentation; subsequent braces are stripped to bare "}".
    String[] lines = result.split("\n", -1);
    assertTrue(lines.length >= 3, "Should have at least 3 lines for 3 closing braces");
    assertTrue(
        lines[lines.length - 3].endsWith("}"), "Third-to-last line should end with closing brace");
    assertEquals(
        "}", lines[lines.length - 2], "Second-to-last line should be a bare closing brace");
    assertEquals("}", lines[lines.length - 1], "Last line should be a bare closing brace");
  }

  @Test
  void lightweightFormat_splitsConcatenatedImportAndJavadocWithPrecedingLine() {
    RoasterSourceFormatter formatter = createFormatter(FormattingMode.LIGHTWEIGHT);
    String input =
        """
        package test;
        import java.util.List;/**
         * Test javadoc.
         */
        public class Foo {
        }
        """;
    String result = formatter.lightweightFormat(input);
    String expected =
        """
        import java.util.List;
        /**""";
    assertTrue(
        result.contains(expected),
        "Concatenated import and javadoc should be split into separate lines with newline between");
  }

  @Test
  void format_lightweightMode_usesLightweightFormat() {
    RoasterSourceFormatter formatter = createFormatter(FormattingMode.LIGHTWEIGHT);
    String input =
        """
        package test;
        \tpublic class Foo {
        }
        """;
    String result = formatter.format(input);
    assertTrue(!result.contains("\t"), "LIGHTWEIGHT mode should convert tabs");
  }

  @Test
  void format_noneMode_doesNotConvertTabs() {
    RoasterSourceFormatter formatter = createFormatter(FormattingMode.NONE);
    String input =
        """
        package test;
        \tpublic class Foo {
        }
        """;
    String result = formatter.format(input);
    assertTrue(result.contains("\t"), "NONE mode should preserve tabs");
  }

  @Test
  void format_jdtMode_withProfile_producesFormattedOutput() {
    TestProcessingEnv env = createProcessingEnv();
    ProcessingLogger logger = new ProcessingLogger(env);
    RoasterSourceFormatter formatter = new RoasterSourceFormatter(logger, FormattingMode.JDT);
    String input =
        """
        package test;
        \tpublic class Foo {
        }
        """;
    String result = formatter.format(input);
    assertNotNull(result, "Format should always return a non-null string");
    assertTrue(
        env.messager.warnings.stream().noneMatch(w -> w.contains("JDT formatting requested")),
        "No fallback warning should be logged when formatter profile is available on classpath");
  }

  @Test
  void constructor_jdtMode_missingProfile_logsFallbackWarning() {
    TestProcessingEnv env = createProcessingEnv();
    ProcessingLogger logger = new ProcessingLogger(env);
    new RoasterSourceFormatter(logger, FormattingMode.JDT, "nonexistent-profile.xml");
    assertTrue(
        env.messager.warnings.stream()
            .anyMatch(w -> w.contains("JDT formatting requested") && w.contains("unavailable")),
        "JDT mode with missing profile should log fallback warning");
  }

  @Test
  void constructor_lightweightMode_missingProfile_noFallbackWarning() {
    TestProcessingEnv env = createProcessingEnv();
    ProcessingLogger logger = new ProcessingLogger(env);
    new RoasterSourceFormatter(logger, FormattingMode.LIGHTWEIGHT, "nonexistent-profile.xml");
    assertTrue(
        env.messager.warnings.stream().noneMatch(w -> w.contains("JDT formatting requested")),
        "LIGHTWEIGHT mode should not log JDT fallback warning even if profile is missing");
  }

  @Test
  void constructor_missingProfile_logsProfileNotFoundWarning() {
    TestProcessingEnv env = createProcessingEnv();
    ProcessingLogger logger = new ProcessingLogger(env);
    new RoasterSourceFormatter(logger, FormattingMode.JDT, "nonexistent-profile.xml");
    assertTrue(
        env.messager.warnings.stream()
            .anyMatch(w -> w.contains("not found") && w.contains("nonexistent-profile.xml")),
        "Missing formatter profile should log 'not found' warning with resource name");
  }

  @Test
  void format_jdtMode_missingProfile_fallsBackToLightweight() {
    TestProcessingEnv env = createProcessingEnv();
    ProcessingLogger logger = new ProcessingLogger(env);
    RoasterSourceFormatter formatter =
        new RoasterSourceFormatter(logger, FormattingMode.JDT, "nonexistent-profile.xml");
    String input =
        """
        package test;
        \tpublic class Foo {
        }
        """;
    String result = formatter.format(input);
    assertNotNull(result, "Format should always return a non-null string");
    assertTrue(
        !result.contains("\t"),
        "JDT mode with missing profile should fall back to lightweight (tabs converted)");
  }

  @Test
  void constructor_malformedProfile_logsLoadFailureWarning() {
    TestProcessingEnv env = createProcessingEnv();
    ProcessingLogger logger = new ProcessingLogger(env);
    new RoasterSourceFormatter(logger, FormattingMode.JDT, "eclipse-java-format-malformed.xml");
    assertTrue(
        env.messager.warnings.stream()
            .anyMatch(
                w ->
                    w.contains("Failed to load")
                        && w.contains("eclipse-java-format-malformed.xml")),
        "Malformed formatter profile should log 'Failed to load' warning");
  }
}
