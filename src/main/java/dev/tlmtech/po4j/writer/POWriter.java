package dev.tlmtech.po4j.writer;

import dev.tlmtech.po4j.model.POEntry;
import dev.tlmtech.po4j.model.POFile;
import dev.tlmtech.po4j.model.POHeader;
import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Writer for GNU gettext PO files.
 *
 * <p>This writer produces PO files that are compatible with GNU gettext tools.
 * It handles all entry types, comment types, and proper string escaping.
 *
 * <p>Example usage:
 * <pre>{@code
 * try (OutputStream os = new FileOutputStream("output.po")) {
 *     POWriter.write(poFile, os, StandardCharsets.UTF_8);
 * }
 * }</pre>
 */
public class POWriter implements Closeable {

    private final Writer writer;
    private final POWriterOptions options;
    private final String newline;

    /**
     * Creates a writer with the given output writer and options.
     */
    public POWriter(Writer writer, @Nullable POWriterOptions options) {
        this.writer = new BufferedWriter(writer);
        this.options = options != null ? options : POWriterOptions.defaults();
        this.newline = this.options.getLineSeparator();
    }

    /**
     * Creates a writer with default options.
     */
    public POWriter(Writer writer) {
        this(writer, null);
    }

    // --- Static factory methods ---

    /**
     * Writes a POFile to an output stream.
     */
    public static void write(POFile poFile, OutputStream outputStream, Charset charset) throws IOException {
        write(poFile, outputStream, charset, POWriterOptions.defaults());
    }

    /**
     * Writes a POFile to an output stream with options.
     */
    public static void write(POFile poFile, OutputStream outputStream, Charset charset, POWriterOptions options)
            throws IOException {
        try (POWriter writer = new POWriter(new OutputStreamWriter(outputStream, charset), options)) {
            writer.write(poFile);
        }
    }

    /**
     * Writes a POFile to a writer.
     */
    public static void write(POFile poFile, Writer writer) throws IOException {
        write(poFile, writer, POWriterOptions.defaults());
    }

    /**
     * Writes a POFile to a writer with options.
     */
    public static void write(POFile poFile, Writer writer, POWriterOptions options) throws IOException {
        try (POWriter poWriter = new POWriter(writer, options)) {
            poWriter.write(poFile);
        }
    }

    /**
     * Writes a POFile to a string.
     */
    public static String writeToString(POFile poFile) throws IOException {
        return writeToString(poFile, POWriterOptions.defaults());
    }

    /**
     * Writes a POFile to a string with options.
     */
    public static String writeToString(POFile poFile, POWriterOptions options) throws IOException {
        StringWriter sw = new StringWriter();
        try (POWriter writer = new POWriter(sw, options)) {
            writer.write(poFile);
        }
        return sw.toString();
    }

    // --- Main write method ---

