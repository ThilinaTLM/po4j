package io.pojava.writer;

/**
 * Configuration options for the PO file writer.
 */
public final class POWriterOptions {

    private final int maxLineWidth;
    private final boolean wrapStrings;
    private final String lineSeparator;
    private final boolean writeObsolete;
    private final boolean sortEntries;

    private POWriterOptions(Builder builder) {
        this.maxLineWidth = builder.maxLineWidth;
        this.wrapStrings = builder.wrapStrings;
        this.lineSeparator = builder.lineSeparator;
        this.writeObsolete = builder.writeObsolete;
        this.sortEntries = builder.sortEntries;
    }

    /**
     * Returns the default writer options.
     */
    public static POWriterOptions defaults() {
        return builder().build();
    }

    /**
     * Creates a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the maximum line width for string wrapping.
     * Default is 80 characters.
     */
    public int getMaxLineWidth() {
        return maxLineWidth;
    }

    /**
     * Returns true if long strings should be wrapped.
     * Default is true.
     */
    public boolean isWrapStrings() {
        return wrapStrings;
    }

    /**
     * Returns the line separator to use.
     * Default is "\n" (Unix-style).
     */
    public String getLineSeparator() {
        return lineSeparator;
    }

    /**
     * Returns true if obsolete entries should be written.
     * Default is true.
     */
    public boolean isWriteObsolete() {
        return writeObsolete;
    }

    /**
     * Returns true if entries should be sorted by msgid.
     * Default is false (preserve order).
     */
    public boolean isSortEntries() {
        return sortEntries;
    }

    public static final class Builder {
        private int maxLineWidth = 80;
        private boolean wrapStrings = true;
        private String lineSeparator = "\n";
        private boolean writeObsolete = true;
        private boolean sortEntries = false;

        private Builder() {}

        /**
         * Sets the maximum line width for string wrapping.
         */
        public Builder maxLineWidth(int maxLineWidth) {
            if (maxLineWidth < 20) {
                throw new IllegalArgumentException("maxLineWidth must be at least 20");
            }
            this.maxLineWidth = maxLineWidth;
            return this;
        }

        /**
         * Sets whether to wrap long strings.
         */
        public Builder wrapStrings(boolean wrapStrings) {
            this.wrapStrings = wrapStrings;
            return this;
        }

        /**
         * Sets the line separator.
         */
        public Builder lineSeparator(String lineSeparator) {
            this.lineSeparator = lineSeparator != null ? lineSeparator : "\n";
            return this;
        }

        /**
         * Sets whether to write obsolete entries.
         */
        public Builder writeObsolete(boolean writeObsolete) {
            this.writeObsolete = writeObsolete;
            return this;
        }

        /**
         * Sets whether to sort entries by msgid.
         */
        public Builder sortEntries(boolean sortEntries) {
            this.sortEntries = sortEntries;
            return this;
        }

        public POWriterOptions build() {
            return new POWriterOptions(this);
        }
    }
}
