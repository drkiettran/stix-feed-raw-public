package com.kiettran.stix.feed.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Permitted values for STIX 2.1 indicator pattern_type.
 * https://docs.oasis-open.org/cti/stix/v2.1/os/stix-v2.1-os.html
 */
public enum PatternType {
    STIX("stix"),
    SNORT("snort"),
    YARA("yara"),
    PCRE("pcre");

    private final String value;

    PatternType(String value) { this.value = value; }

    @JsonValue
    public String value() { return value; }

    @JsonCreator
    public static PatternType from(String s) {
        if (s == null) return null;
        for (PatternType p : values()) {
            if (p.value.equalsIgnoreCase(s)) return p;
        }
        throw new IllegalArgumentException("Unknown pattern_type: " + s);
    }
}
