package com.kiettran.stix.feed.http.filters;

import com.kiettran.stix.feed.error.ErrorResponse;
import com.kiettran.stix.feed.error.ErrorType;
import com.kiettran.stix.feed.http.RequestContext;
import com.kiettran.stix.feed.http.ResponseWriter;
import com.kiettran.stix.feed.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Catches {@link RuntimeException} raised anywhere downstream in the filter
 * chain or handler and converts it to a structured {@link ErrorResponse}.
 *
 * Position in the chain is intentional. {@link TracingFilter} runs first so
 * a trace id is always available when an error is reported. This filter
 * runs second so it can wrap everything below it. The {@link JwtFilter}
 * (when applicable) runs after this one — auth failures are handled
 * directly by the JwtFilter as 401 responses rather than as exceptions, so
 * they pass through this filter without intervention.
 *
 * Specific exception types get their own clauses so each maps to the right
 * HTTP status: {@link com.kiettran.stix.feed.kafka.IndicatorPublisher.PublishException
 * PublishException} becomes a 503, while everything else falls through to a
 * generic 500 with the trace id for correlation.
 */
public final class ErrorFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(ErrorFilter.class);

    private final ResponseWriter writer;

    public ErrorFilter(JsonMapper json) {
        this.writer = new ResponseWriter(json);
    }

    @Override
    public void doFilter(RequestContext ctx, FilterChain chain) throws IOException {
        try {
            chain.next(ctx);
        } catch (com.kiettran.stix.feed.kafka.IndicatorPublisher.PublishException e) {
            log.warn("Publish failure: {}", e.toString());
            writer.writeJson(ctx.exchange(), 503,
                ErrorResponse.of(ErrorType.KAFKA_UNAVAILABLE,
                    "Failed to publish to Kafka", ctx.traceId()), ctx.traceId());
        } catch (RuntimeException e) {
            log.error("Unhandled runtime exception", e);
            writer.writeJson(ctx.exchange(), 500,
                ErrorResponse.of(ErrorType.INTERNAL_ERROR, "Unexpected error", ctx.traceId()),
                ctx.traceId());
        }
    }
}
