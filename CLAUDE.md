# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

po4j is a Java library for parsing and writing GNU gettext PO (Portable Object) files. It provides full support for the PO file format including plural forms, message context, all comment types, obsolete entries, and C escape sequences. Zero external dependencies.

## Build Commands

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "dev.tlmtech.po4j.integration.RoundTripTest"

# Run a single test method
./gradlew test --tests "dev.tlmtech.po4j.integration.RoundTripTest.testBasicRoundTrip"

# Check formatting
./gradlew spotlessCheck

# Apply formatting fixes
./gradlew spotlessApply

# Generate Javadoc
./gradlew javadoc
```

## Architecture

### Package Structure

- `dev.tlmtech.po4j.model` - Immutable data models: `POFile`, `POEntry`, `POHeader`, `PluralForms`
- `dev.tlmtech.po4j.parser` - Lexer-based parser: `POLexer` tokenizes, `POParser` builds the model
- `dev.tlmtech.po4j.writer` - Serialization to PO format with configurable options

### Key Design Patterns

- **Immutable models**: All model classes (`POFile`, `POEntry`, `POHeader`) are immutable and thread-safe
- **Builder pattern**: All models use builders for construction (e.g., `POEntry.builder().msgid("...").msgstr("...").build()`)
- **Static factory methods**: Both `POParser` and `POWriter` expose static methods for common operations (`POParser.parse()`, `POParser.parseString()`, `POWriter.write()`, `POWriter.writeToString()`)
- **Lexer-parser separation**: `POLexer` produces tokens (`Token`/`TokenType`), `POParser` consumes them to build the AST

### Parser Flow

1. `POLexer` reads character stream and produces tokens (keywords like `msgid`, `msgstr`, string literals, comments)
2. `POParser` consumes tokens and builds `POEntry` objects using the builder
3. `POFile.Builder` automatically routes header entries (empty msgid) to `POHeader` and obsolete entries (#~) to a separate list

### String Handling

- `StringUnescaper` - Converts PO escape sequences (\n, \t, \\, \", octal, hex) to actual characters during parsing
- `StringEscaper` - Converts special characters back to escape sequences during writing

## Code Conventions

- Java 17+ required
- Formatting: Palantir Java Format (enforced via Spotless)
- Use `Optional<T>` for nullable return values
- Comprehensive JavaDoc for public APIs
