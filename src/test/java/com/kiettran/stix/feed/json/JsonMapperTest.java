package com.kiettran.stix.feed.json;

import com.kiettran.stix.feed.domain.Indicator;
import com.kiettran.stix.feed.domain.PatternType;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonMapperTest {

    private final JsonMapper json = new JsonMapper();

    @Test
    void serializeAndParseIndicatorRoundTrip() throws Exception {
        OffsetDateTime t = OffsetDateTime.of(2026, 5, 5, 14, 0, 0, 0, ZoneOffset.UTC);
        Indicator original = new Indicator(
            "indicator", "2.1",
            "indicator--8e2e2d2b-17d4-4cbf-938f-98ee46b3cd3f",
            t, t, "round-trip", null,
            List.of("malicious-activity"),
            "[ipv4-addr:value = '203.0.113.1']",
            PatternType.STIX, "2.1", t, t.plusDays(30),
            List.of("recon"), 75
        );

        String s = json.writeString(original);
        assertNotNull(s);
        assertTrue(s.contains("\"spec_version\":\"2.1\""));

        Indicator parsed = json.read(s, Indicator.class);
        assertEquals(original, parsed);
    }

    @Test
    void datesSerializeAsIso8601String() throws Exception {
        record Sample(OffsetDateTime when) {}
        Sample s = new Sample(OffsetDateTime.of(2026, 5, 5, 14, 0, 0, 0, ZoneOffset.UTC));
        String result = json.writeString(s);
        // Must NOT be a number (epoch); must be an ISO string
        assertTrue(result.contains("2026-05-05"), () -> "expected ISO date in: " + result);
        assertTrue(result.contains("\"when\":\""), () -> "expected string-quoted date in: " + result);
    }

    @Test
    void unknownPropertiesAreIgnored() throws Exception {
        // Confirms FAIL_ON_UNKNOWN_PROPERTIES is disabled
        record Small(String name) {}
        String input = "{\"name\":\"x\",\"unknown_field\":42}";
        Small parsed = json.read(input, Small.class);
        assertEquals("x", parsed.name());
    }

    @Test
    void nullFieldsAreOmittedFromOutput() throws Exception {
        record Sample(String present, String absent) {}
        Sample s = new Sample("yes", null);
        String out = json.writeString(s);
        assertTrue(out.contains("\"present\":\"yes\""));
        assertTrue(!out.contains("absent"), () -> "null field should be omitted: " + out);
    }
}
