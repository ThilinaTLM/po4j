package dev.tlmtech.po4j.parser;

/**
 * Configuration options for the PO file parser.
 */
public final class POParserOptions {

    private final boolean strict;
    private final boolean preserveObsolete;

    private POParserOptions(Builder builder) {
        this.strict = builder.strict;
        this.preserveObsolete = builder.preserveObsolete;
    }

    /**
     * Returns the default parser options.
     */
    public static POParserOptions defaults() {
        return builder().build();
    }

    /**
     * Creates a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns true if strict parsing mode is enabled.
     * In strict mode, parse errors throw exceptions.
     * In lenient mode, malformed entries are skipped.
     */
    public boolean isStrict() {
        return strict;
    }

    /**
     * Returns true if obsolete entries should be preserved.
     */
    public boolean isPreserveObsolete() {
        return preserveObsolete;
    }

    public static final class Builder {
        private boolean strict = true;
        private boolean preserveObsolete = true;

        private Builder() {
        }

        /**
         * Sets strict parsing mode.
         *
         * @param strict true to throw on parse errors, false to skip malformed entries
         */
        public Builder strict(boolean strict) {
            this.strict = strict;
            return this;
        }

        /**
         * Sets whether to preserve obsolete entries.
         *
         * @param preserveObsolete true to include obsolete entries in parse result
         */
        public Builder preserveObsolete(boolean preserveObsolete) {
            this.preserveObsolete = preserveObsolete;
            return this;
        }

        public POParserOptions build() {
            return new POParserOptions(this);
        }
    }
}
