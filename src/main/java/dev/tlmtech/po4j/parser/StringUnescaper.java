package dev.tlmtech.po4j.parser;

/**
 * Unescapes C-style escape sequences in strings.
 * Used when parsing PO file strings.
 *
 * <p>Supported escape sequences:
 * <ul>
 *   <li>{@code \\} - backslash</li>
 *   <li>{@code \"} - double quote</li>
 *   <li>{@code \n} - newline (LF)</li>
 *   <li>{@code \t} - horizontal tab</li>
 *   <li>{@code \r} - carriage return</li>
 *   <li>{@code \v} - vertical tab</li>
 *   <li>{@code \b} - backspace</li>
 *   <li>{@code \f} - form feed</li>
 *   <li>{@code \a} - alert (bell)</li>
 *   <li>{@code \0} - null character</li>
 *   <li>{@code \ooo} - octal value (1-3 octal digits)</li>
 *   <li>{@code \xHH} - hexadecimal value (1+ hex digits, but typically 2)</li>
 * </ul>
 */
public final class StringUnescaper {

    private StringUnescaper() {
        // Utility class
    }

    /**
     * Unescapes a string with C-style escape sequences.
     *
     * @param input the escaped string
     * @return the unescaped string
     * @throws IllegalArgumentException if an invalid escape sequence is encountered
     */
    public static String unescape(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder(input.length());
        int i = 0;
        int len = input.length();

        while (i < len) {
            char c = input.charAt(i);

            if (c != '\\') {
                result.append(c);
                i++;
                continue;
            }

            // We have a backslash - need at least one more character
            if (i + 1 >= len) {
                throw new IllegalArgumentException("Incomplete escape sequence at end of string");
            }

            char next = input.charAt(i + 1);
            i += 2; // Consume backslash and escape character

            switch (next) {
                case '\\' -> result.append('\\');
                case '"' -> result.append('"');
                case '\'' -> result.append('\'');
                case 'n' -> result.append('\n');
                case 't' -> result.append('\t');
                case 'r' -> result.append('\r');
                case 'v' -> result.append('\u000B'); // Vertical tab
                case 'b' -> result.append('\b');
                case 'f' -> result.append('\f');
                case 'a' -> result.append('\u0007'); // Bell
                case '?' -> result.append('?'); // C trigraph escape (rarely used)
                case '0', '1', '2', '3', '4', '5', '6', '7' -> {
                    // Octal escape: \0 through \377 (1-3 octal digits)
                    int octalStart = i - 1; // Points to first digit
                    int octalValue = next - '0';

                    // Read up to 2 more octal digits
                    for (int j = 0; j < 2 && i < len; j++) {
                        char oc = input.charAt(i);
                        if (oc >= '0' && oc <= '7') {
                            int newValue = octalValue * 8 + (oc - '0');
                            if (newValue > 255) {
                                break; // Would exceed byte range
                            }
                            octalValue = newValue;
                            i++;
                        } else {
                            break;
                        }
                    }
                    result.append((char) octalValue);
                }
                case 'x' -> {
                    // Hexadecimal escape: \xHH (typically 2 hex digits)
                    if (i >= len) {
                        throw new IllegalArgumentException("Incomplete hex escape sequence at position " + (i - 2));
                    }

                    int hexValue = 0;
                    int hexDigits = 0;

                    // Read hex digits (at least 1 required, typically 2)
                    while (i < len && hexDigits < 2) {
                        char hc = input.charAt(i);
                        int digit = hexDigitValue(hc);
                        if (digit < 0) {
                            break;
                        }
                        hexValue = hexValue * 16 + digit;
                        hexDigits++;
                        i++;
                    }

                    if (hexDigits == 0) {
                        throw new IllegalArgumentException("Invalid hex escape sequence at position " + (i - 2));
                    }

                    result.append((char) hexValue);
                }
                default -> {
                    // Unknown escape - in lenient mode, keep the backslash and character
                    // In strict mode, we'd throw an exception
                    // For PO files, unknown escapes are kept as-is
                    result.append('\\').append(next);
                }
            }
        }

        return result.toString();
    }

    /**
     * Unescapes a string, but with lenient handling of invalid escapes.
     * Invalid escape sequences are kept as-is.
     *
     * @param input the escaped string
     * @return the unescaped string
     */
    public static String unescapeLenient(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder(input.length());
        int i = 0;
        int len = input.length();

        while (i < len) {
            char c = input.charAt(i);

            if (c != '\\') {
                result.append(c);
                i++;
                continue;
            }

            if (i + 1 >= len) {
                // Trailing backslash - keep it
                result.append('\\');
                i++;
                continue;
            }

            char next = input.charAt(i + 1);

            switch (next) {
                case '\\' -> {
                    result.append('\\');
                    i += 2;
                }
                case '"' -> {
                    result.append('"');
                    i += 2;
                }
                case '\'' -> {
                    result.append('\'');
                    i += 2;
                }
                case 'n' -> {
                    result.append('\n');
                    i += 2;
                }
                case 't' -> {
                    result.append('\t');
                    i += 2;
                }
                case 'r' -> {
                    result.append('\r');
                    i += 2;
                }
                case 'v' -> {
                    result.append('\u000B');
                    i += 2;
                }
                case 'b' -> {
                    result.append('\b');
                    i += 2;
                }
                case 'f' -> {
                    result.append('\f');
                    i += 2;
                }
                case 'a' -> {
                    result.append('\u0007');
                    i += 2;
                }
                case '?' -> {
                    result.append('?');
                    i += 2;
                }
                case '0', '1', '2', '3', '4', '5', '6', '7' -> {
                    i += 2;
                    int octalValue = next - '0';
                    for (int j = 0; j < 2 && i < len; j++) {
                        char oc = input.charAt(i);
                        if (oc >= '0' && oc <= '7') {
                            int newValue = octalValue * 8 + (oc - '0');
                            if (newValue > 255) break;
                            octalValue = newValue;
                            i++;
                        } else {
                            break;
                        }
                    }
                    result.append((char) octalValue);
                }
                case 'x' -> {
                    i += 2;
                    if (i >= len || hexDigitValue(input.charAt(i)) < 0) {
                        // Invalid hex escape - keep as-is
                        result.append("\\x");
                    } else {
                        int hexValue = 0;
                        int hexDigits = 0;
                        while (i < len && hexDigits < 2) {
                            int digit = hexDigitValue(input.charAt(i));
                            if (digit < 0) break;
                            hexValue = hexValue * 16 + digit;
                            hexDigits++;
                            i++;
                        }
                        result.append((char) hexValue);
                    }
                }
                default -> {
                    // Unknown escape - keep both characters
                    result.append('\\').append(next);
                    i += 2;
                }
            }
        }

        return result.toString();
    }

    private static int hexDigitValue(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        return -1;
    }
}
