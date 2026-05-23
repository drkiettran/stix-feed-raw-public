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
import com.kiettran.stix.feed.validation.ValidationResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class IndicatorBatchHandler implements Handler {

    private static final int MAX_BODY = 1024 * 1024;
    private static final int MAX_BATCH_SIZE = 100;
    private static final String ROLE_ANALYST = "analyst";

    public record BatchItemResult(int index, String id, int status, String error, List<ErrorDetail> details) {}
    public record BatchResponse(int total, int accepted, int rejected, List<BatchItemResult> items) {}

    private final IndicatorValidator validator;
    private final IndicatorPublisher publisher;
    private final InMemoryIndicatorStore store;
    private final JsonMapper json;
    private final ResponseWriter writer;

    public IndicatorBatchHandler(IndicatorValidator validator,
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

        Indicator[] batch;
        try {
            byte[] body = ctx.readAllBytes(MAX_BODY);
            batch = json.read(body, Indicator[].class);
        } catch (Exception e) {
            writer.writeJson(ctx.exchange(), 400,
                ErrorResponse.of(ErrorType.BAD_REQUEST,
                    "Malformed JSON array: " + e.getMessage(), ctx.traceId()),
                ctx.traceId());
            return;
        }

        if (batch == null || batch.length == 0) {
            writer.writeJson(ctx.exchange(), 400,
                ErrorResponse.of(ErrorType.BAD_REQUEST, "Empty batch", ctx.traceId()),
                ctx.traceId());
            return;
        }
        if (batch.length > MAX_BATCH_SIZE) {
            writer.writeJson(ctx.exchange(), 400,
                ErrorResponse.of(ErrorType.BAD_REQUEST,
                    "Batch exceeds max size " + MAX_BATCH_SIZE, ctx.traceId()),
                ctx.traceId());
            return;
        }

        List<BatchItemResult> results = new ArrayList<>(batch.length);
        int accepted = 0;
        int rejected = 0;

        for (int idx = 0; idx < batch.length; idx++) {
            Indicator i = batch[idx];
            ValidationResult vr = validator.validate(i);
            if (!vr.isValid()) {
                List<ErrorDetail> details = vr.errors().stream()
                    .map(ve -> new ErrorDetail(ve.field(), ve.issue())).toList();
                results.add(new BatchItemResult(idx, i == null ? null : i.id(), 400, "validation_failed", details));
                rejected++;
                continue;
            }

            try {
                publisher.publish(i, ctx.traceId());
            } catch (IndicatorPublisher.PublishException pe) {
                results.add(new BatchItemResult(idx, i.id(), 503, "kafka_unavailable", null));
                rejected++;
                continue;
            }

            if (!store.putIfAbsent(i)) {
                results.add(new BatchItemResult(idx, i.id(), 409, "conflict", null));
                rejected++;
                continue;
            }

            results.add(new BatchItemResult(idx, i.id(), 201, null, null));
            accepted++;
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("total", batch.length);
        body.put("accepted", accepted);
        body.put("rejected", rejected);
        body.put("items", results);

        int status = (rejected == 0) ? 201 : (accepted == 0 ? 400 : 207);
        writer.writeJson(ctx.exchange(), status, body, ctx.traceId());
    }
}
