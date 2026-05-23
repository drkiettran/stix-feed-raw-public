package com.kiettran.stix.feed.http.handlers;

import com.kiettran.stix.feed.domain.Indicator;
import com.kiettran.stix.feed.error.ErrorDetail;
import com.kiettran.stix.feed.error.ErrorResponse;
import com.kiettran.stix.feed.error.ErrorType;
import com.kiettran.stix.feed.http.RequestContext;
import com.kiettran.stix.feed.http.ResponseWriter;
import com.kiettran.stix.feed.json.JsonMapper;
import com.kiettran.stix.feed.kafka.IndicatorPublisher;
import com.kiettran.stix.feed.security.JwtTokenVerifier;
import com.kiettran.stix.feed.store.InMemoryIndicatorStore;
import com.kiettran.stix.feed.validation.IndicatorValidator;
import com.kiettran.stix.feed.validation.ValidationError;
import com.kiettran.stix.feed.validation.ValidationResult;

import java.io.IOException;
import java.util.List;

/**
 * Handles {@code POST /api/v1/indicators}: parses, validates, publishes,
 * persists, responds.
 *
 * Two design choices are worth calling out:
 *
 * <p><b>Role check lives here, not in a filter.</b> Authentication (token
 * present and valid) is enforced upstream by {@link
 * com.kiettran.stix.feed.http.filters.JwtFilter JwtFilter}; authorization
 * (role sufficient for this endpoint) is enforced inside the handler.
 * Different endpoints require different roles, and a single filter would
 * have to know about each.
 *
 * <p><b>Publish-then-persist.</b> The Kafka publish runs before the store
 * write. Reverse the order and a successful store with a failed publish
 * leaves the system in a state where the indicator exists locally but never
 * reached downstream consumers. With this ordering, a Kafka outage produces
 * a 503 and no local state change — clients can retry safely.
 */
public final class IndicatorPostHandler implements Handler {

    private static final int MAX_BODY = 64 * 1024;
    private static final String ROLE_ANALYST = "analyst";

    private final IndicatorValidator validator;
    private final IndicatorPublisher publisher;
    private final InMemoryIndicatorStore store;
    private final JsonMapper json;
    private final ResponseWriter writer;

    public IndicatorPostHandler(IndicatorValidator validator,
                                IndicatorPublisher publisher,
                                InMemoryIndicatorStore store,
                                JsonMapper json) {
        this.validator = validator;
        this.publisher = publisher;
        this.store = store;
        this.json = json;
        this.writer = new ResponseWriter(json);
    }

    @Override
    public void handle(RequestContext ctx) throws IOException {
        JwtTokenVerifier.Principal p = ctx.principal();
        if (p == null || !p.hasRole(ROLE_ANALYST)) {
            writer.writeJson(ctx.exchange(), 403,
                ErrorResponse.of(ErrorType.FORBIDDEN, "Requires role: analyst", ctx.traceId()),
                ctx.traceId());
            return;
        }

        Indicator indicator;
        try {
            byte[] body = ctx.readAllBytes(MAX_BODY);
            if (body.length == 0) {
                writer.writeJson(ctx.exchange(), 400,
                    ErrorResponse.of(ErrorType.BAD_REQUEST, "Empty body", ctx.traceId()),
                    ctx.traceId());
                return;
            }
            indicator = json.read(body, Indicator.class);
        } catch (Exception e) {
            writer.writeJson(ctx.exchange(), 400,
                ErrorResponse.of(ErrorType.BAD_REQUEST,
                    "Malformed JSON: " + e.getMessage(), ctx.traceId()),
                ctx.traceId());
            return;
        }

        ValidationResult vr = validator.validate(indicator);
        if (!vr.isValid()) {
            List<ErrorDetail> details = vr.errors().stream()
                .map(ve -> new ErrorDetail(ve.field(), ve.issue()))
                .toList();
            writer.writeJson(ctx.exchange(), 400,
                ErrorResponse.of(ErrorType.VALIDATION_FAILED,
                    "Indicator validation failed", details, ctx.traceId()),
                ctx.traceId());
            return;
        }

        // Publish first; on failure return 503 and do NOT persist (see class
        // header for why this ordering matters).
        publisher.publish(indicator, ctx.traceId());

        if (!store.putIfAbsent(indicator)) {
            writer.writeJson(ctx.exchange(), 409,
                ErrorResponse.of(ErrorType.CONFLICT,
                    "Indicator with this id already exists", ctx.traceId()),
                ctx.traceId());
            return;
        }

        writer.writeJson(ctx.exchange(), 201, indicator, ctx.traceId());
    }

    /** Test seam — keeps the validation reference even when not used elsewhere. */
    @SuppressWarnings("unused")
    private ValidationError sample() { return new ValidationError("x", "y"); }
}
