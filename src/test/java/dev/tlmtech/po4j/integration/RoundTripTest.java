package dev.tlmtech.po4j.integration;

import dev.tlmtech.po4j.model.POEntry;
import dev.tlmtech.po4j.model.POFile;
import dev.tlmtech.po4j.model.POHeader;
import dev.tlmtech.po4j.model.PluralForms;
import dev.tlmtech.po4j.parser.POParser;
import dev.tlmtech.po4j.writer.POWriter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests verifying that parsing and writing PO files
 * preserves all content correctly.
 */
class RoundTripTest {

    @Test
    void testSimpleEntry() throws IOException {
        String input = """
                msgid "Hello"
                msgstr "Bonjour"
                """;

        POFile parsed = POParser.parseString(input);

        assertEquals(1, parsed.size());
        POEntry entry = parsed.getEntries().get(0);
        assertEquals("Hello", entry.getMsgid());
        assertEquals("Bonjour", entry.getMsgstr().orElse(null));

        String output = POWriter.writeToString(parsed);
        POFile reparsed = POParser.parseString(output);

        assertEquals(parsed.size(), reparsed.size());
        assertEquals(entry.getMsgid(), reparsed.getEntries().get(0).getMsgid());
        assertEquals(entry.getMsgstr().orElse(null), reparsed.getEntries().get(0).getMsgstr().orElse(null));
    }

    @Test
    void testEntryWithContext() throws IOException {
        String input = """
                msgctxt "menu"
                msgid "File"
                msgstr "Fichier"
                """;

        POFile parsed = POParser.parseString(input);
        POEntry entry = parsed.getEntries().get(0);

        assertEquals("menu", entry.getMsgctxt().orElse(null));
        assertEquals("File", entry.getMsgid());
        assertEquals("Fichier", entry.getMsgstr().orElse(null));

        String output = POWriter.writeToString(parsed);
        POFile reparsed = POParser.parseString(output);

        POEntry reparsedEntry = reparsed.getEntries().get(0);
        assertEquals("menu", reparsedEntry.getMsgctxt().orElse(null));
    }

    @Test
    void testPluralEntry() throws IOException {
        String input = """
                msgid "One file"
                msgid_plural "%d files"
                msgstr[0] "Un fichier"
                msgstr[1] "%d fichiers"
                """;

        POFile parsed = POParser.parseString(input);
        POEntry entry = parsed.getEntries().get(0);

        assertTrue(entry.isPlural());
        assertEquals("One file", entry.getMsgid());
        assertEquals("%d files", entry.getMsgidPlural().orElse(null));
        assertEquals(2, entry.getMsgstrPlural().size());
        assertEquals("Un fichier", entry.getMsgstrPlural().get(0));
        assertEquals("%d fichiers", entry.getMsgstrPlural().get(1));

        String output = POWriter.writeToString(parsed);
        POFile reparsed = POParser.parseString(output);

        POEntry reparsedEntry = reparsed.getEntries().get(0);
        assertTrue(reparsedEntry.isPlural());
        assertEquals(2, reparsedEntry.getMsgstrPlural().size());
    }

    @Test
    void testAllCommentTypes() throws IOException {
        String input = """
                # Translator comment
                #. Extracted comment
                #: file.c:123 file.c:456
                #, fuzzy, c-format
                msgid "Test"
                msgstr "Test translation"
                """;

        POFile parsed = POParser.parseString(input);
        POEntry entry = parsed.getEntries().get(0);

        assertEquals(List.of("Translator comment"), entry.getTranslatorComments());
        assertEquals(List.of("Extracted comment"), entry.getExtractedComments());
        assertEquals(List.of("file.c:123", "file.c:456"), entry.getReferences());
        assertTrue(entry.hasFlag("fuzzy"));
        assertTrue(entry.hasFlag("c-format"));
        assertTrue(entry.isFuzzy());

        String output = POWriter.writeToString(parsed);
        POFile reparsed = POParser.parseString(output);

        POEntry reparsedEntry = reparsed.getEntries().get(0);
        assertEquals(entry.getTranslatorComments(), reparsedEntry.getTranslatorComments());
        assertTrue(reparsedEntry.isFuzzy());
    }

    @Test
    void testHeader() throws IOException {
        String input = """
                msgid ""
                msgstr ""
                "Project-Id-Version: Test 1.0\\n"
                "Language: fr\\n"
                "Content-Type: text/plain; charset=UTF-8\\n"
                "Plural-Forms: nplurals=2; plural=(n > 1);\\n"
                
                msgid "Hello"
                msgstr "Bonjour"
                """;

        POFile parsed = POParser.parseString(input);

        assertTrue(parsed.getHeader().isPresent());
        POHeader header = parsed.getHeader().get();

        assertEquals("Test 1.0", header.getProjectIdVersion().orElse(null));
        assertEquals("fr", header.getLanguage().orElse(null));
        assertEquals("UTF-8", header.getCharset().orElse(null));

        PluralForms pf = header.getPluralForms().orElse(null);
        assertNotNull(pf);
        assertEquals(2, pf.getNplurals());

        String output = POWriter.writeToString(parsed);
        POFile reparsed = POParser.parseString(output);

        assertTrue(reparsed.getHeader().isPresent());
        assertEquals("fr", reparsed.getHeader().get().getLanguage().orElse(null));
    }

