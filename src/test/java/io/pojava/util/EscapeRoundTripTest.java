package io.pojava.util;

import io.pojava.parser.StringUnescaper;
import io.pojava.writer.StringEscaper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class EscapeRoundTripTest {

    @Test
    void testSimpleString() {
        String original = "Hello, World!";
        String escaped = StringEscaper.escape(original);
        String unescaped = StringUnescaper.unescape(escaped);
        assertEquals(original, unescaped);
    }

    @Test
    void testEmptyString() {
        assertEquals("", StringEscaper.escape(""));
        assertEquals("", StringUnescaper.unescape(""));
    }

    @Test
    void testNullString() {
        assertNull(StringEscaper.escape(null));
        assertNull(StringUnescaper.unescape(null));
    }

    @Test
    void testNewline() {
        String original = "Line 1\nLine 2";
        String escaped = StringEscaper.escape(original);
        assertEquals("Line 1\\nLine 2", escaped);
        assertEquals(original, StringUnescaper.unescape(escaped));
    }

    @Test
    void testTab() {
        String original = "Col1\tCol2";
        String escaped = StringEscaper.escape(original);
        assertEquals("Col1\\tCol2", escaped);
        assertEquals(original, StringUnescaper.unescape(escaped));
    }

    @Test
    void testBackslash() {
        String original = "path\\to\\file";
        String escaped = StringEscaper.escape(original);
        assertEquals("path\\\\to\\\\file", escaped);
        assertEquals(original, StringUnescaper.unescape(escaped));
    }

    @Test
    void testDoubleQuote() {
        String original = "He said \"Hello\"";
        String escaped = StringEscaper.escape(original);
        assertEquals("He said \\\"Hello\\\"", escaped);
        assertEquals(original, StringUnescaper.unescape(escaped));
    }

    @Test
    void testCarriageReturn() {
        String original = "Windows\r\nLine endings";
        String escaped = StringEscaper.escape(original);
        assertEquals("Windows\\r\\nLine endings", escaped);
        assertEquals(original, StringUnescaper.unescape(escaped));
    }

    @Test
    void testAllBasicEscapes() {
        String original = "\\\"\n\t\r";
        String escaped = StringEscaper.escape(original);
        assertEquals("\\\\\\\"\\n\\t\\r", escaped);
        assertEquals(original, StringUnescaper.unescape(escaped));
    }

    @Test
    void testVerticalTab() {
        String original = "before\u000Bafter";
        String escaped = StringEscaper.escape(original);
        assertEquals("before\\vafter", escaped);
        assertEquals(original, StringUnescaper.unescape(escaped));
    }

    @Test
    void testBackspace() {
        String original = "back\bspace";
        String escaped = StringEscaper.escape(original);
        assertEquals("back\\bspace", escaped);
        assertEquals(original, StringUnescaper.unescape(escaped));
    }

    @Test
    void testFormFeed() {
        String original = "form\ffeed";
        String escaped = StringEscaper.escape(original);
        assertEquals("form\\ffeed", escaped);
        assertEquals(original, StringUnescaper.unescape(escaped));
    }

    @Test
    void testBell() {
        String original = "alert\u0007bell";
        String escaped = StringEscaper.escape(original);
        assertEquals("alert\\abell", escaped);
        assertEquals(original, StringUnescaper.unescape(escaped));
    }

    @Test
    void testNullChar() {
        String original = "null\0char";
        String escaped = StringEscaper.escape(original);
        assertEquals("null\\0char", escaped);
        assertEquals(original, StringUnescaper.unescape(escaped));
    }

    @Test
    void testControlCharacterHex() {
        String original = "ctrl\u0001char";
        String escaped = StringEscaper.escape(original);
        assertEquals("ctrl\\x01char", escaped);
        assertEquals(original, StringUnescaper.unescape(escaped));
    }

    @Test
    void testOctalEscape() {
        // \101 = 'A' (65 in decimal, 101 in octal)
        String escaped = "\\101";
        String unescaped = StringUnescaper.unescape(escaped);
        assertEquals("A", unescaped);
    }

    @Test
    void testOctalEscapeMultipleDigits() {
        // \141 = 'a' (97 in decimal)
        assertEquals("a", StringUnescaper.unescape("\\141"));
        // \377 = 255
        assertEquals("\u00FF", StringUnescaper.unescape("\\377"));
    }

    @Test
    void testHexEscape() {
        // \x41 = 'A'
        assertEquals("A", StringUnescaper.unescape("\\x41"));
        // \xFF = 255
        assertEquals("\u00FF", StringUnescaper.unescape("\\xFF"));
        // \xAB (uppercase)
        assertEquals("\u00AB", StringUnescaper.unescape("\\xAB"));
        // \xab (lowercase)
        assertEquals("\u00AB", StringUnescaper.unescape("\\xab"));
    }

    @Test
    void testSingleOctalDigit() {
        // \0 = null
        assertEquals("\0", StringUnescaper.unescape("\\0"));
        // \7 = bell
        assertEquals("\u0007", StringUnescaper.unescape("\\7"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Simple text",
            "Unicode: café résumé naïve",
            "Japanese: 日本語",
            "Emoji: Hello 世界",
            "Mixed: Tab\there\nNewline\"Quote\\Backslash",
            ""
    })
    void testRoundTrip(String original) {
        String escaped = StringEscaper.escape(original);
        String unescaped = StringUnescaper.unescape(escaped);
        assertEquals(original, unescaped, "Round-trip failed for: " + original);
    }

    @Test
    void testUnknownEscape() {
        // Unknown escapes are passed through (backslash + char)
        String escaped = "\\q";
        String unescaped = StringUnescaper.unescape(escaped);
        assertEquals("\\q", unescaped);
    }

    @Test
    void testIncompleteEscapeThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                StringUnescaper.unescape("trailing\\"));
    }

    @Test
    void testInvalidHexEscapeThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                StringUnescaper.unescape("\\xZZ"));
    }

    @Test
    void testLenientIncompleteEscape() {
        // Trailing backslash is kept
        assertEquals("trailing\\", StringUnescaper.unescapeLenient("trailing\\"));
    }

    @Test
    void testLenientInvalidHex() {
        // Invalid hex escape is kept as-is
        assertEquals("\\xZZ", StringUnescaper.unescapeLenient("\\xZZ"));
    }

    @Test
    void testNeedsEscaping() {
        assertFalse(StringEscaper.needsEscaping("hello"));
        assertFalse(StringEscaper.needsEscaping(""));
        assertFalse(StringEscaper.needsEscaping(null));

        assertTrue(StringEscaper.needsEscaping("hello\n"));
        assertTrue(StringEscaper.needsEscaping("hello\\"));
        assertTrue(StringEscaper.needsEscaping("say \"hi\""));
        assertTrue(StringEscaper.needsEscaping("\t"));
        assertTrue(StringEscaper.needsEscaping("\u0001"));
    }

    @Test
    void testQuoteAndEscape() {
        assertEquals("\"hello\"", StringEscaper.quoteAndEscape("hello"));
        assertEquals("\"line1\\nline2\"", StringEscaper.quoteAndEscape("line1\nline2"));
        assertEquals("\"say \\\"hi\\\"\"", StringEscaper.quoteAndEscape("say \"hi\""));
    }

    @Test
    void testSingleQuoteEscape() {
        // Single quote escape (rarely used in PO files but valid in C)
        assertEquals("'", StringUnescaper.unescape("\\'"));
    }

    @Test
    void testQuestionMarkEscape() {
        // Question mark escape (C trigraph prevention)
        assertEquals("?", StringUnescaper.unescape("\\?"));
    }
}
