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

package org.javahelpers.simple.builders.processor.processing.logging;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import org.javahelpers.simple.builders.processor.processing.CompilerArgumentsEnum;
import org.javahelpers.simple.builders.processor.processing.CompilerArgumentsReader;

/**
 * Logger for all messages during annotation processing. Providing util-functions for posting
 * messages easily. Using a {@code javax.annotation.processing.Messager} inside to publish the
 * messages.
 */
public class ProcessingLogger {
  /** Util class for exposing messages of type {@code javax.annotation.processing.Messager}. */
  private final Messager messager;

  /** Flag indicating if debug logging is enabled. */
  private final boolean debugEnabled;

  /** Thread-local indentation level for hierarchical logging. */
  private static final ThreadLocal<Integer> indentationLevel = ThreadLocal.withInitial(() -> 0);

  /**
   * Constructs a new ProcessingLogger with the specified ProcessingEnvironment. The Messager is
   * used to report errors, warnings, and other notices during annotation processing. Debug logging
   * is enabled by setting the compiler argument: -Averbose=true or -Asimplebuilder.verbose=true
   *
   * @param processingEnv the processing environment providing messager and options
   */
  public ProcessingLogger(ProcessingEnvironment processingEnv) {
    // Reset ThreadLocal state to ensure clean state between test runs
    resetThreadLocalState();

    this.messager = processingEnv.getMessager();
    CompilerArgumentsReader reader = new CompilerArgumentsReader(processingEnv);
    this.debugEnabled = reader.readBooleanValue(CompilerArgumentsEnum.VERBOSE);
  }

  /** Resets ThreadLocal state to ensure clean state between test runs. */
  private void resetThreadLocalState() {
    indentationLevel.set(0);
  }

  /**
   * Reports an error with a formatted message.
   *
   * @param format the format string
   * @param args arguments referenced by the format specifiers in the format string
   */
  public void error(String format, Object... args) {
    messager.printMessage(Diagnostic.Kind.ERROR, String.format(format, args));
  }

  /**
   * Reports an error at the location of the given element with a formatted message.
   *
   * @param e the element where the error occurred, used for location information
   * @param format the format string
   * @param args arguments referenced by the format specifiers in the format string
   */
  public void error(Element e, String format, Object... args) {
    messager.printMessage(Diagnostic.Kind.ERROR, String.format(format, args), e);
  }

  /**
   * Posts an info-level message (NOTE level in Maven). Used for important status information about
   * builder generation.
   *
   * @param message the info message to be posted
   */
  public void info(String message) {
    messager.printMessage(Diagnostic.Kind.NOTE, message);
  }

  /**
   * Formats a message with hierarchical indentation and proper spacing for alignment. Used by info
   * and warning methods when debug mode is enabled.
   *
   * @param message the message to format
   * @param spaces the number of spaces to add before the hierarchy for alignment
   * @return the formatted message with proper indentation and spacing
   */
  private String formatHierarchicalMessage(String message, int spaces) {
    String prefix = getCurrentIndentationLevel() > 0 ? "└─ " : "";
    String spacing = " ".repeat(spaces);
    return String.format("%s%s", spacing, formatWithIndentationNoDebug(message, prefix));
  }

  /**
   * Posts an info-level message with a formatted string.
   *
   * @param format the format string
   * @param args arguments referenced by the format specifiers in the format string
   */
  public void info(String format, Object... args) {
    String message = String.format(format, args);
    if (debugEnabled) {
      // When debug is enabled, add spaces to align with [DEBUG] prefix (which is 6 characters
      // longer)
      String indentedMessage = formatHierarchicalMessage(message, 8); // Add 8 spaces for alignment
      messager.printMessage(Diagnostic.Kind.NOTE, indentedMessage);
    } else {
      // When debug is disabled, use flat formatting (current behavior)
      messager.printMessage(Diagnostic.Kind.NOTE, message);
    }
  }

  /**
   * Posts a debug message with OTHER level. Used for detailed tracing of the builder generation
   * process. Only visible when enabled via -Averbose=true or -Asimplebuilder.verbose=true compiler
   * argument.
   *
   * @param message the debug message to be posted
   */
  public void debug(String message) {
    if (debugEnabled) {
      String indentedMessage = formatWithIndentation(message);
      messager.printMessage(Diagnostic.Kind.OTHER, indentedMessage);
    }
  }

  /**
   * Posts a debug message with a formatted string. Only visible when enabled via -Averbose=true or
   * -Asimplebuilder.verbose=true.
   *
   * @param format the format string
   * @param args arguments referenced by the format specifiers in the format string
   */
  public void debug(String format, Object... args) {
    if (debugEnabled) {
      String message = String.format(format, args);
      String indentedMessage = formatWithIndentation(message);
      messager.printMessage(Diagnostic.Kind.OTHER, indentedMessage);
    }
  }

  /**
   * Posts a warning message with a formatted string. Warnings are displayed by default and indicate
   * potential issues that don't prevent compilation.
   *
   * @param format the format string
   * @param args arguments referenced by the format specifiers in the format string
   */
  public void warning(String format, Object... args) {
    String message = String.format(format, args);
    if (debugEnabled) {
      // When debug is enabled, add spaces to align with [DEBUG] prefix (which is 5 characters
      // longer)
      String indentedMessage = formatHierarchicalMessage(message, 5); // Add 5 spaces for alignment
      messager.printMessage(Diagnostic.Kind.WARNING, indentedMessage);
    } else {
      // When debug is disabled, print flat
      messager.printMessage(Diagnostic.Kind.WARNING, message);
    }
  }

