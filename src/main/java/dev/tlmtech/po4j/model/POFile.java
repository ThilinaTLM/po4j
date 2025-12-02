package dev.tlmtech.po4j.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a complete PO file with header, entries, and obsolete entries.
 * This class is immutable and thread-safe.
 *
 * <p>A PO file consists of:
 * <ul>
 *   <li>An optional header entry (first entry with empty msgid)</li>
 *   <li>Zero or more translation entries</li>
 *   <li>Zero or more obsolete entries (marked with #~)</li>
 * </ul>
 */
public final class POFile {

    private final POHeader header;
    private final List<POEntry> entries;
    private final List<POEntry> obsoleteEntries;

    private POFile(Builder builder) {
        this.header = builder.header;
        this.entries = List.copyOf(builder.entries);
        this.obsoleteEntries = List.copyOf(builder.obsoleteEntries);
    }

    /**
     * Creates a new builder for POFile.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder pre-populated with this file's values.
     */
    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.header = this.header;
        builder.entries.addAll(this.entries);
        builder.obsoleteEntries.addAll(this.obsoleteEntries);
        return builder;
    }

    // --- Accessors ---

    /**
     * Returns the header, if present.
     */
    public Optional<POHeader> getHeader() {
        return Optional.ofNullable(header);
    }

    /**
     * Returns all non-obsolete entries (excluding header).
     */
    public List<POEntry> getEntries() {
        return entries;
    }

    /**
     * Returns all obsolete entries.
     */
    public List<POEntry> getObsoleteEntries() {
        return obsoleteEntries;
    }

    /**
     * Returns the total number of entries (excluding header and obsolete).
     */
    public int size() {
        return entries.size();
    }

    /**
     * Returns true if there are no entries (excluding header and obsolete).
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    // --- Query methods ---

    /**
     * Finds an entry by msgid.
     * If multiple entries have the same msgid (different contexts), returns the first one.
     */
    public Optional<POEntry> findByMsgid(String msgid) {
        return entries.stream()
                .filter(e -> e.getMsgid().equals(msgid))
                .findFirst();
    }

    /**
     * Finds an entry by msgid and context.
     *
     * @param msgid   the message ID
     * @param context the context, or null for entries without context
     */
    public Optional<POEntry> findByMsgidAndContext(String msgid, String context) {
        return entries.stream()
                .filter(e -> e.getMsgid().equals(msgid))
                .filter(e -> Objects.equals(e.getMsgctxt().orElse(null), context))
                .findFirst();
    }

    /**
     * Finds all entries with the specified msgid (may have different contexts).
     */
    public List<POEntry> findAllByMsgid(String msgid) {
        return entries.stream()
                .filter(e -> e.getMsgid().equals(msgid))
                .toList();
    }

    /**
     * Returns all entries that are marked as fuzzy.
     */
    public List<POEntry> getFuzzyEntries() {
        return entries.stream()
                .filter(POEntry::isFuzzy)
                .toList();
    }

    /**
     * Returns all entries that are not translated.
     */
    public List<POEntry> getUntranslatedEntries() {
        return entries.stream()
                .filter(e -> !e.isTranslated())
                .toList();
    }

    /**
     * Returns all entries that have the specified flag.
     */
    public List<POEntry> getEntriesWithFlag(String flag) {
        return entries.stream()
                .filter(e -> e.hasFlag(flag))
                .toList();
    }

    // --- Statistics ---

    /**
     * Returns the number of translated entries.
     */
    public int getTranslatedCount() {
        return (int) entries.stream().filter(POEntry::isTranslated).count();
    }

    /**
     * Returns the number of fuzzy entries.
     */
    public int getFuzzyCount() {
        return (int) entries.stream().filter(POEntry::isFuzzy).count();
    }

    /**
     * Returns the number of untranslated entries.
     */
    public int getUntranslatedCount() {
        return (int) entries.stream().filter(e -> !e.isTranslated()).count();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof POFile poFile)) return false;
        return Objects.equals(header, poFile.header)
                && Objects.equals(entries, poFile.entries)
                && Objects.equals(obsoleteEntries, poFile.obsoleteEntries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(header, entries, obsoleteEntries);
    }

    @Override
    public String toString() {
        return "POFile{" +
                "header=" + (header != null) +
                ", entries=" + entries.size() +
                ", obsolete=" + obsoleteEntries.size() +
                "}";
    }

    // --- Builder ---

    public static final class Builder {
        private final List<POEntry> entries = new ArrayList<>();
        private final List<POEntry> obsoleteEntries = new ArrayList<>();
        private POHeader header;

        private Builder() {
        }

        public Builder header(POHeader header) {
            this.header = header;
            return this;
        }

        public Builder entry(POEntry entry) {
            if (entry.isObsolete()) {
                this.obsoleteEntries.add(entry);
            } else if (entry.isHeader()) {
                this.header = POHeader.fromEntry(entry);
            } else {
                this.entries.add(entry);
            }
            return this;
        }

        public Builder entries(List<POEntry> entries) {
            for (POEntry entry : entries) {
                entry(entry);
            }
            return this;
        }

        public Builder addObsoleteEntry(POEntry entry) {
            this.obsoleteEntries.add(entry);
            return this;
        }

        public Builder obsoleteEntries(List<POEntry> entries) {
            this.obsoleteEntries.clear();
            this.obsoleteEntries.addAll(entries);
            return this;
        }

        /**
         * Clears all entries (but keeps header).
         */
        public Builder clearEntries() {
            this.entries.clear();
            return this;
        }

        /**
         * Clears all obsolete entries.
         */
        public Builder clearObsoleteEntries() {
            this.obsoleteEntries.clear();
            return this;
        }

        public POFile build() {
            return new POFile(this);
        }
    }
}
