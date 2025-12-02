package io.pojava.integration;

import io.pojava.model.*;
import io.pojava.parser.POParser;
import io.pojava.writer.POWriter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests parsing a sample PO file with various features.
 */
class SampleFileTest {

    @Test
    void testParseSampleFile() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/po/sample.po")) {
            assertNotNull(is, "Sample PO file not found");

            POFile poFile = POParser.parse(is, StandardCharsets.UTF_8);

            // Check header
            assertTrue(poFile.getHeader().isPresent());
            POHeader header = poFile.getHeader().get();
            assertEquals("Test 1.0", header.getProjectIdVersion().orElse(null));
            assertEquals("fr", header.getLanguage().orElse(null));
            assertEquals("UTF-8", header.getCharset().orElse(null));

            PluralForms pf = header.getPluralForms().orElse(null);
            assertNotNull(pf);
            assertEquals(2, pf.getNplurals());
            assertEquals("(n > 1)", pf.getPluralExpression());

            // Check entries (7 non-obsolete: hello, welcome, morning, files, file-menu, file-doc, current)
            assertEquals(7, poFile.size(), "Expected 7 non-obsolete entries");

            // Check simple entry
            POEntry hello = poFile.findByMsgid("Hello, World!").orElse(null);
            assertNotNull(hello);
            assertEquals("Bonjour, le monde !", hello.getMsgstr().orElse(null));
            assertEquals(1, hello.getReferences().size());

            // Check c-format entry
            POEntry welcome = poFile.findByMsgid("Welcome, %s!").orElse(null);
            assertNotNull(welcome);
            assertTrue(welcome.hasFlag("c-format"));

            // Check fuzzy entry
            POEntry morning = poFile.findByMsgid("Good morning").orElse(null);
            assertNotNull(morning);
            assertTrue(morning.isFuzzy());
            assertEquals(1, morning.getTranslatorComments().size());
            assertEquals(1, morning.getExtractedComments().size());
            assertEquals(2, morning.getReferences().size());

            // Check plural entry
            POEntry files = poFile.findByMsgid("One file").orElse(null);
            assertNotNull(files);
            assertTrue(files.isPlural());
            assertEquals("%d files", files.getMsgidPlural().orElse(null));
            assertEquals(2, files.getMsgstrPlural().size());
            assertEquals("Un fichier", files.getMsgstrPlural().get(0));
            assertEquals("%d fichiers", files.getMsgstrPlural().get(1));

            // Check context entries
            POEntry fileMenu = poFile.findByMsgidAndContext("File", "menu").orElse(null);
            assertNotNull(fileMenu);
            assertEquals("Fichier", fileMenu.getMsgstr().orElse(null));

            POEntry fileDoc = poFile.findByMsgidAndContext("File", "document").orElse(null);
            assertNotNull(fileDoc);
            assertEquals("Dossier", fileDoc.getMsgstr().orElse(null));

            // Check previous value
            POEntry current = poFile.findByMsgid("Current message").orElse(null);
            assertNotNull(current);
            assertEquals("Previous message", current.getPreviousMsgid().orElse(null));

            // Check obsolete entries
            assertEquals(2, poFile.getObsoleteEntries().size());
            POEntry obsolete = poFile.getObsoleteEntries().get(0);
            assertTrue(obsolete.isObsolete());
        }
    }

    @Test
    void testRoundTripSampleFile() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/po/sample.po")) {
            assertNotNull(is, "Sample PO file not found");

            POFile original = POParser.parse(is, StandardCharsets.UTF_8);

            // Write to string
            String output = POWriter.writeToString(original);

            // Parse again
            POFile reparsed = POParser.parseString(output);

            // Compare
            assertEquals(original.size(), reparsed.size());
            assertEquals(original.getObsoleteEntries().size(), reparsed.getObsoleteEntries().size());

            // Check header preserved
            assertTrue(reparsed.getHeader().isPresent());
            assertEquals(
                    original.getHeader().get().getLanguage().orElse(null),
                    reparsed.getHeader().get().getLanguage().orElse(null)
            );

            // Check each entry
            for (int i = 0; i < original.size(); i++) {
                POEntry orig = original.getEntries().get(i);
                POEntry repr = reparsed.getEntries().get(i);
                assertEquals(orig.getMsgid(), repr.getMsgid(), "Msgid mismatch at " + i);
                assertEquals(orig.getMsgstr().orElse(null), repr.getMsgstr().orElse(null),
                        "Msgstr mismatch at " + i);
                assertEquals(orig.getMsgctxt().orElse(null), repr.getMsgctxt().orElse(null),
                        "Msgctxt mismatch at " + i);
                assertEquals(orig.isPlural(), repr.isPlural(), "Plural mismatch at " + i);
            }
        }
    }
}
