package dev.tlmtech.po4j.model;

import java.util.*;

/**
 * Represents a single translation entry in a PO file.
 * This class is immutable and thread-safe.
 *
 * <p>An entry consists of:
 * <ul>
 *   <li>Optional context (msgctxt)</li>
 *   <li>Source string (msgid)</li>
 *   <li>Optional plural source string (msgid_plural)</li>
 *   <li>Translation (msgstr) or plural translations (msgstr[n])</li>
 *   <li>Various comment types</li>
 *   <li>Flags (e.g., fuzzy, c-format)</li>
 *   <li>Previous values for fuzzy matching</li>
 * </ul>
 */
public final class POEntry {

    private final String msgctxt;
    private final String msgid;
    private final String msgidPlural;
    private final String msgstr;
    private final List<String> msgstrPlural;
    private final List<String> translatorComments;
    private final List<String> extractedComments;
    private final List<String> references;
    private final Set<String> flags;
    private final String previousMsgctxt;
    private final String previousMsgid;
    private final String previousMsgidPlural;
    private final boolean obsolete;

    private POEntry(Builder builder) {
        this.msgctxt = builder.msgctxt;
        this.msgid = Objects.requireNonNull(builder.msgid, "msgid is required");
        this.msgidPlural = builder.msgidPlural;
        this.msgstr = builder.msgstr;
        this.msgstrPlural = builder.msgstrPlural != null
                ? List.copyOf(builder.msgstrPlural)
                : List.of();
        this.translatorComments = List.copyOf(builder.translatorComments);
        this.extractedComments = List.copyOf(builder.extractedComments);
        this.references = List.copyOf(builder.references);
        this.flags = Set.copyOf(builder.flags);
        this.previousMsgctxt = builder.previousMsgctxt;
        this.previousMsgid = builder.previousMsgid;
        this.previousMsgidPlural = builder.previousMsgidPlural;
        this.obsolete = builder.obsolete;
    }

