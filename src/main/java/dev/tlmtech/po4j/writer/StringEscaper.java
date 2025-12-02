package dev.tlmtech.po4j.writer;

import org.jspecify.annotations.Nullable;

/**
 * Escapes strings for output in PO file format.
 * Converts special characters to C-style escape sequences.
 *
 * <p>Characters that are escaped:
 * <ul>
 *   <li>{@code \} - to {@code \\}</li>
 *   <li>{@code "} - to {@code \"}</li>
 *   <li>newline - to {@code \n}</li>
 *   <li>tab - to {@code \t}</li>
 *   <li>carriage return - to {@code \r}</li>
 *   <li>control characters (0x00-0x1F, 0x7F) - to {@code \xHH}</li>
 * </ul>
 */
public final class StringEscaper {

    private StringEscaper() {
        // Utility class
    }

    /**
     * Escapes a string for PO file output.
     *
     * @param input the string to escape
     * @return the escaped string
     */
    public static @Nullable String escape(@Nullable String input) {
        if (input == null) {
            return null;
        }

        if (input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder(input.length() + 16);

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            switch (c) {
                case '\\' -> result.append("\\\\");
                case '"' -> result.append("\\\"");
                case '\n' -> result.append("\\n");
                case '\t' -> result.append("\\t");
                case '\r' -> result.append("\\r");
                case '\u000B' -> result.append("\\v"); // Vertical tab
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                case '\u0007' -> result.append("\\a"); // Bell
                case '\0' -> result.append("\\0");
                default -> {
                    if (c < 0x20 || c == 0x7F) {
                        // Control character - escape as hex
                        result.append(String.format("\\x%02X", (int) c));
                    } else {
                        // Printable character - keep as-is
                        result.append(c);
                    }
                }
            }
        }

        return result.toString();
    }

    /**
     * Checks if a string needs escaping.
     *
     * @param input the string to check
     * @return true if the string contains characters that need escaping
     */
    public static boolean needsEscaping(@Nullable String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\\'
                    || c == '"'
                    || c == '\n'
                    || c == '\t'
                    || c == '\r'
                    || c == '\u000B'
                    || c == '\b'
                    || c == '\f'
                    || c == '\u0007'
                    || c == '\0'
                    || c < 0x20
                    || c == 0x7F) {
                return true;
            }
        }

        return false;
    }

    /**
     * Quotes and escapes a string for PO file output.
     * The result includes surrounding double quotes.
     *
     * @param input the string to quote and escape
     * @return the quoted and escaped string, e.g., "escaped content"
     */
    public static String quoteAndEscape(String input) {
        return "\"" + escape(input) + "\"";
    }
}
