package com.kiettran.stix.feed.domain;

import java.util.Set;

/**
 * STIX 2.1 indicator-type-ov open vocabulary.
 * Open vocabulary: values not in this set are allowed but discouraged;
 * we accept any non-empty string and log a warning when not in the OV.
 */
public final class IndicatorTypeOV {

    public static final Set<String> VALUES = Set.of(
        "anomalous-activity",
        "anonymization",
        "benign",
        "compromised",
        "malicious-activity",
        "attribution",
        "unknown"
    );

    private IndicatorTypeOV() {}

    public static boolean contains(String value) {
        return VALUES.contains(value);
    }
}