    /**
     * Creates a new builder for constructing POEntry instances.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder pre-populated with this entry's values.
     */
    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.msgctxt = this.msgctxt;
        builder.msgid = this.msgid;
        builder.msgidPlural = this.msgidPlural;
        builder.msgstr = this.msgstr;
        if (!this.msgstrPlural.isEmpty()) {
            builder.msgstrPlural = new ArrayList<>(this.msgstrPlural);
        }
        builder.translatorComments.addAll(this.translatorComments);
        builder.extractedComments.addAll(this.extractedComments);
        builder.references.addAll(this.references);
        builder.flags.addAll(this.flags);
        builder.previousMsgctxt = this.previousMsgctxt;
        builder.previousMsgid = this.previousMsgid;
        builder.previousMsgidPlural = this.previousMsgidPlural;
        builder.obsolete = this.obsolete;
        return builder;
    }

    // --- Accessors ---

    public Optional<String> getMsgctxt() {
        return Optional.ofNullable(msgctxt);
    }

    public String getMsgid() {
        return msgid;
    }

    public Optional<String> getMsgidPlural() {
        return Optional.ofNullable(msgidPlural);
    }

    /**
     * Returns the singular translation (msgstr).
     * For plural entries, this returns null - use getMsgstrPlural() instead.
     */
    public Optional<String> getMsgstr() {
        return Optional.ofNullable(msgstr);
    }

    /**
     * Returns the plural translations (msgstr[0], msgstr[1], etc.).
     * For singular entries, this returns an empty list.
     */
    public List<String> getMsgstrPlural() {
        return msgstrPlural;
    }

    /**
     * Returns the translation for a specific plural index.
     * For singular entries, index 0 returns msgstr.
     */
    public Optional<String> getMsgstr(int index) {
        if (!isPlural()) {
            return index == 0 ? getMsgstr() : Optional.empty();
        }
        if (index >= 0 && index < msgstrPlural.size()) {
            return Optional.ofNullable(msgstrPlural.get(index));
        }
        return Optional.empty();
    }

    public List<String> getTranslatorComments() {
        return translatorComments;
    }

    public List<String> getExtractedComments() {
        return extractedComments;
    }

    public List<String> getReferences() {
        return references;
    }

    public Set<String> getFlags() {
        return flags;
    }

    public Optional<String> getPreviousMsgctxt() {
        return Optional.ofNullable(previousMsgctxt);
    }

    public Optional<String> getPreviousMsgid() {
        return Optional.ofNullable(previousMsgid);
    }

    public Optional<String> getPreviousMsgidPlural() {
        return Optional.ofNullable(previousMsgidPlural);
    }

    public boolean isObsolete() {
        return obsolete;
    }

    // --- Helper methods ---

    /**
     * Returns true if this is a plural entry (has msgid_plural).
     */
    public boolean isPlural() {
        return msgidPlural != null;
    }

    /**
     * Returns true if this entry is marked as fuzzy.
     */
    public boolean isFuzzy() {
        return flags.contains("fuzzy");
    }

    /**
     * Returns true if this entry is a header entry (empty msgid).
     */
    public boolean isHeader() {
        return msgid.isEmpty() && msgctxt == null;
    }

    /**
     * Returns true if this entry has the specified flag.
     */
    public boolean hasFlag(String flag) {
        return flags.contains(flag);
    }

    /**
     * Returns true if this entry is translated (has non-empty msgstr).
     */
    public boolean isTranslated() {
        if (isPlural()) {
            return !msgstrPlural.isEmpty() && msgstrPlural.stream().anyMatch(s -> s != null && !s.isEmpty());
        }
        return msgstr != null && !msgstr.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof POEntry entry)) return false;
        return obsolete == entry.obsolete
                && Objects.equals(msgctxt, entry.msgctxt)
                && Objects.equals(msgid, entry.msgid)
                && Objects.equals(msgidPlural, entry.msgidPlural)
                && Objects.equals(msgstr, entry.msgstr)
                && Objects.equals(msgstrPlural, entry.msgstrPlural)
                && Objects.equals(translatorComments, entry.translatorComments)
                && Objects.equals(extractedComments, entry.extractedComments)
                && Objects.equals(references, entry.references)
                && Objects.equals(flags, entry.flags)
                && Objects.equals(previousMsgctxt, entry.previousMsgctxt)
                && Objects.equals(previousMsgid, entry.previousMsgid)
                && Objects.equals(previousMsgidPlural, entry.previousMsgidPlural);
    }

    @Override
    public int hashCode() {
        return Objects.hash(msgctxt, msgid, msgidPlural, msgstr, msgstrPlural,
                translatorComments, extractedComments, references, flags,
                previousMsgctxt, previousMsgid, previousMsgidPlural, obsolete);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("POEntry{");
        if (msgctxt != null) {
            sb.append("msgctxt='").append(truncate(msgctxt)).append("', ");
        }
        sb.append("msgid='").append(truncate(msgid)).append("'");
        if (isPlural()) {
            sb.append(", msgid_plural='").append(truncate(msgidPlural)).append("'");
            sb.append(", msgstr[").append(msgstrPlural.size()).append("]");
        } else if (msgstr != null) {
            sb.append(", msgstr='").append(truncate(msgstr)).append("'");
        }
        if (obsolete) {
            sb.append(", obsolete");
        }
        if (isFuzzy()) {
            sb.append(", fuzzy");
        }
        sb.append("}");
        return sb.toString();
    }

    private String truncate(String s) {
        if (s == null) return null;
        if (s.length() <= 30) return s;
        return s.substring(0, 27) + "...";
    }

    // --- Builder ---

    public static final class Builder {
        private final List<String> translatorComments = new ArrayList<>();
        private final List<String> extractedComments = new ArrayList<>();
        private final List<String> references = new ArrayList<>();
        private final Set<String> flags = new LinkedHashSet<>();
        private String msgctxt;
        private String msgid;
        private String msgidPlural;
        private String msgstr;
        private List<String> msgstrPlural;
        private String previousMsgctxt;
        private String previousMsgid;
        private String previousMsgidPlural;
        private boolean obsolete;

        private Builder() {
        }

        public Builder msgctxt(String msgctxt) {
            this.msgctxt = msgctxt;
            return this;
        }

        public Builder msgid(String msgid) {
            this.msgid = msgid;
            return this;
        }

        public Builder msgidPlural(String msgidPlural) {
            this.msgidPlural = msgidPlural;
            return this;
        }

        public Builder msgstr(String msgstr) {
            this.msgstr = msgstr;
            return this;
        }

        public Builder msgstrPlural(List<String> msgstrPlural) {
            this.msgstrPlural = msgstrPlural != null ? new ArrayList<>(msgstrPlural) : null;
            return this;
        }

        public Builder addMsgstrPlural(int index, String value) {
            if (this.msgstrPlural == null) {
                this.msgstrPlural = new ArrayList<>();
            }
            // Expand list if needed
            while (this.msgstrPlural.size() <= index) {
                this.msgstrPlural.add("");
            }
            this.msgstrPlural.set(index, value);
            return this;
        }

        public Builder addTranslatorComment(String comment) {
            this.translatorComments.add(comment);
            return this;
        }

        public Builder translatorComments(List<String> comments) {
            this.translatorComments.clear();
            this.translatorComments.addAll(comments);
            return this;
        }

        public Builder addExtractedComment(String comment) {
            this.extractedComments.add(comment);
            return this;
        }

        public Builder extractedComments(List<String> comments) {
            this.extractedComments.clear();
            this.extractedComments.addAll(comments);
            return this;
        }

        public Builder addReference(String reference) {
            this.references.add(reference);
            return this;
        }

        public Builder references(List<String> references) {
            this.references.clear();
            this.references.addAll(references);
            return this;
        }

        public Builder addFlag(String flag) {
            this.flags.add(flag);
            return this;
        }

        public Builder flags(Collection<String> flags) {
            this.flags.clear();
            this.flags.addAll(flags);
            return this;
        }

        public Builder previousMsgctxt(String previousMsgctxt) {
            this.previousMsgctxt = previousMsgctxt;
            return this;
        }

        public Builder previousMsgid(String previousMsgid) {
            this.previousMsgid = previousMsgid;
            return this;
        }

        public Builder previousMsgidPlural(String previousMsgidPlural) {
            this.previousMsgidPlural = previousMsgidPlural;
            return this;
        }

        public Builder obsolete(boolean obsolete) {
            this.obsolete = obsolete;
            return this;
        }

        public Builder fuzzy(boolean fuzzy) {
            if (fuzzy) {
                this.flags.add("fuzzy");
            } else {
                this.flags.remove("fuzzy");
            }
            return this;
        }

        /**
         * Builds the POEntry.
         *
         * @throws NullPointerException  if msgid is null
         * @throws IllegalStateException if both msgstr and msgstrPlural are set,
         *                               or if msgstrPlural is set without msgidPlural
         */
        public POEntry build() {
            if (msgid == null) {
                throw new NullPointerException("msgid is required");
            }

            // Validate consistency
            if (msgstrPlural != null && !msgstrPlural.isEmpty() && msgstr != null && !msgstr.isEmpty()) {
                throw new IllegalStateException("Cannot have both msgstr and msgstrPlural");
            }

            if (msgstrPlural != null && !msgstrPlural.isEmpty() && msgidPlural == null) {
                throw new IllegalStateException("msgstrPlural requires msgidPlural");
            }

            return new POEntry(this);
        }
    }
}
