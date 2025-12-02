package dev.tlmtech.po4j.model;

import java.util.*;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * Represents the header of a PO file.
 * The header is the first entry with an empty msgid and contains metadata as key-value pairs.
 *
 * <p>Standard header fields include:
 * <ul>
 *   <li>Project-Id-Version</li>
 *   <li>Report-Msgid-Bugs-To</li>
 *   <li>POT-Creation-Date</li>
 *   <li>PO-Revision-Date</li>
 *   <li>Last-Translator</li>
 *   <li>Language-Team</li>
 *   <li>Language</li>
 *   <li>MIME-Version</li>
 *   <li>Content-Type</li>
 *   <li>Content-Transfer-Encoding</li>
 *   <li>Plural-Forms</li>
 * </ul>
 */
public final class POHeader {

    public static final String PROJECT_ID_VERSION = "Project-Id-Version";
    public static final String REPORT_MSGID_BUGS_TO = "Report-Msgid-Bugs-To";
    public static final String POT_CREATION_DATE = "POT-Creation-Date";
    public static final String PO_REVISION_DATE = "PO-Revision-Date";
    public static final String LAST_TRANSLATOR = "Last-Translator";
    public static final String LANGUAGE_TEAM = "Language-Team";
    public static final String LANGUAGE = "Language";
    public static final String MIME_VERSION = "MIME-Version";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
    public static final String PLURAL_FORMS = "Plural-Forms";
    public static final String X_GENERATOR = "X-Generator";

    private final Map<String, String> fields;
    private final List<String> translatorComments;
    private final Set<String> flags;

