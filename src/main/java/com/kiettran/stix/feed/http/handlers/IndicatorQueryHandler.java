package com.kiettran.stix.feed.http.handlers;

import com.kiettran.stix.feed.domain.PatternType;
import com.kiettran.stix.feed.error.ErrorResponse;
import com.kiettran.stix.feed.error.ErrorType;
import com.kiettran.stix.feed.http.RequestContext;
import com.kiettran.stix.feed.http.ResponseWriter;
import com.kiettran.stix.feed.json.JsonMapper;
import com.kiettran.stix.feed.store.InMemoryIndicatorStore;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class IndicatorQueryHandler implements Handler {

    private final InMemoryIndicatorStore store;
    private final ResponseWriter writer;

    public IndicatorQueryHandler(InMemoryIndicatorStore store, JsonMapper json) {
        this.store = store;
        this.writer = new ResponseWriter(json);
    }

    @Override
    public void handle(RequestContext ctx) throws IOException {
        try {
            PatternType patternType = parsePatternType(ctx.queryParam("pattern_type"));
            OffsetDateTime validFromAfter = parseInstant(ctx.queryParam("valid_from_after"));
            Integer confidenceGte = parseInt(ctx.queryParam("confidence_gte"));
            int limit  = parseLimit(ctx.queryParam("limit"));
            int offset = parseOffset(ctx.queryParam("offset"));
            List<String> labels = ctx.queryParamValues("label");

            InMemoryIndicatorStore.Query q = new InMemoryIndicatorStore.Query(
                patternType, validFromAfter, confidenceGte, labels, limit, offset
            );
            InMemoryIndicatorStore.QueryResult result = store.query(q);
            writer.writeJson(ctx.exchange(), 200, result, ctx.traceId());

        } catch (BadQueryException bqe) {
            writer.writeJson(ctx.exchange(), 400,
                ErrorResponse.of(ErrorType.BAD_REQUEST, bqe.getMessage(), ctx.traceId()),
                ctx.traceId());
        }
    }

    private static PatternType parsePatternType(String s) {
        if (s == null || s.isBlank()) return null;
        try { return PatternType.from(s); }
        catch (IllegalArgumentException e) {
            throw new BadQueryException("pattern_type must be one of [stix, snort, yara, pcre]");
        }
    }

    private static OffsetDateTime parseInstant(String s) {
        if (s == null || s.isBlank()) return null;
        try { return OffsetDateTime.parse(s); }
        catch (DateTimeParseException e) {
            throw new BadQueryException("valid_from_after must be ISO 8601 (e.g. 2026-01-01T00:00:00Z)");
        }
    }

    private static Integer parseInt(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) {
            throw new BadQueryException("confidence_gte must be an integer");
        }
    }

    private static int parseLimit(String s) {
        if (s == null || s.isBlank()) return 50;
        try { int n = Integer.parseInt(s); return Math.max(1, Math.min(200, n)); }
        catch (NumberFormatException e) {
            throw new BadQueryException("limit must be an integer in [1, 200]");
        }
    }

    private static int parseOffset(String s) {
        if (s == null || s.isBlank()) return 0;
        try { return Math.max(0, Integer.parseInt(s)); }
        catch (NumberFormatException e) {
            throw new BadQueryException("offset must be a non-negative integer");
        }
    }

    private static final class BadQueryException extends RuntimeException {
        BadQueryException(String msg) { super(msg); }
    }
}
