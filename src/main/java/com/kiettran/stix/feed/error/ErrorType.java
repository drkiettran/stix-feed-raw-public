package com.kiettran.stix.feed.error;

/**
 * Error codes mapped to HTTP status. Used as the "error" field
 * in the standardized ErrorResponse body.
 */
public enum ErrorType {
    BAD_REQUEST(400, "bad_request"),
    VALIDATION_FAILED(400, "validation_failed"),
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "not_found"),
    METHOD_NOT_ALLOWED(405, "method_not_allowed"),
    CONFLICT(409, "conflict"),
    PAYLOAD_TOO_LARGE(413, "payload_too_large"),
    UNSUPPORTED_MEDIA_TYPE(415, "unsupported_media_type"),
    INTERNAL_ERROR(500, "internal_error"),
    KAFKA_UNAVAILABLE(503, "kafka_unavailable");

    private final int status;
    private final String code;

    ErrorType(int status, String code) {
        this.status = status;
        this.code = code;
    }

    public int status() { return status; }
    public String code() { return code; }
}
