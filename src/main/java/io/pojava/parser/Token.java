package io.pojava.parser;

import java.util.Objects;

/**
 * Represents a token from the PO file lexer.
 *
 * @param type the type of token
 * @param value the token value (string content for STRING, comment text for comments, etc.)
 * @param line the line number (1-based) where the token starts
 * @param column the column number (1-based) where the token starts
 */
public record Token(TokenType type, String value, int line, int column) {

    public Token {
        Objects.requireNonNull(type, "type must not be null");
    }

    /**
     * Creates a token with default position (used for testing).
     */
    public static Token of(TokenType type, String value) {
        return new Token(type, value, 0, 0);
    }

    /**
     * Creates an EOF token.
     */
    public static Token eof(int line, int column) {
        return new Token(TokenType.EOF, null, line, column);
    }

    /**
     * Returns true if this is an EOF token.
     */
    public boolean isEof() {
        return type == TokenType.EOF;
    }

    /**
     * For MSGSTR_PLURAL tokens, returns the plural index.
     * Returns -1 for non-plural msgstr tokens.
     */
    public int getPluralIndex() {
        if (type != TokenType.MSGSTR_PLURAL || value == null) {
            return -1;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public String toString() {
        if (value == null || value.isEmpty()) {
            return String.format("%s at %d:%d", type, line, column);
        }
        String displayValue = value.length() > 30
                ? value.substring(0, 27) + "..."
                : value;
        return String.format("%s('%s') at %d:%d", type, displayValue, line, column);
    }
}