    @Test
    void testMultiLineStrings() throws IOException {
        String input = """
                msgid ""
                "First line\\n"
                "Second line\\n"
                "Third line"
                msgstr ""
                "Première ligne\\n"
                "Deuxième ligne\\n"
                "Troisième ligne"
                """;

        POFile parsed = POParser.parseString(input);
        POEntry entry = parsed.getEntries().get(0);

        assertEquals("First line\nSecond line\nThird line", entry.getMsgid());
        assertEquals("Première ligne\nDeuxième ligne\nTroisième ligne",
                entry.getMsgstr().orElse(null));

        String output = POWriter.writeToString(parsed);
        POFile reparsed = POParser.parseString(output);

        assertEquals(entry.getMsgid(), reparsed.getEntries().get(0).getMsgid());
    }

    @Test
    void testEscapeSequences() throws IOException {
        String input = """
                msgid "Tab:\\tNewline:\\nQuote:\\""
                msgstr "Tab:\\tNouvelle ligne:\\nGuillemet:\\""
                """;

        POFile parsed = POParser.parseString(input);
        POEntry entry = parsed.getEntries().get(0);

        assertEquals("Tab:\tNewline:\nQuote:\"", entry.getMsgid());

        String output = POWriter.writeToString(parsed);
        POFile reparsed = POParser.parseString(output);

        assertEquals(entry.getMsgid(), reparsed.getEntries().get(0).getMsgid());
    }

    @Test
    void testObsoleteEntry() throws IOException {
        String input = """
                msgid "Current"
                msgstr "Actuel"
                
                #~ msgid "Old"
                #~ msgstr "Ancien"
                """;

        POFile parsed = POParser.parseString(input);

        assertEquals(1, parsed.getEntries().size());
        assertEquals(1, parsed.getObsoleteEntries().size());

        POEntry obsolete = parsed.getObsoleteEntries().get(0);
        assertTrue(obsolete.isObsolete());
        assertEquals("Old", obsolete.getMsgid());

        String output = POWriter.writeToString(parsed);
        POFile reparsed = POParser.parseString(output);

        assertEquals(1, reparsed.getObsoleteEntries().size());
    }

    @Test
    void testPreviousValues() throws IOException {
        String input = """
                #| msgid "Previous message"
                msgid "Current message"
                msgstr "Message actuel"
                """;

        POFile parsed = POParser.parseString(input);
        POEntry entry = parsed.getEntries().get(0);

        assertEquals("Previous message", entry.getPreviousMsgid().orElse(null));

        String output = POWriter.writeToString(parsed);
        assertTrue(output.contains("#| msgid"));

        POFile reparsed = POParser.parseString(output);
        assertEquals("Previous message",
                reparsed.getEntries().get(0).getPreviousMsgid().orElse(null));
    }

    @Test
    void testMultipleEntries() throws IOException {
        String input = """
                msgid "First"
                msgstr "Premier"
                
                msgid "Second"
                msgstr "Deuxième"
                
                msgid "Third"
                msgstr "Troisième"
                """;

        POFile parsed = POParser.parseString(input);

        assertEquals(3, parsed.size());
        assertEquals("First", parsed.getEntries().get(0).getMsgid());
        assertEquals("Second", parsed.getEntries().get(1).getMsgid());
        assertEquals("Third", parsed.getEntries().get(2).getMsgid());

        String output = POWriter.writeToString(parsed);
        POFile reparsed = POParser.parseString(output);

        assertEquals(3, reparsed.size());
    }

    @Test
    void testUnicodeContent() throws IOException {
        String input = """
                msgid "Hello"
                msgstr "你好"
                
                msgid "Goodbye"
                msgstr "さようなら"
                """;

        POFile parsed = POParser.parseString(input);

        assertEquals("你好", parsed.getEntries().get(0).getMsgstr().orElse(null));
        assertEquals("さようなら", parsed.getEntries().get(1).getMsgstr().orElse(null));

        String output = POWriter.writeToString(parsed);
        POFile reparsed = POParser.parseString(output);

        assertEquals("你好", reparsed.getEntries().get(0).getMsgstr().orElse(null));
        assertEquals("さようなら", reparsed.getEntries().get(1).getMsgstr().orElse(null));
    }

    @Test
    void testEmptyMsgstr() throws IOException {
        String input = """
                msgid "Untranslated"
                msgstr ""
                """;

        POFile parsed = POParser.parseString(input);
        POEntry entry = parsed.getEntries().get(0);

        assertEquals("Untranslated", entry.getMsgid());
        assertEquals("", entry.getMsgstr().orElse(null));
        assertFalse(entry.isTranslated());

        String output = POWriter.writeToString(parsed);
        POFile reparsed = POParser.parseString(output);

        assertFalse(reparsed.getEntries().get(0).isTranslated());
    }

    @Test
    void testBuilderAPI() throws IOException {
        POFile poFile = POFile.builder()
                .header(POHeader.builder()
                        .language("de")
                        .withDefaults()
                        .build())
                .entry(POEntry.builder()
                        .msgid("Hello")
                        .msgstr("Hallo")
                        .addReference("main.c:10")
                        .build())
                .entry(POEntry.builder()
                        .msgctxt("greeting")
                        .msgid("Hello")
                        .msgstr("Grüß Gott")
                        .build())
                .build();

        assertEquals(2, poFile.size());
        assertEquals("de", poFile.getHeader().get().getLanguage().orElse(null));

        String output = POWriter.writeToString(poFile);
        POFile reparsed = POParser.parseString(output);

        assertEquals(2, reparsed.size());
        assertEquals("de", reparsed.getHeader().get().getLanguage().orElse(null));
    }
}
