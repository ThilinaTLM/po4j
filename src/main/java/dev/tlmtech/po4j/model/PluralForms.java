package dev.tlmtech.po4j.model;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents the Plural-Forms header value from a PO file.
 * Contains the number of plural forms and the expression to determine which form to use.
 *
 * <p>Example header value: {@code nplurals=3; plural=(n==1 ? 0 : n>=2 && n<=4 ? 1 : 2);}
 */
public final class PluralForms {

    private static final Pattern PLURAL_FORMS_PATTERN = Pattern.compile(
            "nplurals\\s*=\\s*(\\d+)\\s*;\\s*plural\\s*=\\s*(.+?)\\s*;?\\s*$", Pattern.CASE_INSENSITIVE);

    private final int nplurals;
    private final String pluralExpression;

    private PluralForms(int nplurals, String pluralExpression) {
        if (nplurals < 1) {
            throw new IllegalArgumentException("nplurals must be at least 1");
        }
        this.nplurals = nplurals;
        this.pluralExpression = Objects.requireNonNull(pluralExpression, "pluralExpression must not be null");
    }

    /**
     * Creates a new PluralForms instance.
     *
     * @param nplurals         the number of plural forms (must be >= 1)
     * @param pluralExpression the C expression to evaluate for input n
     * @return a new PluralForms instance
     */
    public static PluralForms of(int nplurals, String pluralExpression) {
        return new PluralForms(nplurals, pluralExpression);
    }

    /**
     * Parses a Plural-Forms header value.
     *
     * @param value the header value, e.g., "nplurals=2; plural=(n != 1);"
     * @return an Optional containing the parsed PluralForms, or empty if parsing fails
     */
    public static Optional<PluralForms> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = PLURAL_FORMS_PATTERN.matcher(value.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }

        try {
            int nplurals = Integer.parseInt(matcher.group(1));
            String expression = matcher.group(2).trim();
            // Remove trailing semicolon if present
            if (expression.endsWith(";")) {
                expression = expression.substring(0, expression.length() - 1).trim();
            }
            return Optional.of(new PluralForms(nplurals, expression));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Returns the number of plural forms.
     */
    public int getNplurals() {
        return nplurals;
    }

    /**
     * Returns the plural expression (C expression evaluated for input n).
     */
    public String getPluralExpression() {
        return pluralExpression;
    }

    /**
     * Formats the plural forms back to header value format.
     */
    public String toHeaderValue() {
        return String.format("nplurals=%d; plural=%s;", nplurals, pluralExpression);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PluralForms that)) return false;
        return nplurals == that.nplurals && Objects.equals(pluralExpression, that.pluralExpression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nplurals, pluralExpression);
    }

    @Override
    public String toString() {
        return "PluralForms{nplurals=" + nplurals + ", plural=" + pluralExpression + "}";
    }
}