    /**
     * Writes the POFile to the output.
     */
    public void write(POFile poFile) throws IOException {
        // Write header first
        poFile.getHeader().ifPresent(header -> {
            try {
                writeHeader(header);
                writer.write(newline);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        // Get entries
        List<POEntry> entries = new ArrayList<>(poFile.getEntries());

        // Sort if requested
        if (options.isSortEntries()) {
            entries.sort(Comparator.comparing(POEntry::getMsgid));
        }

        // Write entries
        boolean first = !poFile.getHeader().isPresent();
        for (POEntry entry : entries) {
            if (!first) {
                writer.write(newline);
            }
            writeEntry(entry, false);
            first = false;
        }

        // Write obsolete entries if requested
        if (options.isWriteObsolete() && !poFile.getObsoleteEntries().isEmpty()) {
            for (POEntry entry : poFile.getObsoleteEntries()) {
                writer.write(newline);
                writeEntry(entry, true);
            }
        }

        writer.flush();
    }

    // --- Header writing ---

    private void writeHeader(POHeader header) throws IOException {
        // Write translator comments
        for (String comment : header.getTranslatorComments()) {
            writer.write("# ");
            writer.write(comment);
            writer.write(newline);
        }

        // Write flags
        if (!header.getFlags().isEmpty()) {
            writer.write("#, ");
            writer.write(String.join(", ", header.getFlags()));
            writer.write(newline);
        }

        // Write msgid ""
        writer.write("msgid \"\"");
        writer.write(newline);

        // Write msgstr with header content
        String msgstr = header.toMsgstr();
        writeStringLines("msgstr", msgstr, false);
    }

    // --- Entry writing ---

    private void writeEntry(POEntry entry, boolean forceObsolete) throws IOException {
        boolean obsolete = forceObsolete || entry.isObsolete();
        String prefix = obsolete ? "#~ " : "";

        // Write translator comments
        for (String comment : entry.getTranslatorComments()) {
            if (obsolete) {
                writer.write("#~ # ");
            } else {
                writer.write("# ");
            }
            writer.write(comment);
            writer.write(newline);
        }

        // Write extracted comments
        for (String comment : entry.getExtractedComments()) {
            if (obsolete) {
                writer.write("#~ ");
            }
            writer.write("#. ");
            writer.write(comment);
            writer.write(newline);
        }

        // Write references
        if (!entry.getReferences().isEmpty()) {
            if (obsolete) {
                writer.write("#~ ");
            }
            writer.write("#: ");
            writer.write(String.join(" ", entry.getReferences()));
            writer.write(newline);
        }

        // Write flags
        if (!entry.getFlags().isEmpty()) {
            if (obsolete) {
                writer.write("#~ ");
            }
            writer.write("#, ");
            writer.write(String.join(", ", entry.getFlags()));
            writer.write(newline);
        }

        // Write previous values
        entry.getPreviousMsgctxt().ifPresent(prev -> {
            try {
                if (obsolete) {
                    writer.write("#~| msgctxt ");
                } else {
                    writer.write("#| msgctxt ");
                }
                writer.write(StringEscaper.quoteAndEscape(prev));
                writer.write(newline);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        entry.getPreviousMsgid().ifPresent(prev -> {
            try {
                if (obsolete) {
                    writer.write("#~| msgid ");
                } else {
                    writer.write("#| msgid ");
                }
                writer.write(StringEscaper.quoteAndEscape(prev));
                writer.write(newline);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        entry.getPreviousMsgidPlural().ifPresent(prev -> {
            try {
                if (obsolete) {
                    writer.write("#~| msgid_plural ");
                } else {
                    writer.write("#| msgid_plural ");
                }
                writer.write(StringEscaper.quoteAndEscape(prev));
                writer.write(newline);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        // Write msgctxt
        entry.getMsgctxt().ifPresent(msgctxt -> {
            try {
                writeStringLines(prefix + "msgctxt", msgctxt, obsolete);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        // Write msgid
        writeStringLines(prefix + "msgid", entry.getMsgid(), obsolete);

        // Write msgid_plural or msgstr
        if (entry.isPlural()) {
            writeStringLines(prefix + "msgid_plural", entry.getMsgidPlural().orElse(""), obsolete);

            List<String> msgstrPlural = entry.getMsgstrPlural();
            for (int i = 0; i < msgstrPlural.size(); i++) {
                writeStringLines(prefix + "msgstr[" + i + "]", msgstrPlural.get(i), obsolete);
            }
        } else {
            writeStringLines(prefix + "msgstr", entry.getMsgstr().orElse(""), obsolete);
        }
    }

    // --- String writing ---

    private void writeStringLines(String keyword, String value, boolean obsolete) throws IOException {
        // value is non-null, so escape() will return non-null
        String escaped = java.util.Objects.requireNonNull(StringEscaper.escape(value));

        // Check if we need multi-line format
        boolean needsMultiLine = options.isWrapStrings()
                && (keyword.length() + 2 + escaped.length() > options.getMaxLineWidth() || escaped.contains("\\n"));

        if (!needsMultiLine) {
            // Single line format
            writer.write(keyword);
            writer.write(" \"");
            writer.write(escaped);
            writer.write("\"");
            writer.write(newline);
            return;
        }

        // Multi-line format
        writer.write(keyword);
        writer.write(" \"\"");
        writer.write(newline);

        // Split on \n characters
        List<String> lines = splitForWrapping(escaped);
        String linePrefix = obsolete ? "#~ " : "";

        for (String line : lines) {
            writer.write(linePrefix);
            writer.write("\"");
            writer.write(line);
            writer.write("\"");
            writer.write(newline);
        }
    }

    private List<String> splitForWrapping(String escaped) {
        List<String> result = new ArrayList<>();
        int maxLen = options.getMaxLineWidth() - 4; // Account for quotes and prefix

        // First, split on \n
        String[] parts = escaped.split("(?<=\\\\n)", -1);

        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }

            // If the part is short enough, add it directly
            if (part.length() <= maxLen) {
                result.add(part);
                continue;
            }

            // Need to wrap this part
            int pos = 0;
            while (pos < part.length()) {
                int end = Math.min(pos + maxLen, part.length());

                // Try to break at a space
                if (end < part.length()) {
                    int spacePos = part.lastIndexOf(' ', end);
                    if (spacePos > pos) {
                        end = spacePos + 1;
                    }
                }

                // Don't break in the middle of an escape sequence
                if (end < part.length() && end > 0) {
                    char prev = part.charAt(end - 1);
                    if (prev == '\\') {
                        end--;
                    }
                }

                result.add(part.substring(pos, end));
                pos = end;
            }
        }

        return result;
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
