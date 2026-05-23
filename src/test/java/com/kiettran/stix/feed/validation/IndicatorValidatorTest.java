package com.kiettran.stix.feed.validation;

import com.kiettran.stix.feed.domain.Indicator;
import com.kiettran.stix.feed.domain.PatternType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndicatorValidatorTest {

    private final IndicatorValidator validator = new IndicatorValidator();

    private Indicator validIndicator() {
        OffsetDateTime t = OffsetDateTime.of(2026, 5, 5, 14, 30, 0, 0, ZoneOffset.UTC);
        return new Indicator(
            "indicator",
            "2.1",
            "indicator--8e2e2d2b-17d4-4cbf-938f-98ee46b3cd3f",
            t, t, "test", null,
            List.of("malicious-activity"),
            "[file:hashes.'SHA-256' = 'aec070645fe53ee3b3763059376134f058cc337247c978add178b6ccdfb0019f']",
            PatternType.STIX,
            "2.1",
            t,
            t.plusDays(90),
            List.of("phishing"),
            85
        );
    }

    @Test
    @DisplayName("Valid indicator passes validation")
    void validatesHappyPath() {
        ValidationResult r = validator.validate(validIndicator());
        assertTrue(r.isValid(), () -> "expected valid, got: " + r.errors());
    }

    @Test
    @DisplayName("Wrong type field fails validation")
    void rejectsWrongType() {
        Indicator base = validIndicator();
        Indicator bad = new Indicator(
            "malware", base.specVersion(), base.id(),
            base.created(), base.modified(), base.name(), base.description(),
            base.indicatorTypes(), base.pattern(), base.patternType(),
            base.patternVersion(), base.validFrom(), base.validUntil(),
            base.labels(), base.confidence()
        );
        assertFalse(validator.validate(bad).isValid());
    }

    @Test
    @DisplayName("Malformed id fails validation")
    void rejectsMalformedId() {
        Indicator base = validIndicator();
        Indicator bad = new Indicator(
            base.type(), base.specVersion(), "indicator--bad-id",
            base.created(), base.modified(), base.name(), base.description(),
            base.indicatorTypes(), base.pattern(), base.patternType(),
            base.patternVersion(), base.validFrom(), base.validUntil(),
            base.labels(), base.confidence()
        );
        assertFalse(validator.validate(bad).isValid());
    }

    @Test
    @DisplayName("valid_until before valid_from fails validation")
    void rejectsValidUntilBeforeFrom() {
        Indicator base = validIndicator();
        Indicator bad = new Indicator(
            base.type(), base.specVersion(), base.id(),
            base.created(), base.modified(), base.name(), base.description(),
            base.indicatorTypes(), base.pattern(), base.patternType(),
            base.patternVersion(),
            base.validFrom(),
            base.validFrom().minusDays(1),
            base.labels(), base.confidence()
        );
        assertFalse(validator.validate(bad).isValid());
    }

    @Test
    @DisplayName("Confidence out of range fails validation")
    void rejectsBadConfidence() {
        Indicator base = validIndicator();
        Indicator bad = new Indicator(
            base.type(), base.specVersion(), base.id(),
            base.created(), base.modified(), base.name(), base.description(),
            base.indicatorTypes(), base.pattern(), base.patternType(),
            base.patternVersion(), base.validFrom(), base.validUntil(),
            base.labels(), 150
        );
        assertFalse(validator.validate(bad).isValid());
    }

    @Test
    @DisplayName("Empty pattern fails validation")
    void rejectsEmptyPattern() {
        Indicator base = validIndicator();
        Indicator bad = new Indicator(
            base.type(), base.specVersion(), base.id(),
            base.created(), base.modified(), base.name(), base.description(),
            base.indicatorTypes(), "  ", base.patternType(),
            base.patternVersion(), base.validFrom(), base.validUntil(),
            base.labels(), base.confidence()
        );
        assertFalse(validator.validate(bad).isValid());
    }

    @Test
    @DisplayName("Null pattern_type fails validation")
    void rejectsNullPatternType() {
        Indicator base = validIndicator();
        Indicator bad = new Indicator(
            base.type(), base.specVersion(), base.id(),
            base.created(), base.modified(), base.name(), base.description(),
            base.indicatorTypes(), base.pattern(), null,
            base.patternVersion(), base.validFrom(), base.validUntil(),
            base.labels(), base.confidence()
        );
        assertFalse(validator.validate(bad).isValid());
    }
}
