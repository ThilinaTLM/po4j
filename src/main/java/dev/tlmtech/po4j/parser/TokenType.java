package dev.tlmtech.po4j.parser;

/**
 * Types of tokens that can appear in a PO file.
 */
public enum TokenType {
    /**
     * msgctxt keyword - message context
     */
    MSGCTXT,

    /**
     * msgid keyword - message identifier (source string)
     */
    MSGID,

    /**
     * msgid_plural keyword - plural form of message identifier
     */
    MSGID_PLURAL,

    /**
     * msgstr keyword - translated string (singular)
     */
    MSGSTR,

    /**
     * msgstr[N] keyword - translated string for plural form N
     */
    MSGSTR_PLURAL,

    /**
     * Quoted string literal
     */
    STRING,

    /**
     * Translator comment (# followed by space or end-of-line)
     */
    COMMENT_TRANSLATOR,

    /**
     * Extracted comment (#.)
     */
    COMMENT_EXTRACTED,

    /**
     * Reference comment (#:)
     */
    COMMENT_REFERENCE,

    /**
     * Flag comment (#,)
     */
    COMMENT_FLAG,

    /**
     * Previous value comment (#|)
     */
    COMMENT_PREVIOUS,

    /**
     * Obsolete marker (#~) - can prefix other elements
     */
    OBSOLETE_PREFIX,

    /**
     * End of file
     */
    EOF;

    /**
     * Returns true if this token type is a comment.
     */
    public boolean isComment() {
        return switch (this) {
            case COMMENT_TRANSLATOR, COMMENT_EXTRACTED, COMMENT_REFERENCE,
                 COMMENT_FLAG, COMMENT_PREVIOUS -> true;
            default -> false;
        };
    }

    /**
     * Returns true if this token type is a keyword (msgid, msgstr, etc.).
     */
    public boolean isKeyword() {
        return switch (this) {
            case MSGCTXT, MSGID, MSGID_PLURAL, MSGSTR, MSGSTR_PLURAL -> true;
            default -> false;
        };
    }
}
