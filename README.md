# po4j

[![](https://jitpack.io/v/ThilinaTLM/po4j.svg)](https://jitpack.io/#ThilinaTLM/po4j)
[![CI](https://github.com/ThilinaTLM/po4j/actions/workflows/ci.yml/badge.svg)](https://github.com/ThilinaTLM/po4j/actions/workflows/ci.yml)

A Java library for parsing and writing GNU gettext PO (Portable Object) files. Zero dependencies.

## Features

- Parse and write PO files from files, streams, or strings
- Full PO format support: singular/plural entries, context, all comment types, obsolete entries
- Immutable, thread-safe models with builder API
- Header parsing with plural forms support

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

## Quick Start

### Reading a PO File

```java
// From file
try (InputStream is = new FileInputStream("messages.po")) {
    POFile poFile = POParser.parse(is, StandardCharsets.UTF_8);

    for (POEntry entry : poFile.getEntries()) {
        System.out.println(entry.getMsgid() + " -> " + entry.getMsgstr().orElse(""));
    }
}

// From string
POFile poFile = POParser.parseString("""
    msgid "Hello"
    msgstr "Bonjour"
    """);
```

### Creating a PO File

```java
POFile poFile = POFile.builder()
    .header(POHeader.builder()
        .language("fr")
        .contentType("text/plain; charset=UTF-8")
        .withDefaults()
        .build())
    .entry(POEntry.builder()
        .msgid("Hello")
        .msgstr("Bonjour")
        .addReference("src/main.c:10")
        .build())
    .entry(POEntry.builder()
        .msgctxt("menu")
        .msgid("File")
        .msgstr("Fichier")
        .build())
    .build();
```

### Modifying a PO File

```java
// Load existing file
POFile original = POParser.parseString(existingContent);

// Find and update an entry
POFile modified = original.toBuilder()
    .clearEntries()
    .entries(original.getEntries().stream()
        .map(entry -> {
            if (entry.getMsgid().equals("Hello")) {
                return entry.toBuilder()
                    .msgstr("Salut")
                    .addFlag("fuzzy")
                    .build();
            }
            return entry;
        })
        .toList())
    .build();
```

### Writing a PO File

```java
// To string
String output = POWriter.writeToString(poFile);

// To file
try (OutputStream os = new FileOutputStream("output.po")) {
    POWriter.write(poFile, os, StandardCharsets.UTF_8);
}
```

## Advanced Usage

### Plural Forms

```java
// Create plural entry
POEntry entry = POEntry.builder()
    .msgid("One file")
    .msgidPlural("%d files")
    .msgstrPlural(List.of("Un fichier", "%d fichiers"))
    .build();

// Check and access plural translations
if (entry.isPlural()) {
    List<String> translations = entry.getMsgstrPlural();
}
```

### Working with Headers

```java
POHeader header = poFile.getHeader().orElse(null);
if (header != null) {
    String language = header.getLanguage().orElse("unknown");
    String charset = header.getCharset().orElse("UTF-8");

    // Access plural forms configuration
    header.getPluralForms().ifPresent(pf -> {
        int nplurals = pf.getNplurals();
        String formula = pf.getFormula();
    });
}
```

### Querying Entries

```java
// Find by msgid
Optional<POEntry> entry = poFile.findByMsgid("Hello");

// Find by msgid and context
Optional<POEntry> menuEntry = poFile.findByMsgidAndContext("File", "menu");

// Get statistics
int translated = poFile.getTranslatedCount();
int fuzzy = poFile.getFuzzyCount();
int untranslated = poFile.getUntranslatedCount();

// Get entries by status
List<POEntry> fuzzyEntries = poFile.getFuzzyEntries();
List<POEntry> untranslatedEntries = poFile.getUntranslatedEntries();
```

## Requirements

- Java 17 or higher

## License

Apache License 2.0 - see [LICENSE](LICENSE) for details.
