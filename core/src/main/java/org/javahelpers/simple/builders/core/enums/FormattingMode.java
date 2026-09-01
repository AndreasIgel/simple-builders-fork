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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.javahelpers.simple.builders.core.enums;

/**
 * Enum representing the formatting mode for generated builder source files.
 *
 * <p>This enum controls how the raw source code produced by Roaster's {@code toUnformattedString()}
 * is post-processed before being written to disk.
 *
 * <p>Compiler option: {@code -Asimplebuilder.formattingMode=JDT|LIGHTWEIGHT|NONE}
 */
public enum FormattingMode {

  /**
   * Full Eclipse JDT formatter (default). Produces the highest quality output but is the slowest.
   */
  JDT("jdt"),

  /**
   * Lightweight post-processing. Applies minimal cosmetic fixes (tab-to-space conversion, blank
   * line collapsing, javadoc asterisk prefixes) at a fraction of the cost of full formatting.
   */
  LIGHTWEIGHT("lightweight"),

  /** No formatting at all. Returns raw Roaster output without any post-processing. */
  NONE("none");

  private final String optionValue;

  FormattingMode(String optionValue) {
    this.optionValue = optionValue;
  }

  /** Returns the option string used in compiler arguments and annotations. */
  public String getOptionValue() {
    return optionValue;
  }

  /**
   * Parses a string into a {@link FormattingMode}.
   *
   * <p>Accepts case-insensitive matching of either the enum name or the option value. Returns
   * {@link #JDT} as the default for unrecognized or null input.
   *
   * @param value the string to parse (e.g., "jdt", "LIGHTWEIGHT", "none")
   * @return the matching FormattingMode, or {@link #JDT} if not recognized
   */
  public static FormattingMode fromString(String value) {
    if (value == null || value.isBlank()) {
      return JDT;
    }
    for (FormattingMode mode : values()) {
      if (mode.optionValue.equalsIgnoreCase(value) || mode.name().equalsIgnoreCase(value)) {
        return mode;
      }
    }
    return JDT;
  }
}
