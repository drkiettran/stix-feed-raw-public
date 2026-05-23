package com.kiettran.stix.feed.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * On-the-wire shape of every error response emitted by the API.
 *
 * Keeping a single record for all error cases means clients can parse error
 * bodies with one schema regardless of which endpoint failed, and a global
 * grep for {@code ErrorResponse.of(} surfaces every error site in the
 * codebase. The {@code @JsonInclude(NON_NULL)} setting drops the
 * {@code details} array when there are no field-level errors, keeping the
 * common 4xx body compact.
 *
 * The {@code trace_id} is propagated from the request's
 * {@link com.kiettran.stix.feed.http.filters.TracingFilter TracingFilter}
 * so a 5xx response a client sees can be correlated to the corresponding
 * server-side log entry without further coordination.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    @JsonProperty("timestamp") OffsetDateTime timestamp,
    @JsonProperty("status")    int status,
    @JsonProperty("error")     String error,
    @JsonProperty("message")   String message,
    @JsonProperty("details")   List<ErrorDetail> details,
    @JsonProperty("trace_id")  String traceId
) {
    public static ErrorResponse of(ErrorType type, String message, String traceId) {
        return new ErrorResponse(
            OffsetDateTime.now(),
            type.status(),
            type.code(),
            message,
            null,
            traceId
        );
    }

    public static ErrorResponse of(ErrorType type, String message, List<ErrorDetail> details, String traceId) {
        return new ErrorResponse(
            OffsetDateTime.now(),
            type.status(),
            type.code(),
            message,
            details == null || details.isEmpty() ? null : details,
            traceId
        );
    }
}
