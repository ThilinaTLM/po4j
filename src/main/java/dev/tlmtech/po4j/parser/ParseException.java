package dev.tlmtech.po4j.parser;

/**
 * Exception thrown when parsing a PO file fails.
 * Contains location information to help identify the error.
 */
public class ParseException extends RuntimeException {

    private final int line;
    private final int column;
    private final String context;

    /**
     * Creates a parse exception with location information.
     *
     * @param message the error message
     * @param line    the line number (1-based)
     * @param column  the column number (1-based)
     */
    public ParseException(String message, int line, int column) {
        this(message, line, column, null, null);
    }

    /**
     * Creates a parse exception with location and context information.
     *
     * @param message the error message
     * @param line    the line number (1-based)
     * @param column  the column number (1-based)
     * @param context the line content where the error occurred
     */
    public ParseException(String message, int line, int column, String context) {
        this(message, line, column, context, null);
    }

    /**
     * Creates a parse exception with a cause.
     *
     * @param message the error message
     * @param line    the line number (1-based)
     * @param column  the column number (1-based)
     * @param context the line content where the error occurred
     * @param cause   the underlying cause
     */
    public ParseException(String message, int line, int column, String context, Throwable cause) {
        super(message, cause);
        this.line = line;
        this.column = column;
        this.context = context;
    }

    /**
     * Returns the line number where the error occurred (1-based).
     */
    public int getLine() {
        return line;
    }

    /**
     * Returns the column number where the error occurred (1-based).
     */
    public int getColumn() {
        return column;
    }

    /**
     * Returns the context (line content) where the error occurred.
     */
    public String getContext() {
        return context;
    }

    /**
     * Returns a formatted error message with location and context.
     */
    public String getFormattedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Parse error at line ").append(line);
        if (column > 0) {
            sb.append(", column ").append(column);
        }
        sb.append(": ").append(super.getMessage());

        if (context != null && !context.isEmpty()) {
            sb.append("\n").append(context);
            if (column > 0 && column <= context.length()) {
                sb.append("\n").append(" ".repeat(column - 1)).append("^");
            }
        }

        return sb.toString();
    }

    @Override
    public String getMessage() {
        return getFormattedMessage();
    }
}
