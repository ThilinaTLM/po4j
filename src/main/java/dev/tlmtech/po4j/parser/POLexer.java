package dev.tlmtech.po4j.parser;

import java.io.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lexer (tokenizer) for PO files.
 * Reads from a character stream and produces tokens.
 *
 * <p>This lexer handles:
 * <ul>
 *   <li>Keywords: msgctxt, msgid, msgid_plural, msgstr, msgstr[N]</li>
 *   <li>Quoted strings with escape sequences</li>
 *   <li>Various comment types (#, #., #:, #,, #|, #~)</li>
 *   <li>Line and column tracking for error messages</li>
 * </ul>
 */
public class POLexer implements Closeable {

    private static final Pattern MSGSTR_PLURAL_PATTERN = Pattern.compile("msgstr\\[(\\d+)]");

    private final PushbackReader reader;
    private final Deque<Token> tokenBuffer = new ArrayDeque<>();
    private int line = 1;
    private int column = 0;
    private String currentLine = "";
    private boolean inObsoleteBlock = false;

    /**
     * Creates a lexer for the given reader.
     */
    public POLexer(Reader reader) {
        this.reader = new PushbackReader(new BufferedReader(reader), 16);
    }

    /**
     * Creates a lexer for the given input stream with specified charset.
     */
    public POLexer(InputStream inputStream, String charset) throws UnsupportedEncodingException {
        this(new InputStreamReader(inputStream, charset));
    }

    /**
     * Returns the current line number (1-based).
     */
    public int getLine() {
        return line;
    }

    /**
     * Returns the current column number (1-based).
     */
    public int getColumn() {
        return column;
    }

    /**
     * Peeks at the next token without consuming it.
     */
    public Token peek() throws IOException {
        if (tokenBuffer.isEmpty()) {
            tokenBuffer.addFirst(readNextToken());
        }
        return tokenBuffer.peekFirst();
    }

    /**
     * Returns the next token and advances the lexer.
     */
    public Token nextToken() throws IOException {
        if (!tokenBuffer.isEmpty()) {
            return tokenBuffer.removeFirst();
        }
        return readNextToken();
    }

    /**
     * Pushes a token back to be returned by the next peek() or nextToken() call.
     * Multiple tokens can be pushed back; they will be returned in LIFO order.
     */
    public void unread(Token token) {
        tokenBuffer.addFirst(token);
    }

    private Token readNextToken() throws IOException {
        skipWhitespace();

        int startLine = line;
        int startColumn = column;
        int ch = readChar();

        if (ch == -1) {
            return Token.eof(line, column);
        }

        // Comment or obsolete marker
        if (ch == '#') {
            return scanComment(startLine, startColumn);
        }

        // Quoted string
        if (ch == '"') {
            return scanString(startLine, startColumn);
        }

        // Keyword (msgid, msgstr, msgctxt)
        if (Character.isLetter(ch)) {
            unreadChar(ch);
            return scanKeyword(startLine, startColumn);
        }

        throw new ParseException(
                "Unexpected character '" + (char) ch + "'",
                startLine, startColumn, currentLine
        );
    }

    private Token scanComment(int startLine, int startColumn) throws IOException {
        int ch = readChar();

        // Check for obsolete marker (#~)
        if (ch == '~') {
            // Read the character after #~
            int next = readChar();

            // Check for #~| (previous value in obsolete entry)
            if (next == '|') {
                // Skip optional whitespace after #~|
                int afterPipe = readChar();
                while (afterPipe == ' ' || afterPipe == '\t') {
                    afterPipe = readChar();
                }
                if (afterPipe != -1 && afterPipe != '\n' && afterPipe != '\r') {
                    unreadChar(afterPipe);
                }
                String content = readToEndOfLine();
                return new Token(TokenType.COMMENT_PREVIOUS, content.trim(), startLine, startColumn);
            }

            // Skip any whitespace after #~
            while (next != -1 && (next == ' ' || next == '\t')) {
                next = readChar();
            }

            if (next != -1 && next != '\n' && next != '\r') {
                unreadChar(next);
            }

            // Check if next char indicates a keyword (letter) or string (quote)
            // without consuming it
            if (next != -1 && (Character.isLetter(next) || next == '"')) {
                inObsoleteBlock = true;
                return new Token(TokenType.OBSOLETE_PREFIX, null, startLine, startColumn);
            }

            // Otherwise treat as obsolete translator comment (empty line after #~)
            String content = readToEndOfLine();
            return new Token(TokenType.COMMENT_TRANSLATOR, content, startLine, startColumn);
        }

        // Determine comment type based on second character
        TokenType type = switch (ch) {
            case '.' -> TokenType.COMMENT_EXTRACTED;
            case ':' -> TokenType.COMMENT_REFERENCE;
            case ',' -> TokenType.COMMENT_FLAG;
            case '|' -> TokenType.COMMENT_PREVIOUS;
            case ' ', '\t', '\n', '\r', -1 -> {
                // Translator comment - space after # or empty
                if (ch == '\n' || ch == '\r' || ch == -1) {
                    if (ch == '\r') {
                        int lf = readChar();
                        if (lf != '\n') {
                            unreadChar(lf);
                        }
                    }
                    yield TokenType.COMMENT_TRANSLATOR;
                }
                yield TokenType.COMMENT_TRANSLATOR;
            }
            default -> {
                // # followed by other char - could be translator comment without space
                unreadChar(ch);
                yield TokenType.COMMENT_TRANSLATOR;
            }
        };

        // Read the rest of the comment line
        String content;
        if (ch == '\n' || ch == '\r' || ch == -1) {
            content = "";
        } else {
            content = readToEndOfLine();
        }

        return new Token(type, content.trim(), startLine, startColumn);
    }

    private Token scanString(int startLine, int startColumn) throws IOException {
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;

        while (true) {
            int ch = readChar();

            if (ch == -1) {
                throw new ParseException(
                        "Unterminated string literal",
                        startLine, startColumn, currentLine
                );
            }

            if (escaped) {
                // Keep the escape sequence - unescaping happens later
                sb.append('\\').append((char) ch);
                escaped = false;
                continue;
            }

            if (ch == '\\') {
                escaped = true;
                continue;
            }

            if (ch == '"') {
                // End of this string segment
                break;
            }

            if (ch == '\n' || ch == '\r') {
                throw new ParseException(
                        "Newline in string literal (use \\n for embedded newlines)",
                        startLine, startColumn, currentLine
                );
            }

            sb.append((char) ch);
        }

        // Unescape the string content
        String raw = sb.toString();
        String unescaped = StringUnescaper.unescapeLenient(raw);

        return new Token(TokenType.STRING, unescaped, startLine, startColumn);
    }

    private Token scanKeyword(int startLine, int startColumn) throws IOException {
        StringBuilder sb = new StringBuilder();

        int ch;
        while ((ch = readChar()) != -1) {
            if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '[' || ch == ']') {
                sb.append((char) ch);
                if (ch == ']') {
                    break; // End of msgstr[N]
                }
            } else {
                unreadChar(ch);
                break;
            }
        }

        String keyword = sb.toString();

        // Match keywords
        return switch (keyword) {
            case "msgctxt" -> new Token(TokenType.MSGCTXT, null, startLine, startColumn);
            case "msgid" -> new Token(TokenType.MSGID, null, startLine, startColumn);
            case "msgid_plural" -> new Token(TokenType.MSGID_PLURAL, null, startLine, startColumn);
            case "msgstr" -> new Token(TokenType.MSGSTR, null, startLine, startColumn);
            default -> {
                // Check for msgstr[N]
                Matcher m = MSGSTR_PLURAL_PATTERN.matcher(keyword);
                if (m.matches()) {
                    yield new Token(TokenType.MSGSTR_PLURAL, m.group(1), startLine, startColumn);
                }
                throw new ParseException(
                        "Unknown keyword '" + keyword + "'",
                        startLine, startColumn, currentLine
                );
            }
        };
    }

    private void skipWhitespace() throws IOException {
        int ch;
        while ((ch = readChar()) != -1) {
            if (ch == ' ' || ch == '\t') {
                // Skip horizontal whitespace
                continue;
            }
            if (ch == '\n') {
                // Newline resets obsolete block flag at blank lines
                int next = readChar();
                if (next == '\n' || next == -1) {
                    inObsoleteBlock = false;
                }
                if (next != -1) {
                    unreadChar(next);
                }
                continue;
            }
            if (ch == '\r') {
                // Handle \r\n
                int next = readChar();
                if (next != '\n' && next != -1) {
                    unreadChar(next);
                }
                continue;
            }
            // Non-whitespace
            unreadChar(ch);
            break;
        }
    }

    private String readToEndOfLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        int ch;
        while ((ch = readChar()) != -1) {
            if (ch == '\n') {
                break;
            }
            if (ch == '\r') {
                int next = readChar();
                if (next != '\n' && next != -1) {
                    unreadChar(next);
                }
                break;
            }
            sb.append((char) ch);
        }
        return sb.toString();
    }

    private int readChar() throws IOException {
        int ch = reader.read();
        if (ch == '\n') {
            line++;
            column = 0;
            currentLine = "";
        } else if (ch != -1) {
            column++;
            if (ch != '\r') {
                currentLine += (char) ch;
            }
        }
        return ch;
    }

    private void unreadChar(int ch) throws IOException {
        if (ch == -1) {
            return;
        }
        reader.unread(ch);
        if (ch == '\n') {
            line--;
        } else {
            column--;
            if (!currentLine.isEmpty()) {
                currentLine = currentLine.substring(0, currentLine.length() - 1);
            }
        }
    }

    /**
     * Returns true if we're currently inside an obsolete block.
     */
    public boolean isInObsoleteBlock() {
        return inObsoleteBlock;
    }

    /**
     * Resets the obsolete block flag.
     */
    public void resetObsoleteBlock() {
        inObsoleteBlock = false;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
