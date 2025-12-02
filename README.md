# po4j

[![](https://jitpack.io/v/ThilinaTLM/po4j.svg)](https://jitpack.io/#ThilinaTLM/po4j)
[![CI](https://github.com/ThilinaTLM/po4j/actions/workflows/ci.yml/badge.svg)](https://github.com/ThilinaTLM/po4j/actions/workflows/ci.yml)

A Java library for parsing and writing GNU gettext PO (Portable Object) files.

## Features

- Parse PO files from files, streams, or strings
- Write PO files with proper formatting
- Full support for PO file features:
    - Simple and plural message entries
    - Message context (msgctxt)
    - All comment types (translator, extracted, reference, flags, previous)
    - Header parsing with plural forms support
    - Obsolete entries
    - Multi-line strings
    - Escape sequences
- Builder API for creating PO files programmatically
- Zero dependencies (only JUnit for testing)

## Installation

### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.ThilinaTLM:po4j:0.1.0")
}
```

### Gradle (Groovy)

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.ThilinaTLM:po4j:0.1.0'
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.ThilinaTLM</groupId>
    <artifactId>po4j</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Usage

### Parsing a PO file

```java
import dev.tlmtech.po4j.model.*;
import dev.tlmtech.po4j.parser.POParser;

// From a file
try (InputStream is = new FileInputStream("messages.po")) {
    POFile poFile = POParser.parse(is, StandardCharsets.UTF_8);

    for (POEntry entry : poFile.getEntries()) {
        System.out.println(entry.getMsgid() + " -> " + entry.getMsgstr().orElse(""));
    }
}

// From a string
POFile poFile = POParser.parseString("""
    msgid "Hello"
    msgstr "Bonjour"
    """);
```

### Writing a PO file

```java
import dev.tlmtech.po4j.model.*;
import dev.tlmtech.po4j.writer.POWriter;

// Write to string
String output = POWriter.writeToString(poFile);

// Write to file
try (OutputStream os = new FileOutputStream("output.po")) {
    POWriter.write(poFile, os, StandardCharsets.UTF_8);
}
```

### Building PO files programmatically

```java
POFile poFile = POFile.builder()
    .header(POHeader.builder()
        .language("fr")
        .charset("UTF-8")
        .withDefaults()
        .build())
    .entry(POEntry.builder()
        .msgid("Hello")
        .msgstr("Bonjour")
        .addReference("main.c:10")
        .build())
    .entry(POEntry.builder()
        .msgctxt("greeting")
        .msgid("Hello")
        .msgstr("Salut")
        .addFlag("fuzzy")
        .build())
    .build();
```

### Working with plural forms

```java
POEntry pluralEntry = POEntry.builder()
    .msgid("One file")
    .msgidPlural("%d files")
    .msgstrPlural(List.of("Un fichier", "%d fichiers"))
    .build();

if (entry.isPlural()) {
    List<String> translations = entry.getMsgstrPlural();
}
```

### Accessing header information

```java
POHeader header = poFile.getHeader().orElse(null);
if (header != null) {
    String language = header.getLanguage().orElse("unknown");
    String charset = header.getCharset().orElse("UTF-8");

    PluralForms pf = header.getPluralForms().orElse(null);
    if (pf != null) {
        int nplurals = pf.getNplurals();
    }
}
```

## Requirements

- Java 17 or higher

## License

Apache License 2.0 - see [LICENSE](LICENSE) for details.
