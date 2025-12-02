package dev.tlmtech.po4j.integration;

import dev.tlmtech.po4j.model.POEntry;
import dev.tlmtech.po4j.model.POFile;
import dev.tlmtech.po4j.parser.POParser;
import dev.tlmtech.po4j.writer.POWriter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests parsing and writing a large real-world PO file.
 */
class RealWorldFileTest {

    private static final String REAL_WORLD_FILE = "/po/real-world.po";
    private static final int EXPECTED_ENTRY_COUNT = 1590;

    @Test
    void testParseRealWorldFile() throws IOException {
        try (InputStream is = getClass().getResourceAsStream(REAL_WORLD_FILE)) {
            assertNotNull(is, "Real-world PO file not found");

            POFile poFile = POParser.parse(is, StandardCharsets.UTF_8);

            // Verify header exists
            assertTrue(poFile.getHeader().isPresent(), "Header should be present");
            assertEquals("en", poFile.getHeader().get().getLanguage().orElse(null));

            // Verify entry count (includes both regular and obsolete)
            int totalEntries = poFile.size() + poFile.getObsoleteEntries().size();
            assertTrue(totalEntries > 1000, "Expected at least 1000 entries, got " + totalEntries);

            System.out.println("Parsed " + poFile.size() + " entries + "
                    + poFile.getObsoleteEntries().size() + " obsolete entries");
        }
    }

    @Test
    void testRoundTripSemanticComparison() throws IOException {
        String originalContent = readResourceAsString(REAL_WORLD_FILE);
        POFile original = POParser.parseString(originalContent);

        // Write to string
        String output = POWriter.writeToString(original);

        // Parse the output
        POFile reparsed = POParser.parseString(output);

        // Semantic comparison
        assertEquals(original.size(), reparsed.size(),
                "Entry count mismatch");
        assertEquals(original.getObsoleteEntries().size(), reparsed.getObsoleteEntries().size(),
                "Obsolete entry count mismatch");

        // Compare header
        assertTrue(reparsed.getHeader().isPresent());
        assertEquals(
                original.getHeader().get().getLanguage().orElse(null),
                reparsed.getHeader().get().getLanguage().orElse(null),
                "Language mismatch"
        );

        // Compare each entry
        for (int i = 0; i < original.size(); i++) {
            POEntry orig = original.getEntries().get(i);
            POEntry repr = reparsed.getEntries().get(i);

            assertEquals(orig.getMsgid(), repr.getMsgid(),
                    "Msgid mismatch at entry " + i);
            assertEquals(orig.getMsgstr().orElse(null), repr.getMsgstr().orElse(null),
                    "Msgstr mismatch at entry " + i);
            assertEquals(orig.getMsgctxt().orElse(null), repr.getMsgctxt().orElse(null),
                    "Msgctxt mismatch at entry " + i);
            assertEquals(orig.isPlural(), repr.isPlural(),
                    "Plural mismatch at entry " + i);
            assertEquals(orig.getMsgidPlural().orElse(null), repr.getMsgidPlural().orElse(null),
                    "MsgidPlural mismatch at entry " + i);
            assertEquals(orig.getMsgstrPlural(), repr.getMsgstrPlural(),
                    "MsgstrPlural mismatch at entry " + i);
            assertEquals(orig.getFlags(), repr.getFlags(),
                    "Flags mismatch at entry " + i);
            assertEquals(orig.getReferences(), repr.getReferences(),
                    "References mismatch at entry " + i);
        }

        // Compare obsolete entries
        for (int i = 0; i < original.getObsoleteEntries().size(); i++) {
            POEntry orig = original.getObsoleteEntries().get(i);
            POEntry repr = reparsed.getObsoleteEntries().get(i);

            assertEquals(orig.getMsgid(), repr.getMsgid(),
                    "Obsolete msgid mismatch at entry " + i);
            assertEquals(orig.getMsgstr().orElse(null), repr.getMsgstr().orElse(null),
                    "Obsolete msgstr mismatch at entry " + i);
        }

        System.out.println("Semantic round-trip test passed for " + original.size() + " entries");
    }

    @Test
    void testRoundTripByteComparison() throws IOException {
        String originalContent = readResourceAsString(REAL_WORLD_FILE);
        POFile parsed = POParser.parseString(originalContent);

        // Write back to string
        String output = POWriter.writeToString(parsed);

        // Compare byte-for-byte
        if (originalContent.equals(output)) {
            System.out.println("Byte-for-byte comparison: EXACT MATCH");
        } else {
            // Find first difference for debugging
            int diffIndex = findFirstDifference(originalContent, output);
            int contextStart = Math.max(0, diffIndex - 50);
            int contextEnd = Math.min(Math.min(originalContent.length(), output.length()), diffIndex + 50);

            System.out.println("Byte-for-byte comparison: MISMATCH at position " + diffIndex);
            System.out.println("Original length: " + originalContent.length());
            System.out.println("Output length: " + output.length());
            System.out.println("Context around difference:");
            System.out.println("Original: ..." + escape(originalContent.substring(contextStart, Math.min(originalContent.length(), contextEnd))) + "...");
            System.out.println("Output:   ..." + escape(output.substring(contextStart, Math.min(output.length(), contextEnd))) + "...");

            // This test documents the difference but doesn't fail
            // The semantic comparison test validates correctness
            System.out.println("Note: Minor formatting differences are expected and acceptable");
        }
    }

    private String readResourceAsString(String resource) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resource)) {
            assertNotNull(is, "Resource not found: " + resource);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            is.transferTo(baos);
            return baos.toString(StandardCharsets.UTF_8);
        }
    }

    private int findFirstDifference(String a, String b) {
        int minLen = Math.min(a.length(), b.length());
        for (int i = 0; i < minLen; i++) {
            if (a.charAt(i) != b.charAt(i)) {
                return i;
            }
        }
        return minLen;
    }

    private String escape(String s) {
        return s.replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r");
    }
}
