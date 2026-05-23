package com.kiettran.stix.feed.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * STIX 2.1 Indicator (subset).
 *
 * Modeled as a Java record for value semantics. Optional fields are nullable.
 * Validation is performed by IndicatorValidator, not by this record.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Indicator(
    @JsonProperty("type")             String type,
    @JsonProperty("spec_version")     String specVersion,
    @JsonProperty("id")               String id,
    @JsonProperty("created")          OffsetDateTime created,
    @JsonProperty("modified")         OffsetDateTime modified,
    @JsonProperty("name")             String name,
    @JsonProperty("description")      String description,
    @JsonProperty("indicator_types")  List<String> indicatorTypes,
    @JsonProperty("pattern")          String pattern,
    @JsonProperty("pattern_type")     PatternType patternType,
    @JsonProperty("pattern_version")  String patternVersion,
    @JsonProperty("valid_from")       OffsetDateTime validFrom,
    @JsonProperty("valid_until")      OffsetDateTime validUntil,
    @JsonProperty("labels")           List<String> labels,
    @JsonProperty("confidence")       Integer confidence
) {
    public static final String EXPECTED_TYPE = "indicator";
    public static final String EXPECTED_SPEC_VERSION = "2.1";
}
