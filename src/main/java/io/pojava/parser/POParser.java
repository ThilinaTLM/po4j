package io.pojava.parser;

import io.pojava.model.*;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Parser for GNU gettext PO files.
 *
 * <p>This parser handles the complete PO file format including:
 * <ul>
 *   <li>Header entries</li>
 *   <li>Simple entries (msgid + msgstr)</li>
 *   <li>Entries with context (msgctxt)</li>
 *   <li>Plural entries (msgid_plural + msgstr[n])</li>
 *   <li>Obsolete entries (#~)</li>
 *   <li>All comment types</li>
 *   <li>Previous values for fuzzy matching</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * try (InputStream is = new FileInputStream("messages.po")) {
 *     POFile poFile = POParser.parse(is, StandardCharsets.UTF_8);
 *     // Use poFile...
 * }
 * }</pre>
 */
public class POParser implements Closeable {

    private final POLexer lexer;
    private final POParserOptions options;

    /**
     * Creates a parser with the given lexer and options.
     */
    public POParser(POLexer lexer, POParserOptions options) {
        this.lexer = Objects.requireNonNull(lexer);
        this.options = options != null ? options : POParserOptions.defaults();
    }

    /**
     * Creates a parser for the given reader.
     */
    public POParser(Reader reader) {
        this(new POLexer(reader), null);
    }

    /**
     * Creates a parser for the given input stream with UTF-8 encoding.
     */
    public POParser(InputStream inputStream) throws UnsupportedEncodingException {
        this(new POLexer(inputStream, "UTF-8"), null);
    }

    // --- Static factory methods ---

    /**
     * Parses a PO file from an input stream.
     */
    public static POFile parse(InputStream inputStream, Charset charset) throws IOException {
        return parse(inputStream, charset, POParserOptions.defaults());
    }

    /**
     * Parses a PO file from an input stream with options.
     */
    public static POFile parse(InputStream inputStream, Charset charset, POParserOptions options)
            throws IOException {
        try (POParser parser = new POParser(
                new POLexer(new InputStreamReader(inputStream, charset)),
                options)) {
            return parser.parse();
        }
    }

    /**
     * Parses a PO file from a reader.
     */
    public static POFile parse(Reader reader) throws IOException {
        return parse(reader, POParserOptions.defaults());
    }

    /**
     * Parses a PO file from a reader with options.
     */
    public static POFile parse(Reader reader, POParserOptions options) throws IOException {
        try (POParser parser = new POParser(new POLexer(reader), options)) {
            return parser.parse();
        }
    }

    /**
     * Parses a PO file from a string.
     */
    public static POFile parseString(String content) throws IOException {
        return parse(new StringReader(content));
    }

    /**
     * Parses a PO file from a string with options.
     */
    public static POFile parseString(String content, POParserOptions options) throws IOException {
        return parse(new StringReader(content), options);
    }

    // --- Main parse method ---

    /**
     * Parses the PO file and returns the result.
     */
    public POFile parse() throws IOException {
        POFile.Builder builder = POFile.builder();

        while (true) {
            Token token = lexer.peek();

            if (token.isEof()) {
                break;
            }

            try {
                POEntry entry = parseEntry();
                if (entry != null) {
                    builder.entry(entry);
                }
            } catch (ParseException e) {
                if (options.isStrict()) {
                    throw e;
                }
                // Skip to next entry in lenient mode
                skipToNextEntry();
            }
        }

        return builder.build();
    }

    // --- Entry parsing ---

    private POEntry parseEntry() throws IOException {
        POEntry.Builder builder = POEntry.builder();

        // Check for obsolete prefix
        boolean isObsolete = false;
        Token token = lexer.peek();

        if (token.type() == TokenType.OBSOLETE_PREFIX) {
            lexer.nextToken(); // Consume #~
            isObsolete = true;
            builder.obsolete(true);
        }

        // Parse comments
        parseComments(builder, isObsolete);

        // Re-check for obsolete prefix after comments
        token = lexer.peek();
        if (token.type() == TokenType.OBSOLETE_PREFIX) {
            lexer.nextToken();
            isObsolete = true;
            builder.obsolete(true);
        }

        // Parse msgctxt (optional)
        token = lexer.peek();
        if (token.type() == TokenType.MSGCTXT) {
            lexer.nextToken();
            String msgctxt = parseStringValue();
            builder.msgctxt(msgctxt);

            // Handle obsolete continuation
            if (isObsolete) {
                consumeObsoletePrefix();
            }
        }

        // Parse msgid (required)
        token = lexer.peek();
        if (token.type() != TokenType.MSGID) {
            if (token.isEof()) {
                return null; // No more entries
            }
            throw new ParseException(
                    "Expected msgid, found " + token.type(),
                    token.line(), token.column()
            );
        }
        lexer.nextToken();
        String msgid = parseStringValue();
        builder.msgid(msgid);

        // Handle obsolete continuation
        if (isObsolete) {
            consumeObsoletePrefix();
        }

        // Parse msgid_plural (optional)
        token = lexer.peek();
        if (token.type() == TokenType.MSGID_PLURAL) {
            lexer.nextToken();
            String msgidPlural = parseStringValue();
            builder.msgidPlural(msgidPlural);

            if (isObsolete) {
                consumeObsoletePrefix();
            }

            // Parse plural msgstr values
            parsePluralMsgstr(builder, isObsolete);
        } else {
            // Parse singular msgstr
            token = lexer.peek();
            if (token.type() != TokenType.MSGSTR) {
                throw new ParseException(
                        "Expected msgstr, found " + token.type(),
                        token.line(), token.column()
                );
            }
            lexer.nextToken();
            String msgstr = parseStringValue();
            builder.msgstr(msgstr);
        }

        return builder.build();
    }

