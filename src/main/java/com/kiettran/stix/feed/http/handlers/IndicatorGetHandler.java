package com.kiettran.stix.feed.http.handlers;

import com.kiettran.stix.feed.domain.Indicator;
import com.kiettran.stix.feed.error.ErrorResponse;
import com.kiettran.stix.feed.error.ErrorType;
import com.kiettran.stix.feed.http.RequestContext;
import com.kiettran.stix.feed.http.ResponseWriter;
import com.kiettran.stix.feed.json.JsonMapper;
import com.kiettran.stix.feed.store.InMemoryIndicatorStore;

import java.io.IOException;
import java.util.Optional;

public final class IndicatorGetHandler implements Handler {

    private final InMemoryIndicatorStore store;
    private final ResponseWriter writer;

    public IndicatorGetHandler(InMemoryIndicatorStore store, JsonMapper json) {
        this.store = store;
        this.writer = new ResponseWriter(json);
    }

    @Override
    public void handle(RequestContext ctx) throws IOException {
        String id = ctx.pathParam("id");
        if (id == null || id.isBlank()) {
            writer.writeJson(ctx.exchange(), 400,
                ErrorResponse.of(ErrorType.BAD_REQUEST, "id path param missing", ctx.traceId()),
                ctx.traceId());
            return;
        }
        // The path matcher restricts {id} to a single segment, but we further
        // sanity-check the prefix to avoid leaking invalid lookups into the store.
        if (!id.startsWith("indicator--")) {
            writer.writeJson(ctx.exchange(), 400,
                ErrorResponse.of(ErrorType.BAD_REQUEST, "id must start with indicator--", ctx.traceId()),
                ctx.traceId());
            return;
        }
        Optional<Indicator> found = store.findById(id);
        if (found.isEmpty()) {
            writer.writeJson(ctx.exchange(), 404,
                ErrorResponse.of(ErrorType.NOT_FOUND, "Indicator not found", ctx.traceId()),
                ctx.traceId());
            return;
        }
        writer.writeJson(ctx.exchange(), 200, found.get(), ctx.traceId());
    }
}
