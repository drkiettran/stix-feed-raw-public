package com.kiettran.stix.feed.http.handlers;

import com.kiettran.stix.feed.http.RequestContext;
import com.kiettran.stix.feed.http.ResponseWriter;
import com.kiettran.stix.feed.json.JsonMapper;
import com.kiettran.stix.feed.kafka.IndicatorPublisher;
import com.kiettran.stix.feed.store.InMemoryIndicatorStore;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ReadyHandler implements Handler {

    private final IndicatorPublisher publisher;
    private final InMemoryIndicatorStore store;
    private final ResponseWriter writer;

    public ReadyHandler(IndicatorPublisher publisher, InMemoryIndicatorStore store, JsonMapper json) {
        this.publisher = publisher;
        this.store = store;
        this.writer = new ResponseWriter(json);
    }

    @Override
    public void handle(RequestContext ctx) throws IOException {
        Map<String, String> checks = new LinkedHashMap<>();
        checks.put("kafka", publisher.isHealthy() ? "UP" : "DOWN");
        checks.put("store", store.isHealthy() ? "UP" : "DOWN");
        boolean ready = checks.values().stream().allMatch("UP"::equals);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", ready ? "READY" : "NOT_READY");
        body.put("checks", checks);
        writer.writeJson(ctx.exchange(), ready ? 200 : 503, body, ctx.traceId());
    }
}
