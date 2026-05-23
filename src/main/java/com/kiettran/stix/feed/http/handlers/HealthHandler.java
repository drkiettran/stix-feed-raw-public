package com.kiettran.stix.feed.http.handlers;

import com.kiettran.stix.feed.http.RequestContext;
import com.kiettran.stix.feed.http.ResponseWriter;
import com.kiettran.stix.feed.json.JsonMapper;

import java.io.IOException;
import java.util.Map;

public final class HealthHandler implements Handler {

    private final ResponseWriter writer;

    public HealthHandler(JsonMapper json) {
        this.writer = new ResponseWriter(json);
    }

    @Override
    public void handle(RequestContext ctx) throws IOException {
        writer.writeJson(ctx.exchange(), 200, Map.of("status", "UP"), ctx.traceId());
    }
}