    private POHeader(Builder builder) {
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(builder.fields));
        this.translatorComments = List.copyOf(builder.translatorComments);
        this.flags = Set.copyOf(builder.flags);
    }

    /**
     * Creates a POHeader from a header POEntry.
     *
     * @param entry the header entry (must have empty msgid)
     * @return the parsed POHeader
     * @throws IllegalArgumentException if the entry is not a header entry
     */
    public static POHeader fromEntry(POEntry entry) {
        if (!entry.isHeader()) {
            throw new IllegalArgumentException("Entry is not a header (msgid must be empty, msgctxt must be null)");
        }

        Builder builder = builder();
        builder.translatorComments.addAll(entry.getTranslatorComments());
        builder.flags.addAll(entry.getFlags());

        String msgstr = entry.getMsgstr().orElse("");
        parseHeaderFields(msgstr, builder.fields);

        return builder.build();
    }

    private static void parseHeaderFields(String msgstr, Map<String, String> fields) {
        if (msgstr == null || msgstr.isEmpty()) {
            return;
        }

        for (String line : msgstr.split("\n", -1)) {
            if (line.isEmpty()) {
                continue;
            }
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                fields.put(key, value);
            }
        }
    }

    /**
     * Creates a new builder for POHeader.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder pre-populated with this header's values.
     */
    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.fields.putAll(this.fields);
        builder.translatorComments.addAll(this.translatorComments);
        builder.flags.addAll(this.flags);
        return builder;
    }

    // --- Accessors ---

    /**
     * Returns all header fields as an unmodifiable map.
     */
    public Map<String, String> getAllFields() {
        return fields;
    }

    /**
     * Returns the value of a specific header field.
     */
    public Optional<String> getField(String name) {
        return Optional.ofNullable(fields.get(name));
    }

    public Optional<String> getProjectIdVersion() {
        return getField(PROJECT_ID_VERSION);
    }

    public Optional<String> getReportMsgidBugsTo() {
        return getField(REPORT_MSGID_BUGS_TO);
    }

    public Optional<String> getPotCreationDate() {
        return getField(POT_CREATION_DATE);
    }

    public Optional<String> getPoRevisionDate() {
        return getField(PO_REVISION_DATE);
    }

    public Optional<String> getLastTranslator() {
        return getField(LAST_TRANSLATOR);
    }

    public Optional<String> getLanguageTeam() {
        return getField(LANGUAGE_TEAM);
    }

    public Optional<String> getLanguage() {
        return getField(LANGUAGE);
    }

    public Optional<String> getMimeVersion() {
        return getField(MIME_VERSION);
    }

    public Optional<String> getContentType() {
        return getField(CONTENT_TYPE);
    }

    /**
     * Returns the charset from Content-Type header.
     * Parses "text/plain; charset=UTF-8" to return "UTF-8".
     */
    public Optional<String> getCharset() {
        return getContentType().flatMap(ct -> {
            String lower = ct.toLowerCase(Locale.ROOT);
            int idx = lower.indexOf("charset=");
            if (idx < 0) return Optional.empty();
            String charset = ct.substring(idx + 8).trim();
            // Remove trailing semicolon or other parameters
            int endIdx = charset.indexOf(';');
            if (endIdx > 0) {
                charset = charset.substring(0, endIdx);
            }
            return Optional.of(charset.trim());
        });
    }

    public Optional<String> getContentTransferEncoding() {
        return getField(CONTENT_TRANSFER_ENCODING);
    }

    /**
     * Returns the parsed Plural-Forms value.
     */
    public Optional<PluralForms> getPluralForms() {
        return getField(PLURAL_FORMS).flatMap(PluralForms::parse);
    }

    /**
     * Returns the raw Plural-Forms string.
     */
    public Optional<String> getPluralFormsRaw() {
        return getField(PLURAL_FORMS);
    }

    public Optional<String> getGenerator() {
        return getField(X_GENERATOR);
    }

    public List<String> getTranslatorComments() {
        return translatorComments;
    }

    public Set<String> getFlags() {
        return flags;
    }

    /**
     * Converts this header back to a POEntry.
     */
    public POEntry toEntry() {
        POEntry.Builder builder = POEntry.builder().msgid("").msgstr(toMsgstr());

        for (String comment : translatorComments) {
            builder.addTranslatorComment(comment);
        }
        builder.flags(flags);

        return builder.build();
    }

    /**
     * Formats the header fields as a msgstr value.
     */
    public String toMsgstr() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof POHeader poHeader)) return false;
        return Objects.equals(fields, poHeader.fields)
                && Objects.equals(translatorComments, poHeader.translatorComments)
                && Objects.equals(flags, poHeader.flags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fields, translatorComments, flags);
    }

    @Override
    public String toString() {
        return "POHeader{fields=" + fields.keySet() + "}";
    }

    // --- Builder ---

    public static final class Builder {
        private final Map<String, String> fields = new LinkedHashMap<>();
        private final List<String> translatorComments = new ArrayList<>();
        private final Set<String> flags = new LinkedHashSet<>();

        private Builder() {}

        public Builder field(String name, @Nullable String value) {
            if (value == null) {
                fields.remove(name);
            } else {
                fields.put(name, value);
            }
            return this;
        }

        public Builder projectIdVersion(String value) {
            return field(PROJECT_ID_VERSION, value);
        }

        public Builder reportMsgidBugsTo(String value) {
            return field(REPORT_MSGID_BUGS_TO, value);
        }

        public Builder potCreationDate(String value) {
            return field(POT_CREATION_DATE, value);
        }

        public Builder poRevisionDate(String value) {
            return field(PO_REVISION_DATE, value);
        }

        public Builder lastTranslator(String value) {
            return field(LAST_TRANSLATOR, value);
        }

        public Builder languageTeam(String value) {
            return field(LANGUAGE_TEAM, value);
        }

        public Builder language(String value) {
            return field(LANGUAGE, value);
        }

        public Builder mimeVersion(String value) {
            return field(MIME_VERSION, value);
        }

        public Builder contentType(String value) {
            return field(CONTENT_TYPE, value);
        }

        public Builder contentTransferEncoding(String value) {
            return field(CONTENT_TRANSFER_ENCODING, value);
        }

        public Builder pluralForms(String value) {
            return field(PLURAL_FORMS, value);
        }

        public Builder pluralForms(PluralForms pluralForms) {
            return field(PLURAL_FORMS, pluralForms != null ? pluralForms.toHeaderValue() : null);
        }

        public Builder generator(String value) {
            return field(X_GENERATOR, value);
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

        public Builder addFlag(String flag) {
            this.flags.add(flag);
            return this;
        }

        public Builder flags(Collection<String> flags) {
            this.flags.clear();
            this.flags.addAll(flags);
            return this;
        }

        /**
         * Creates a minimal header with common defaults.
         */
        public Builder withDefaults() {
            fields.putIfAbsent(MIME_VERSION, "1.0");
            fields.putIfAbsent(CONTENT_TYPE, "text/plain; charset=UTF-8");
            fields.putIfAbsent(CONTENT_TRANSFER_ENCODING, "8bit");
            return this;
        }

        public POHeader build() {
            return new POHeader(this);
        }
    }
}