    private void parseComments(POEntry.Builder builder, boolean inObsolete) throws IOException {
        while (true) {
            Token token = lexer.peek();

            if (inObsolete && token.type() == TokenType.OBSOLETE_PREFIX) {
                lexer.nextToken();
                token = lexer.peek();
            }

            if (!token.type().isComment()) {
                break;
            }

            lexer.nextToken();
            String value = token.value() != null ? token.value() : "";

            switch (token.type()) {
                case COMMENT_TRANSLATOR -> builder.addTranslatorComment(value);
                case COMMENT_EXTRACTED -> builder.addExtractedComment(value);
                case COMMENT_REFERENCE -> parseReferences(builder, value);
                case COMMENT_FLAG -> parseFlags(builder, value);
                case COMMENT_PREVIOUS -> parsePrevious(builder, value);
                default -> { /* Ignore */ }
            }
        }
    }

    private void parseReferences(POEntry.Builder builder, String value) {
        // References are space-separated on a single line
        // Format: filename:line filename:line ...
        if (value == null || value.isEmpty()) {
            return;
        }
        String[] refs = value.trim().split("\\s+");
        for (String ref : refs) {
            if (!ref.isEmpty()) {
                builder.addReference(ref);
            }
        }
    }

    private void parseFlags(POEntry.Builder builder, String value) {
        // Flags are comma-separated
        if (value == null || value.isEmpty()) {
            return;
        }
        String[] flags = value.split(",");
        for (String flag : flags) {
            String trimmed = flag.trim();
            if (!trimmed.isEmpty()) {
                builder.addFlag(trimmed);
            }
        }
    }

    private void parsePrevious(POEntry.Builder builder, String value) throws IOException {
        // Previous value format: msgid "string" or msgctxt "string" or msgid_plural "string"
        if (value == null || value.isEmpty()) {
            return;
        }

        String trimmed = value.trim();
        if (trimmed.startsWith("msgctxt ")) {
            String strPart = trimmed.substring(8).trim();
            builder.previousMsgctxt(unquote(strPart));
        } else if (trimmed.startsWith("msgid_plural ")) {
            String strPart = trimmed.substring(13).trim();
            builder.previousMsgidPlural(unquote(strPart));
        } else if (trimmed.startsWith("msgid ")) {
            String strPart = trimmed.substring(6).trim();
            builder.previousMsgid(unquote(strPart));
        }
    }

    private String unquote(String quoted) {
        if (quoted == null || quoted.isEmpty()) {
            return "";
        }
        // Handle multi-part quoted strings
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < quoted.length()) {
            // Find start of quoted segment
            int start = quoted.indexOf('"', i);
            if (start < 0) break;

            // Find end of quoted segment
            int end = start + 1;
            boolean escaped = false;
            while (end < quoted.length()) {
                char c = quoted.charAt(end);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    break;
                }
                end++;
            }

            if (end < quoted.length()) {
                String segment = quoted.substring(start + 1, end);
                result.append(StringUnescaper.unescapeLenient(segment));
            }

            i = end + 1;
        }
        return result.toString();
    }

    private void parsePluralMsgstr(POEntry.Builder builder, boolean isObsolete) throws IOException {
        Map<Integer, String> plurals = new TreeMap<>();

        while (true) {
            if (isObsolete) {
                consumeObsoletePrefix();
            }

            Token token = lexer.peek();
            if (token.type() != TokenType.MSGSTR_PLURAL) {
                break;
            }

            lexer.nextToken();
            int index = token.getPluralIndex();
            String value = parseStringValue();
            plurals.put(index, value);
        }

        // Convert to list, filling gaps with empty strings
        if (!plurals.isEmpty()) {
            int maxIndex = plurals.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
            List<String> msgstrPlural = new ArrayList<>(maxIndex + 1);
            for (int i = 0; i <= maxIndex; i++) {
                msgstrPlural.add(plurals.getOrDefault(i, ""));
            }
            builder.msgstrPlural(msgstrPlural);
        }
    }

    private String parseStringValue() throws IOException {
        StringBuilder result = new StringBuilder();

        while (true) {
            Token token = lexer.peek();
            if (token.type() != TokenType.STRING) {
                break;
            }
            lexer.nextToken();
            result.append(token.value());
        }

        return result.toString();
    }

    private void consumeObsoletePrefix() throws IOException {
        Token token = lexer.peek();
        if (token.type() == TokenType.OBSOLETE_PREFIX) {
            lexer.nextToken();
        }
    }

    private void skipToNextEntry() throws IOException {
        // Skip tokens until we find what looks like the start of a new entry
        // (comment at start of line or msgid/msgctxt after blank line)
        int blankLines = 0;

        while (true) {
            Token token = lexer.nextToken();

            if (token.isEof()) {
                break;
            }

            // A blank line followed by a keyword or comment suggests new entry
            if (token.type().isComment() || token.type().isKeyword()
                    || token.type() == TokenType.OBSOLETE_PREFIX) {
                // Put it back and let the main loop handle it
                // Note: We can't really "unread" a token, so we'll just
                // check if this looks like a valid entry start and return
                break;
            }
        }
    }

    @Override
    public void close() throws IOException {
        lexer.close();
    }
}
