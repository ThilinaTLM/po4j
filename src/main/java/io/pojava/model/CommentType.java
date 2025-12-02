package io.pojava.model;

/**
 * Types of comments that can appear in PO file entries.
 */
public enum CommentType {
    /**
     * Translator comments - lines starting with "# " (hash followed by space or end-of-line).
     * Created and maintained by translators.
     */
    TRANSLATOR('#', ' '),

    /**
     * Extracted comments - lines starting with "#.".
     * Auto-extracted from source code by xgettext.
     */
    EXTRACTED('#', '.'),

    /**
     * Reference comments - lines starting with "#:".
     * Source file locations in "filename:line" format.
     */
    REFERENCE('#', ':'),

    /**
     * Flag comments - lines starting with "#,".
     * Comma-separated list of keywords like "fuzzy", "c-format".
     */
    FLAG('#', ','),

    /**
     * Previous value comments - lines starting with "#|".
     * Previous msgid/msgctxt values for fuzzy matching.
     */
    PREVIOUS('#', '|');

    private final char firstChar;
    private final char secondChar;

    CommentType(char firstChar, char secondChar) {
        this.firstChar = firstChar;
        this.secondChar = secondChar;
    }

    /**
     * Returns the prefix string for this comment type.
     * For TRANSLATOR, returns "#" (space is part of content).
     * For others, returns "#X" where X is the type indicator.
     */
    public String getPrefix() {
        if (this == TRANSLATOR) {
            return "#";
        }
        return String.valueOf(firstChar) + secondChar;
    }

    /**
     * Determines the comment type from a line starting with '#'.
     *
     * @param line the comment line (must start with '#')
     * @return the comment type, or TRANSLATOR if no specific type matches
     */
    public static CommentType fromLine(String line) {
        if (line == null || line.isEmpty() || line.charAt(0) != '#') {
            throw new IllegalArgumentException("Line must start with '#'");
        }

        if (line.length() == 1) {
            return TRANSLATOR;
        }

        char second = line.charAt(1);
        return switch (second) {
            case '.' -> EXTRACTED;
            case ':' -> REFERENCE;
            case ',' -> FLAG;
            case '|' -> PREVIOUS;
            default -> TRANSLATOR;
        };
    }
}