  /**
   * Reports a warning at the location of the given element with a formatted message.
   *
   * @param e the element where the warning occurred, used for location information
   * @param format the format string
   * @param args arguments referenced by the format specifiers in the format string
   */
  public void warning(Element e, String format, Object... args) {
    String message = String.format(format, args);
    if (debugEnabled) {
      // When debug is enabled, add spaces to align with [DEBUG] prefix (which is 5 characters
      // longer)
      String indentedMessage = formatHierarchicalMessage(message, 5); // Add 5 spaces for alignment
      messager.printMessage(Diagnostic.Kind.WARNING, indentedMessage, e);
    } else {
      // When debug is disabled, print flat
      messager.printMessage(Diagnostic.Kind.WARNING, message, e);
    }
  }

  /**
   * Formats a message with appropriate indentation based on current context.
   *
   * @param message the message to format
   * @param prefix the prefix to add (e.g., "├─ ", "└─ ") or empty string for no prefix
   * @return the formatted message with indentation
   */
  private String formatWithIndentation(String message, String prefix) {
    int level = indentationLevel.get();
    if (level == 0) {
      // At level 0, no indentation or prefix
      return "[DEBUG] " + message;
    }

    // Use │ characters with proper spacing for better visual connection between hierarchical levels
    StringBuilder indent = new StringBuilder();
    for (int i = 0; i < level - 1; i++) {
      indent.append("│  ");
    }
    // Add the specified prefix
    indent.append(prefix);
    return "[DEBUG] " + indent + message;
  }

  /**
   * Formats a message with indentation but without [DEBUG] prefix for INFO/WARNING messages.
   *
   * @param message the message to format
   * @param prefix the prefix to add (e.g., "├─ ", "└─ ") or empty string for no prefix
   * @return the formatted message with indentation but without [DEBUG] prefix
   */
  private String formatWithIndentationNoDebug(String message, String prefix) {
    int level = indentationLevel.get();
    if (level == 0) {
      // At level 0, no indentation or prefix
      return message;
    }

    // Use │ characters with proper spacing for better visual connection between hierarchical levels
    StringBuilder indent = new StringBuilder();
    for (int i = 0; i < level - 1; i++) {
      indent.append("│  ");
    }
    // Add the specified prefix
    indent.append(prefix);
    return indent + message;
  }

  /**
   * Formats a message with ├─ prefix for debug messages.
   *
   * @param message the message to format
   * @return the formatted message with indentation and ├─ prefix
   */
  private String formatWithIndentation(String message) {
    return formatWithIndentation(message, "├─ ");
  }

  /**
   * Starts a new hierarchical operation context, increasing indentation for subsequent debug
   * messages. This should be called before starting a major operation that has sub-operations.
   *
   * @param formatClosingMessage the format string for the operation message
   * @param args arguments referenced by the format specifiers
   */
  public void debugStartOperation(String formatClosingMessage, Object... args) {
    int currentLevel = indentationLevel.get();

    // Log the operation start message with proper prefix handling
    if (debugEnabled) {
      String operationMessage = formatWithIndentation(String.format(formatClosingMessage, args));
      messager.printMessage(Diagnostic.Kind.NOTE, operationMessage);
    }

    // Increase indentation for subsequent messages
    indentationLevel.set(currentLevel + 1);
  }

  /**
   * Ends the current hierarchical operation context, decreasing indentation. This should be called
   * after completing an operation started with startOperation.
   */
  public void debugEndOperation() {
    int currentLevel = indentationLevel.get();
    if (currentLevel > 0) {
      indentationLevel.set(currentLevel - 1);
      // No need to log "Operation completed" - it's redundant and adds noise
    }
  }

  /**
   * Ends the current hierarchical operation context with a closing message, decreasing indentation.
   * This should be called after completing an operation started with startOperation when you want
   * to log a closing message with the proper tree structure (using └─ for the last operation).
   *
   * @param formatClosingMessage the format string for the closing message
   * @param args arguments referenced by the format specifiers
   */
  public void debugEndOperation(String formatClosingMessage, Object... args) {
    int currentLevel = indentationLevel.get();

    // Log the closing message with └─ to indicate it's the last operation at this level
    if (debugEnabled) {
      String closingMessage = String.format(formatClosingMessage, args);
      String operationMessage = formatWithIndentation(closingMessage, "└─ ");
      messager.printMessage(Diagnostic.Kind.NOTE, operationMessage);
    }

    // Decrease indentation level
    if (currentLevel > 0) {
      indentationLevel.set(currentLevel - 1);
    }
  }

  /**
   * Resets the indentation level to zero to prevent cascading errors between processing runs. This
   * should be called at the end of each processing round.
   */
  public void resetIndentation() {
    indentationLevel.set(0);
    // Clean up ThreadLocal to prevent memory leaks
    indentationLevel.remove();
  }

  /** Gets the current indentation level for debugging purposes. */
  public int getCurrentIndentationLevel() {
    return indentationLevel.get();
  }
}
