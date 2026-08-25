#!/usr/bin/env python3
"""
Generate Java class/record source files from library-class-catalog.json.

The JSON blueprint defines all classes for the performance-test module. This
script reads the JSON and produces one .java file per class entry under
performance-test/src/main/java/... following the package layout:

  org.javahelpers.simple.builders.performance_test.library.<package>.<ClassName>

The script generates three categories of Java source files:
  1. Enums — from the top-level ``enums.types`` array (into the .enums sub-package).
  2. Base classes — from the top-level ``baseClasses`` section: one root entity
     class (LibraryEntity) and one intermediate base class per package.
  3. DTO classes/records — from the ``packages.<name>.classes`` arrays.

Usage:
  python3 generate_classes.py                                      # generate all classes (default: SimpleBuilder)
  python3 generate_classes.py --builder-type simple-minimal-builder # use @SimpleMinimalBuilder
  python3 generate_classes.py --builder-type record-builder         # use RecordBuilder (records only)
  python3 generate_classes.py --builder-type lombok                 # use Lombok @Builder
  python3 generate_classes.py --limit 5                            # generate only the first 5 classes (test mode)
  python3 generate_classes.py --dry-run                            # print what would be written, write nothing
  python3 generate_classes.py --json other.json                    # use an alternate JSON file
  python3 generate_classes.py --out other/dir                      # use an alternate output root
  python3 generate_classes.py --force                              # overwrite existing .java files

Builder type presets (--builder-type):
  Each preset configures the annotation, its import, the wither interface
  suffix, and which class kinds are supported:

  - simple-builder:        @SimpleBuilder, records implement <Name>Builder.With (wither interface),
                           supports class + record. Uses @Default and @IgnoreInBuilder.
  - simple-minimal-builder: @SimpleMinimalBuilder, no With interface (disabled by
                           template), supports class + record. Uses @Default and @IgnoreInBuilder.
  - record-builder:        @RecordBuilder (io.soabase.recordbuilder.core), records
                           implement <Name>Builder.With, supports record only.
                           No @Default or @IgnoreInBuilder (different mechanisms).
  - lombok:                @Builder (lombok.Builder), no With interface, supports
                           class + record. No @Default or @IgnoreInBuilder.

  Classes whose ``kind`` is not in the preset's ``supported_kinds`` are skipped
  (e.g. plain classes are skipped for record-builder).

JSON schema (expected by this script):
  {
    "topic": "...",                       # ignored
    "designNotes": "...",                 # ignored (human-readable documentation)
    "enums": {                            # optional: enum types to generate
      "types": [
        {
          "name": "<EnumName>",           # required, valid Java identifier
          "values": ["VAL1", "VAL2", ...] # required, list of enum constant names
        }
      ]
    },
    "baseClasses": {                      # optional: infrastructure base classes
      "rootEntity": {                     # the root entity class (e.g. LibraryEntity)
        "name": "<ClassName>",            # required
        "package": "<subPackage>" | "",   # sub-package ("" = base package, no sub-package)
        "properties": [ ... ]             # same property format as DTO classes
      },
      "intermediateClasses": [            # one intermediate base class per package
        {
          "name": "<ClassName>",          # required
          "package": "<subPackage>",      # which sub-package this class lives in
          "extends": "<SuperclassName>"  # typically the root entity name
        }
      ]
    },
    "packages": {
      "<subPackage>": {                   # subPackage -> Java sub-package name
        "classes": [
          {
            "name": "<ClassName>",         # required, must be a valid Java identifier
            "kind": "class" | "record",   # required
            "extends": "<Name>" | null,   # superclass for classes; ignored for records
            "properties": [
              {
                "name": "<propName>",      # required, valid Java identifier
                "type": "<JavaType>",      # required, e.g. "String", "List<Foo>", "int[]"
                "default": "<value>",      # optional -> @Default("value") (SimpleBuilders only)
                "ignore": true             # optional -> @IgnoreInBuilder (SimpleBuilders only)
              }
            ]
          }
        ]
      }
    }
  }

Project-specific assumptions (hardcoded — edit the constants below to reuse
this script for a different project):
  - BASE_PACKAGE: the generated classes live under
    org.javahelpers.simple.builders.performance_test.library.<subPackage>
  - COPYRIGHT_YEAR / COPYRIGHT_HOLDER: the MIT license header in every file
  - SimpleBuilders annotations use org.javahelpers.simple.builders.core.annotations
    (@SimpleBuilder, @SimpleMinimalBuilder, @Default, @IgnoreInBuilder)
  - Records get `implements <Name><wither_interface_suffix>` when the builder
    framework supports a wither interface (SimpleBuilder and RecordBuilder do;
    SimpleMinimalBuilder and Lombok do not). Plain classes do not get an implements clause.
  - ENUM_PACKAGE: enum types are generated in <BASE_PACKAGE>.enums from the
    JSON's ``enums.types`` array. Any property type that is NOT a primitive,
    NOT in STANDARD_IMPORTS, and NOT a class declared in the JSON is assumed
    to be an enum and imported from ENUM_PACKAGE.
  - BASE_CLASSES: the root entity class and intermediate base classes are
    generated from the JSON's ``baseClasses`` section before the DTO classes.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import tempfile
from pathlib import Path
from typing import Iterable

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------

SCRIPT_DIR = Path(__file__).resolve().parent
DEFAULT_JSON = SCRIPT_DIR.parent / "docs" / "library-class-catalog.json"
DEFAULT_OUT_ROOT = SCRIPT_DIR.parent / "src" / "main" / "java"

BASE_PACKAGE = "org.javahelpers.simple.builders.performance_test.library"
COPYRIGHT_YEAR = 2026
COPYRIGHT_HOLDER = "Andreas Igel"

# Java standard/3rd-party types that need imports, mapped to their fully
# qualified names. Only the simple name appears in the JSON.
STANDARD_IMPORTS: dict[str, str] = {
    "LocalDate": "java.time.LocalDate",
    "LocalTime": "java.time.LocalTime",
    "LocalDateTime": "java.time.LocalDateTime",
    "Duration": "java.time.Duration",
    "BigDecimal": "java.math.BigDecimal",
    "BigInteger": "java.math.BigInteger",
    "UUID": "java.util.UUID",
    "List": "java.util.List",
    "Set": "java.util.Set",
    "Map": "java.util.Map",
    "Optional": "java.util.Optional",
    "Date": "java.util.Date",
}

# Package where hand-written enum types live (fixed by the plan). Enum names
# themselves are NOT hardcoded — any property type that is neither a primitive,
# a standard Java type, nor a class declared in the JSON is assumed to be an
# enum imported from this package. This keeps the script JSON-driven: adding a
# new enum to the catalog only requires using it as a property type.
ENUM_PACKAGE = f"{BASE_PACKAGE}.enums"

# Predefined builder type presets. Each preset configures all annotation-related
# parameters for a known framework. Use via ``--builder-type``.
BUILDER_TYPE_PRESETS: dict[str, dict] = {
    "simple-builder": {
        "annotation": "SimpleBuilder",
        "annotation_import": "org.javahelpers.simple.builders.core.annotations.SimpleBuilder",
        "wither_interface_suffix": "Builder.With",
        "default_import": "org.javahelpers.simple.builders.core.annotations.Default",
        "ignore_import": "org.javahelpers.simple.builders.core.annotations.IgnoreInBuilder",
        "supported_kinds": {"class", "record"},
    },
    "simple-minimal-builder": {
        "annotation": "SimpleMinimalBuilder",
        "annotation_import": "org.javahelpers.simple.builders.core.annotations.SimpleMinimalBuilder",
        "wither_interface_suffix": "",
        "default_import": "org.javahelpers.simple.builders.core.annotations.Default",
        "ignore_import": "org.javahelpers.simple.builders.core.annotations.IgnoreInBuilder",
        "supported_kinds": {"class", "record"},
    },
    # RecordBuilder: only supports records (not plain classes). Has its own With interface
    # (enableWither=true by default). No @Default/@IgnoreInBuilder — different mechanisms.
    "record-builder": {
        "annotation": "RecordBuilder",
        "annotation_import": "io.soabase.recordbuilder.core.RecordBuilder",
        "wither_interface_suffix": "Builder.With",
        "default_import": "",
        "ignore_import": "",
        "supported_kinds": {"record"},
    },
    # Lombok: @Builder supports both classes and records. No With interface.
    # No @Default/@IgnoreInBuilder — Lombok uses @Builder.Default on field initializers instead.
    "lombok": {
        "annotation": "Builder",
        "annotation_import": "lombok.Builder",
        "wither_interface_suffix": "",
        "default_import": "",
        "ignore_import": "",
        "supported_kinds": {"class", "record"},
    },
}

# Primitive types (no import needed, no wrapper logic for getters).
PRIMITIVES = {"int", "long", "double", "float", "boolean", "char", "byte", "short"}

# Valid ``kind`` values in the JSON.
VALID_KINDS = {"class", "record"}

# Regex for valid Java identifiers (JLS §3.8). Covers the ASCII subset, which
# is all we expect in this catalog. Keywords are technically valid identifiers
# but would produce non-compiling Java — we reject them separately below.
_JAVA_IDENTIFIER_RE = re.compile(r"^[A-Za-z_$][A-Za-z0-9_$]*$")

# Java keywords that must not be used as identifiers (JLS §3.9).
JAVA_KEYWORDS = {
    "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
    "class", "const", "continue", "default", "do", "double", "else", "enum",
    "extends", "final", "finally", "float", "for", "goto", "if", "implements",
    "import", "instanceof", "int", "interface", "long", "native", "new",
    "package", "private", "protected", "public", "return", "short", "static",
    "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
    "transient", "try", "void", "volatile", "while", "true", "false", "null",
    "record", "sealed", "permits", "var", "yield",
}


# ---------------------------------------------------------------------------
# Validation helpers
# ---------------------------------------------------------------------------


class ValidationError(ValueError):
    """Raised when a JSON entry fails validation."""


def validate_identifier(name: str, label: str) -> None:
    """
    Validate that ``name`` is a safe Java identifier.

    This serves two purposes:
      1. Prevents path traversal — a name like ``../../etc/passwd`` would be
         used directly in a file path without this check.
      2. Ensures the generated Java compiles — ``int`` or ``123abc`` would not.

    Raises ``ValidationError`` if the name is not a valid Java identifier or
    is a Java keyword.
    """
    if not name:
        raise ValidationError(f"{label} is empty")
    if not _JAVA_IDENTIFIER_RE.match(name):
        raise ValidationError(
            f"{label} {name!r} is not a valid Java identifier "
            f"(contains illegal characters or starts with a digit)"
        )
    if name in JAVA_KEYWORDS:
        raise ValidationError(f"{label} {name!r} is a Java keyword and cannot be used as an identifier")


def validate_type_string(type_str: str, label: str) -> None:
    """
    Validate that ``type_str`` is a plausible Java type expression.

    This is a lightweight check — it rejects characters that have no business
    in a type expression (path separators, semicolons, etc.) and ensures every
    token within it is a valid Java identifier. It does NOT fully parse Java
    generics syntax.
    """
    if not type_str:
        raise ValidationError(f"{label} type is empty")
    # Reject characters that could be used for path traversal or injection.
    forbidden = {";", "/", "\\", ".."}
    for ch in forbidden:
        if ch in type_str:
            raise ValidationError(f"{label} type {type_str!r} contains forbidden character {ch!r}")
    # Every token must be a valid identifier (after stripping < > , [ ] spaces).
    for token in extract_simple_types(type_str):
        if token not in PRIMITIVES and token not in JAVA_KEYWORDS:
            if not _JAVA_IDENTIFIER_RE.match(token):
                raise ValidationError(
                    f"{label} type {type_str!r} contains invalid token {token!r}"
                )


def escape_java_string_literal(value: str) -> str:
    """
    Escape a string so it can be safely embedded inside a Java double-quoted
    string literal (e.g. for ``@Default("...")``).

    Escapes backslash, double quote, and common control characters.
    """
    result = []
    for ch in value:
        if ch == "\\":
            result.append("\\\\")
        elif ch == '"':
            result.append('\\"')
        elif ch == "\n":
            result.append("\\n")
        elif ch == "\r":
            result.append("\\r")
        elif ch == "\t":
            result.append("\\t")
        else:
            result.append(ch)
    return "".join(result)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def license_header() -> str:
    """Return the MIT license header used across the project."""
    return f"""/*
 * MIT License
 *
 * Copyright (c) {COPYRIGHT_YEAR} {COPYRIGHT_HOLDER}
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
 */"""


def capitalize(name: str) -> str:
    """Capitalize the first letter of ``name``."""
    return name[0].upper() + name[1:] if name else name


def extract_simple_types(type_str: str) -> set[str]:
    """
    Extract all simple type names referenced in a Java type string.

    Examples:
      "String"                  -> {"String"}
      "List<String>"            -> {"List", "String"}
      "Map<String,OtherDto>"    -> {"Map", "String", "OtherDto"}
      "Optional<List<Author>>"  -> {"Optional", "List", "Author"}
      "int[]"                   -> {"int"}
    """
    # Strip array brackets
    cleaned = type_str.replace("[]", "")
    # Split on anything that is not a Java identifier character
    tokens = re.split(r"[<>,\s]+", cleaned)
    return {t for t in tokens if t and t not in {"?", "extends", "super", "var"}}


def getter_name(field_name: str, field_type: str) -> str:
    """Return the getter method name following the JavaBeans convention."""
    cap = capitalize(field_name)
    return f"is{cap}" if field_type == "boolean" else f"get{cap}"


def setter_name(field_name: str) -> str:
    """Return the setter method name following the JavaBeans convention."""
    return f"set{capitalize(field_name)}"


def collect_class_index(data: dict) -> dict[str, str]:
    """
    Build a mapping ``class_name -> sub_package`` for every class defined in
    the JSON, including base classes from the ``baseClasses`` section.
    Used to resolve cross-package imports for reference types.

    Raises ``ValidationError`` if duplicate class names are found.
    """
    index: dict[str, str] = {}

    # Add base classes to the index so they are not misclassified as enums.
    base_classes_data = data.get("baseClasses", {})
    root_entity = base_classes_data.get("rootEntity")
    if root_entity:
        root_name = root_entity.get("name")
        root_pkg = root_entity.get("package", "")
        if root_name:
            index[root_name] = root_pkg
    for inter in base_classes_data.get("intermediateClasses", []):
        inter_name = inter.get("name")
        inter_pkg = inter.get("package", "")
        if inter_name:
            index[inter_name] = inter_pkg

    for pkg_name, pkg in data.get("packages", {}).items():
        for cls in pkg.get("classes", []):
            name = cls.get("name")
            if not name:
                continue
            if name in index:
                raise ValidationError(
                    f"duplicate class name {name!r} found in package {pkg_name!r} "
                    f"(already defined in package {index[name]!r})"
                )
            index[name] = pkg_name
    return index


def resolve_imports(
    class_entry: dict,
    sub_package: str,
    class_index: dict[str, str],
    preset: dict = BUILDER_TYPE_PRESETS["simple-builder"],
) -> list[str]:
    """
    Determine the sorted list of import statements needed for a class/record.

    Rules:
      - Standard Java types (List, LocalDate, ...) -> java.* imports
      - Classes found in the JSON -> cross-package import, but only when the
        referenced class lives in a different sub-package (same-package types
        need no import).
      - Any other non-primitive, non-String type -> assumed to be an enum in
        .library.enums and imported from there. This avoids hardcoding enum
        names; the trade-off is that a genuinely unknown reference would also
        produce an (incorrect) enum import.
      - Primitives and String -> no import
    """
    needed: set[str] = set()
    uses_default = False
    uses_ignore = False

    for prop in class_entry.get("properties", []):
        prop_type = prop.get("type", "")
        for simple in extract_simple_types(prop_type):
            if simple in PRIMITIVES or simple == "String":
                continue
            if simple in STANDARD_IMPORTS:
                needed.add(STANDARD_IMPORTS[simple])
            elif simple in class_index:
                if class_index[simple] != sub_package:
                    ref_pkg = class_index[simple]
                    if ref_pkg:
                        needed.add(f"{BASE_PACKAGE}.{ref_pkg}.{simple}")
                    else:
                        needed.add(f"{BASE_PACKAGE}.{simple}")
                # same sub-package: no import needed
            else:
                # Not a primitive, not a standard Java type, not a JSON class:
                # assume it is an enum in the .library.enums package.
                needed.add(f"{ENUM_PACKAGE}.{simple}")
        if "default" in prop:
            uses_default = True
        if prop.get("ignore"):
            uses_ignore = True

    # Builder annotation is always used (parameterized via CLI)
    needed.add(preset["annotation_import"])
    if uses_default and preset["default_import"]:
        needed.add(preset["default_import"])
    if uses_ignore and preset["ignore_import"]:
        needed.add(preset["ignore_import"])

    return sorted(needed)


# ---------------------------------------------------------------------------
# Source generation
# ---------------------------------------------------------------------------


def generate_class_source(
    class_entry: dict,
    sub_package: str,
    class_index: dict[str, str],
    preset: dict = BUILDER_TYPE_PRESETS["simple-builder"],
) -> str:
    """
    Generate the full Java source for a ``kind: "class"`` entry.

    The ``extends`` clause uses the per-class ``extends`` field from the JSON.
    If it is null or absent, the class has no superclass.
    """
    name = class_entry["name"]
    props = class_entry.get("properties", [])
    imports = resolve_imports(class_entry, sub_package, class_index, preset)
    package_decl = f"{BASE_PACKAGE}.{sub_package}"

    # Determine extends clause from the per-class field.
    extends_value = class_entry.get("extends")
    extends_clause = f" extends {extends_value}" if extends_value else ""

    lines: list[str] = []
    lines.append(license_header())
    lines.append("")
    lines.append(f"package {package_decl};")
    lines.append("")
    if imports:
        for imp in imports:
            lines.append(f"import {imp};")
        lines.append("")
    lines.append(f"@{preset['annotation']}")
    lines.append(f"public class {name}{extends_clause} {{")
    lines.append("")

    # Fields
    for prop in props:
        field_name = prop["name"]
        field_type = prop["type"]
        annotations: list[str] = []
        if "default" in prop and preset["default_import"]:
            escaped = escape_java_string_literal(str(prop["default"]))
            annotations.append(f'@Default("{escaped}")')
        # NOTE: @IgnoreInBuilder targets METHOD/PARAMETER, not FIELD, so it is
        # placed on the setter below, not here.
        indent = "  "
        for ann in annotations:
            lines.append(f"{indent}{ann}")
        lines.append(f"{indent}private {field_type} {field_name};")
    lines.append("")

    # Getters and setters
    for prop in props:
        field_name = prop["name"]
        field_type = prop["type"]
        getter = getter_name(field_name, field_type)
        setter = setter_name(field_name)
        indent = "  "

        # Getter
        lines.append(f"{indent}public {field_type} {getter}() {{")
        lines.append(f"{indent}  return {field_name};")
        lines.append(f"{indent}}}")
        lines.append("")

        # Setter (with @IgnoreInBuilder if flagged)
        if prop.get("ignore") and preset["ignore_import"]:
            lines.append(f"{indent}@IgnoreInBuilder")
        lines.append(f"{indent}public void {setter}({field_type} {field_name}) {{")
        lines.append(f"{indent}  this.{field_name} = {field_name};")
        lines.append(f"{indent}}}")
        lines.append("")

    lines.append("}")
    lines.append("")
    return "\n".join(lines)


def generate_record_source(
    class_entry: dict,
    sub_package: str,
    class_index: dict[str, str],
    preset: dict = BUILDER_TYPE_PRESETS["simple-builder"],
) -> str:
    """Generate the full Java source for a ``kind: "record"`` entry."""
    name = class_entry["name"]
    props = class_entry.get("properties", [])
    imports = resolve_imports(class_entry, sub_package, class_index, preset)
    package_decl = f"{BASE_PACKAGE}.{sub_package}"

    lines: list[str] = []
    lines.append(license_header())
    lines.append("")
    lines.append(f"package {package_decl};")
    lines.append("")
    if imports:
        for imp in imports:
            lines.append(f"import {imp};")
        lines.append("")
    lines.append(f"@{preset['annotation']}")
    lines.append(f"public record {name}(")

    # Record components
    for i, prop in enumerate(props):
        field_name = prop["name"]
        field_type = prop["type"]
        annotations: list[str] = []
        if "default" in prop and preset["default_import"]:
            escaped = escape_java_string_literal(str(prop["default"]))
            annotations.append(f'@Default("{escaped}")')
        if prop.get("ignore") and preset["ignore_import"]:
            annotations.append("@IgnoreInBuilder")
        prefix = "    "
        if annotations:
            ann_str = "\n".join(f"{prefix}{a}" for a in annotations) + "\n" + prefix
        else:
            ann_str = prefix
        comma = "," if i < len(props) - 1 else ""
        lines.append(f"{ann_str}{field_type} {field_name}{comma}")

    implements_clause = f" implements {name}{preset['wither_interface_suffix']}" if preset["wither_interface_suffix"] else ""
    lines.append(f"){implements_clause} {{")
    lines.append("}")
    lines.append("")
    return "\n".join(lines)


def generate_enum_source(enum_entry: dict) -> str:
    """
    Generate the full Java source for an enum type.

    The enum is placed in the ``.enums`` sub-package and contains only the
    constants listed in the ``values`` array — no fields, constructors, or
    methods.
    """
    name = enum_entry["name"]
    values = enum_entry["values"]

    lines: list[str] = []
    lines.append(license_header())
    lines.append("")
    lines.append(f"package {ENUM_PACKAGE};")
    lines.append("")
    lines.append(f"public enum {name} {{")
    for i, val in enumerate(values):
        comma = "," if i < len(values) - 1 else ""
        lines.append(f"  {val}{comma}")
    lines.append("}")
    lines.append("")
    return "\n".join(lines)


def generate_base_class_source(
    name: str,
    sub_package: str,
    extends_value: str | None,
    props: list[dict],
    class_index: dict[str, str],
    root_entity_name: str = "",
    preset: dict = BUILDER_TYPE_PRESETS["simple-builder"],
) -> str:
    """
    Generate a base infrastructure class (root entity or intermediate base).

    These classes are plain POJOs with fields, getters, and setters — no
    @SimpleBuilder annotation. They use the same import resolution as DTO
    classes so that enum and cross-package references are handled correctly.
    """
    package_decl = f"{BASE_PACKAGE}.{sub_package}" if sub_package else BASE_PACKAGE

    # Build a minimal class_entry for resolve_imports
    class_entry: dict = {"properties": props}
    imports = resolve_imports(class_entry, sub_package, class_index, preset)
    # Remove builder annotation from imports — base classes are not annotated
    imports = [i for i in imports if not i.endswith(f".{preset['annotation']}") and i != preset["annotation_import"]]

    # Add import for superclass if it's in a different package
    if extends_value:
        if extends_value in class_index:
            ext_pkg = class_index[extends_value]
            if ext_pkg != sub_package:
                if ext_pkg:
                    imports.append(f"{BASE_PACKAGE}.{ext_pkg}.{extends_value}")
                else:
                    imports.append(f"{BASE_PACKAGE}.{extends_value}")
        elif extends_value not in PRIMITIVES and extends_value != "String":
            # Root entity in base package — check if it's the root entity name
            if extends_value == root_entity_name:
                imports.append(f"{BASE_PACKAGE}.{extends_value}")

    imports = sorted(set(imports))

    extends_clause = f" extends {extends_value}" if extends_value else ""

    lines: list[str] = []
    lines.append(license_header())
    lines.append("")
    lines.append(f"package {package_decl};")
    lines.append("")
    if imports:
        for imp in imports:
            lines.append(f"import {imp};")
        lines.append("")
    lines.append(f"public class {name}{extends_clause} {{")
    lines.append("")

    # Fields
    for prop in props:
        field_name = prop["name"]
        field_type = prop["type"]
        lines.append(f"  private {field_type} {field_name};")
    lines.append("")

    # Getters and setters
    for prop in props:
        field_name = prop["name"]
        field_type = prop["type"]
        getter = getter_name(field_name, field_type)
        setter = setter_name(field_name)
        lines.append(f"  public {field_type} {getter}() {{")
        lines.append(f"    return {field_name};")
        lines.append("  }")
        lines.append("")
        lines.append(f"  public void {setter}({field_type} {field_name}) {{")
        lines.append(f"    this.{field_name} = {field_name};")
        lines.append("  }")
        lines.append("")

    lines.append("}")
    lines.append("")
    return "\n".join(lines)


def generate_source(
    class_entry: dict,
    sub_package: str,
    class_index: dict[str, str],
    preset: dict = BUILDER_TYPE_PRESETS["simple-builder"],
) -> str:
    """Dispatch to the correct generator based on ``kind``."""
    kind = class_entry.get("kind", "class")
    if kind == "record":
        return generate_record_source(class_entry, sub_package, class_index, preset)
    return generate_class_source(class_entry, sub_package, class_index, preset)


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------


def iter_classes(data: dict) -> Iterable[tuple[str, dict]]:
    """
    Yield ``(sub_package, class_entry)`` tuples in JSON order:
    items, persons, organizations, locations, events, and within each
    package the classes in array order.
    """
    for pkg_name, pkg in data.get("packages", {}).items():
        for cls in pkg.get("classes", []):
            yield pkg_name, cls


def atomic_write(path: Path, content: str) -> None:
    """
    Write ``content`` to ``path`` atomically.

    Writes to a temporary file in the same directory, then renames it to the
    target. This ensures that if the process is interrupted, the target file
    is either the old version or the complete new version — never a partial
    write.
    """
    path.parent.mkdir(parents=True, exist_ok=True)
    # Use the same directory as the target so the rename is atomic on the same
    # filesystem. NamedTemporaryFile handles the unique-name and cleanup-on-
    # error logic.
    with tempfile.NamedTemporaryFile(
        mode="w",
        encoding="utf-8",
        dir=path.parent,
        prefix=f".{path.stem}.",
        suffix=".tmp",
        delete=False,
    ) as tmp:
        tmp.write(content)
        tmp.flush()
        os.fsync(tmp.fileno())
        tmp_path = Path(tmp.name)
    # os.replace is atomic on POSIX and Windows (Python 3.3+).
    os.replace(tmp_path, path)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Generate Java class files from library-class-catalog.json.",
    )
    parser.add_argument(
        "--json",
        type=Path,
        default=DEFAULT_JSON,
        help=f"Path to the catalog JSON (default: {DEFAULT_JSON})",
    )
    parser.add_argument(
        "--out",
        type=Path,
        default=DEFAULT_OUT_ROOT,
        help=f"Output root directory (default: {DEFAULT_OUT_ROOT})",
    )
    parser.add_argument(
        "--limit",
        type=int,
        default=None,
        help="Only generate the first N classes (test mode, e.g. --limit 5).",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print the paths that would be written without writing any files.",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="Overwrite existing .java files (default: skip them).",
    )
    parser.add_argument(
        "--builder-type",
        type=str,
        default=None,
        choices=list(BUILDER_TYPE_PRESETS.keys()),
        help="Shortcut preset for known builder frameworks. Sets all annotation-related "
        "parameters at once. Options: "
        + ", ".join(BUILDER_TYPE_PRESETS.keys())
        + ". Individual --annotation, --annotation-import, etc. override the preset.",
    )
    parser.add_argument(
        "--annotation",
        type=str,
        default=None,
        help=f"Builder annotation name to place on classes (default: {BUILDER_TYPE_PRESETS['simple-builder']['annotation']}). "
        "Use e.g. MinimalBuilder for a custom template annotation.",
    )
    parser.add_argument(
        "--annotation-import",
        type=str,
        default=None,
        help="Fully qualified import for the builder annotation. "
        "If omitted, derived from --annotation (simple-builders annotations package).",
    )
    parser.add_argument(
        "--wither-interface-suffix",
        type=str,
        default=None,
        help="Suffix for the wither interface implements clause on records (default: Builder.With). "
        "The full clause is <ClassName><suffix>. Use empty string to omit the implements clause.",
    )
    parser.add_argument(
        "--default-import",
        type=str,
        default=None,
        help="Fully qualified import for the @Default annotation. "
        "Use empty string to skip @Default annotations entirely.",
    )
    parser.add_argument(
        "--ignore-import",
        type=str,
        default=None,
        help="Fully qualified import for the @IgnoreInBuilder annotation. "
        "Use empty string to skip @IgnoreInBuilder annotations entirely.",
    )
    args = parser.parse_args(argv)

    json_path: Path = args.json
    out_root: Path = args.out
    limit: int | None = args.limit

    # Resolve preset: start with builder-type preset, then apply individual overrides
    base_preset = BUILDER_TYPE_PRESETS.get(args.builder_type, BUILDER_TYPE_PRESETS["simple-builder"])
    preset: dict = {
        "annotation": args.annotation or base_preset["annotation"],
        "annotation_import": args.annotation_import or base_preset["annotation_import"],
        "wither_interface_suffix": (
            args.wither_interface_suffix if args.wither_interface_suffix is not None
            else base_preset["wither_interface_suffix"]
        ),
        "default_import": args.default_import if args.default_import is not None else base_preset["default_import"],
        "ignore_import": args.ignore_import if args.ignore_import is not None else base_preset["ignore_import"],
        "supported_kinds": base_preset.get("supported_kinds", {"class", "record"}),
    }
    supported_kinds: set[str] = preset["supported_kinds"]

    if limit is not None and limit < 0:
        print("error: --limit must be >= 0", file=sys.stderr)
        return 2

    if not json_path.is_file():
        print(f"error: JSON file not found: {json_path}", file=sys.stderr)
        return 2

    try:
        with json_path.open("r", encoding="utf-8") as f:
            data = json.load(f)
    except json.JSONDecodeError as e:
        print(f"error: invalid JSON in {json_path}: {e}", file=sys.stderr)
        return 2

    # Build class index and validate uniqueness.
    try:
        class_index = collect_class_index(data)
    except ValidationError as e:
        print(f"error: {e}", file=sys.stderr)
        return 2

    total_in_json = sum(len(p.get("classes", [])) for p in data.get("packages", {}).values())
    n_to_generate = min(limit, total_in_json) if limit is not None else total_in_json

    # Count enums and base classes for display
    enums_data = data.get("enums", {})
    enum_types = enums_data.get("types", [])
    base_classes_data = data.get("baseClasses", {})
    root_entity = base_classes_data.get("rootEntity")
    intermediate_classes = base_classes_data.get("intermediateClasses", [])

    print(f"JSON:       {json_path}")
    print(f"Output:     {out_root}")
    print(f"Annotation: @{preset['annotation']} (from {preset['annotation_import']})")
    if preset["wither_interface_suffix"]:
        print(f"Wither iface: <Name>{preset['wither_interface_suffix']}")
    else:
        print(f"Wither iface: (none)")
    print(f"Enums:      {len(enum_types)}")
    print(f"Base classes: {1 + len(intermediate_classes) if root_entity else len(intermediate_classes)}")
    print(f"Classes in JSON: {total_in_json}")
    if limit is not None:
        print(f"Limit:      first {n_to_generate} class(es) (test mode)")
    print()

    written = 0
    skipped = 0
    errors: list[str] = []

    # --- Generate enums ---
    for enum_entry in enum_types:
        try:
            enum_name = enum_entry.get("name")
            if not enum_name:
                raise ValidationError("enum entry has no name")
            validate_identifier(enum_name, f"enum name {enum_name!r}")
            for val in enum_entry.get("values", []):
                validate_identifier(val, f"enum constant {val!r} in enum {enum_name!r}")
        except ValidationError as e:
            errors.append(str(e))
            print(f"  ERROR: {e}", file=sys.stderr)
            continue

        try:
            source = generate_enum_source(enum_entry)
        except (KeyError, TypeError) as e:
            msg = f"failed to generate source for enum {enum_name!r}: {e}"
            errors.append(msg)
            print(f"  ERROR: {msg}", file=sys.stderr)
            continue

        enum_rel = Path(*ENUM_PACKAGE.split(".")) / f"{enum_name}.java"
        out_file = out_root / enum_rel

        if args.dry_run:
            print(f"  [dry-run] {out_file}  ({len(source)} bytes)")
            continue
        if out_file.exists() and not args.force:
            print(f"  skip (exists): {out_file}")
            skipped += 1
            continue
        try:
            atomic_write(out_file, source)
        except OSError as e:
            msg = f"failed to write {out_file}: {e}"
            errors.append(msg)
            print(f"  ERROR: {msg}", file=sys.stderr)
            continue
        print(f"  wrote: {out_file}")
        written += 1

    # --- Generate base classes ---
    # Root entity class
    if root_entity:
        root_name = root_entity.get("name", "LibraryEntity")
        root_pkg = root_entity.get("package", "")
        root_props = root_entity.get("properties", [])
        try:
            validate_identifier(root_name, f"root entity name {root_name!r}")
            source = generate_base_class_source(
                root_name, root_pkg, None, root_props, class_index, root_name,
                preset,
            )
        except (ValidationError, KeyError, TypeError) as e:
            msg = f"failed to generate root entity {root_name!r}: {e}"
            errors.append(msg)
            print(f"  ERROR: {msg}", file=sys.stderr)
        else:
            root_rel_parts = BASE_PACKAGE.split(".") if not root_pkg else [*BASE_PACKAGE.split("."), root_pkg]
            root_rel = Path(*root_rel_parts) / f"{root_name}.java"
            out_file = out_root / root_rel
            if args.dry_run:
                print(f"  [dry-run] {out_file}  ({len(source)} bytes)")
            elif out_file.exists() and not args.force:
                print(f"  skip (exists): {out_file}")
                skipped += 1
            else:
                try:
                    atomic_write(out_file, source)
                    print(f"  wrote: {out_file}")
                    written += 1
                except OSError as e:
                    msg = f"failed to write {out_file}: {e}"
                    errors.append(msg)
                    print(f"  ERROR: {msg}", file=sys.stderr)

    # Intermediate base classes
    for inter in intermediate_classes:
        inter_name = inter.get("name")
        inter_pkg = inter.get("package", "")
        inter_extends = inter.get("extends")
        try:
            validate_identifier(inter_name, f"intermediate base class name {inter_name!r}")
            source = generate_base_class_source(
                inter_name, inter_pkg, inter_extends, [], class_index, root_name,
                preset,
            )
        except (ValidationError, KeyError, TypeError) as e:
            msg = f"failed to generate base class {inter_name!r}: {e}"
            errors.append(msg)
            print(f"  ERROR: {msg}", file=sys.stderr)
            continue

        inter_rel = Path(*BASE_PACKAGE.split(".")) / inter_pkg / f"{inter_name}.java"
        out_file = out_root / inter_rel
        if args.dry_run:
            print(f"  [dry-run] {out_file}  ({len(source)} bytes)")
            continue
        if out_file.exists() and not args.force:
            print(f"  skip (exists): {out_file}")
            skipped += 1
            continue
        try:
            atomic_write(out_file, source)
        except OSError as e:
            msg = f"failed to write {out_file}: {e}"
            errors.append(msg)
            print(f"  ERROR: {msg}", file=sys.stderr)
            continue
        print(f"  wrote: {out_file}")
        written += 1

    # --- Generate DTO classes/records ---
    for idx, (sub_package, cls) in enumerate(iter_classes(data)):
        if limit is not None and idx >= limit:
            break

        # --- Validate the entry before generating anything ---
        try:
            name = cls.get("name")
            if not name:
                raise ValidationError(f"class entry at index {idx} has no name")
            validate_identifier(name, f"class name (index {idx})")
            validate_identifier(sub_package, f"package name (index {idx})")

            kind = cls.get("kind", "class")
            if kind not in VALID_KINDS:
                raise ValidationError(
                    f"class {name!r} has invalid kind {kind!r}; "
                    f"expected one of {sorted(VALID_KINDS)}"
                )

            if kind not in supported_kinds:
                continue

            # Validate extends if present.
            extends_value = cls.get("extends")
            if extends_value is not None:
                validate_identifier(str(extends_value), f"extends in class {name!r}")

            # Validate each property.
            for prop in cls.get("properties", []):
                prop_name = prop.get("name")
                if not prop_name:
                    raise ValidationError(f"class {name!r} has a property without a name")
                validate_identifier(prop_name, f"property name in class {name!r}")
                prop_type = prop.get("type")
                if prop_type is None:
                    raise ValidationError(
                        f"property {prop_name!r} in class {name!r} has no type"
                    )
                validate_type_string(str(prop_type), f"property {prop_name!r} in class {name!r}")

        except ValidationError as e:
            errors.append(str(e))
            print(f"  ERROR: {e}", file=sys.stderr)
            continue

        # --- Generate source ---
        try:
            source = generate_source(cls, sub_package, class_index, preset)
        except (KeyError, TypeError) as e:
            msg = f"failed to generate source for class {name!r}: {e}"
            errors.append(msg)
            print(f"  ERROR: {msg}", file=sys.stderr)
            continue

        rel_path = Path(*BASE_PACKAGE.split(".")) / sub_package / f"{name}.java"
        out_file = out_root / rel_path

        # Safety: ensure the resolved path is still under out_root (defense in
        # depth, even though identifiers are validated above).
        try:
            out_file.resolve().relative_to(out_root.resolve())
        except ValueError:
            msg = f"refusing to write outside output root: {out_file}"
            errors.append(msg)
            print(f"  ERROR: {msg}", file=sys.stderr)
            continue

        if args.dry_run:
            print(f"  [dry-run] {out_file}  ({len(source)} bytes)")
            continue

        if out_file.exists() and not args.force:
            print(f"  skip (exists): {out_file}")
            skipped += 1
            continue

        try:
            atomic_write(out_file, source)
        except OSError as e:
            msg = f"failed to write {out_file}: {e}"
            errors.append(msg)
            print(f"  ERROR: {msg}", file=sys.stderr)
            continue

        print(f"  wrote: {out_file}")
        written += 1

    print()
    if args.dry_run:
        total = len(enum_types) + (1 if root_entity else 0) + len(intermediate_classes) + n_to_generate
        print(f"Dry run complete — {total} file(s) would be generated "
              f"({len(enum_types)} enums, {1 + len(intermediate_classes) if root_entity else len(intermediate_classes)} base classes, {n_to_generate} DTO classes).")
    else:
        print(f"Done — wrote {written} file(s), skipped {skipped} existing file(s).")
    if errors:
        print(f"\n{len(errors)} error(s) occurred:", file=sys.stderr)
        for err in errors:
            print(f"  - {err}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
