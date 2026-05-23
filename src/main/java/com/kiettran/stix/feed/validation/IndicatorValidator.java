package com.kiettran.stix.feed.validation;

import com.kiettran.stix.feed.domain.Indicator;
import com.kiettran.stix.feed.domain.IndicatorTypeOV;

import java.util.regex.Pattern;

/**
 * Validates the STIX 2.1 indicator subset supported by this implementation.
 *
 * Each rule is one explicit check that either passes or appends a
 * {@link ValidationError} to the result. There are no annotations and no
 * reflection: a reader of this class can see every rule, in order, without
 * understanding a Bean Validation lifecycle. The cost is more lines than the
 * equivalent {@code @Pattern} / {@code @Min} annotation set in the Spring
 * Boot version; the gain is that failures are returned as values rather than
 * thrown as exceptions, which keeps the handler's flow linear and makes
 * partial validation (collecting all errors before returning) trivial.
 *
 * Some STIX 2.1 vocabularies — like {@code indicator_types} — are
 * intentionally open: a value not present in the standard list is still
 * accepted. Empty or blank values are flagged because the open-vocabulary
 * rule does not extend to "no value at all."
 */
public final class IndicatorValidator {

    private static final Pattern ID_PATTERN = Pattern.compile(
        "^indicator--[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
    );

    private static final int MAX_PATTERN_LEN = 4096;

    public ValidationResult validate(Indicator i) {
        ValidationResult r = new ValidationResult();
        if (i == null) {
            r.add("body", "request body is required");
            return r;
        }

        // type
        if (!Indicator.EXPECTED_TYPE.equals(i.type())) {
            r.add("type", "must equal \"" + Indicator.EXPECTED_TYPE + "\"");
        }

        // spec_version
        if (!Indicator.EXPECTED_SPEC_VERSION.equals(i.specVersion())) {
            r.add("spec_version", "must equal \"" + Indicator.EXPECTED_SPEC_VERSION + "\"");
        }

        // id
        if (i.id() == null || !ID_PATTERN.matcher(i.id()).matches()) {
            r.add("id", "must match indicator--<UUIDv4>");
        }

        // timestamps required
        if (i.created() == null)    r.add("created",    "is required (ISO 8601 UTC)");
        if (i.modified() == null)   r.add("modified",   "is required (ISO 8601 UTC)");
        if (i.validFrom() == null)  r.add("valid_from", "is required (ISO 8601 UTC)");

        // modified >= created
        if (i.created() != null && i.modified() != null
            && i.modified().isBefore(i.created())) {
            r.add("modified", "must be greater than or equal to created");
        }

        // valid_until > valid_from
        if (i.validFrom() != null && i.validUntil() != null
            && !i.validUntil().isAfter(i.validFrom())) {
            r.add("valid_until", "must be after valid_from");
        }

        // pattern
        if (i.pattern() == null || i.pattern().isBlank()) {
            r.add("pattern", "is required and must be non-empty");
        } else if (i.pattern().length() > MAX_PATTERN_LEN) {
            r.add("pattern", "must be at most " + MAX_PATTERN_LEN + " characters");
        }

        // pattern_type
        if (i.patternType() == null) {
            r.add("pattern_type", "must be one of [stix, snort, yara, pcre]");
        }

        // confidence
        if (i.confidence() != null) {
            int c = i.confidence();
            if (c < 0 || c > 100) {
                r.add("confidence", "must be between 0 and 100 inclusive");
            }
        }

        // indicator_types — open vocabulary; values outside the standard OV
        // are accepted, but blank or null entries are flagged because absence
        // is not a valid open-vocabulary value.
        if (i.indicatorTypes() != null) {
            for (int idx = 0; idx < i.indicatorTypes().size(); idx++) {
                String v = i.indicatorTypes().get(idx);
                if (v == null || v.isBlank()) {
                    r.add("indicator_types[" + idx + "]", "must be non-empty");
                }
            }
        }

        return r;
    }

    /** Convenience for tests: returns true iff the value is in the standard OV. */
    public static boolean isStandardIndicatorType(String v) {
        return IndicatorTypeOV.contains(v);
    }
}
