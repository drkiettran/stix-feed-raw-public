package com.kiettran.stix.feed.store;

import com.kiettran.stix.feed.domain.Indicator;
import com.kiettran.stix.feed.domain.PatternType;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe in-memory store for indicators.
 *
 * The choice of an in-memory backing is deliberate. A production deployment
 * would persist to PostgreSQL or Elasticsearch, but for the goal of this
 * project — comparing framework-driven and framework-free implementations
 * of the same service — the data layer is held constant across both
 * versions so that storage characteristics are not a confounder. The boot
 * version uses an identical {@code ConcurrentHashMap} for the same reason.
 *
 * The store offers single-writer-wins semantics on insert via
 * {@link #putIfAbsent}: a duplicate id returns false and the caller maps
 * that to a 409 Conflict rather than overwriting the existing entry
 * silently.
 */
public final class InMemoryIndicatorStore {

    private final ConcurrentMap<String, Indicator> byId = new ConcurrentHashMap<>();

    /** @return true if newly stored, false if id already existed (caller maps to 409). */
    public boolean putIfAbsent(Indicator indicator) {
        return byId.putIfAbsent(indicator.id(), indicator) == null;
    }

    public Optional<Indicator> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public int size() { return byId.size(); }

    public boolean isHealthy() { return true; }

    public QueryResult query(Query q) {
        List<Indicator> all = new ArrayList<>(byId.values());

        List<Indicator> filtered = all.stream()
            .filter(i -> q.patternType() == null || q.patternType() == i.patternType())
            .filter(i -> q.validFromAfter() == null
                         || (i.validFrom() != null && i.validFrom().isAfter(q.validFromAfter())))
            .filter(i -> q.confidenceGte() == null
                         || (i.confidence() != null && i.confidence() >= q.confidenceGte()))
            .filter(i -> q.labels().isEmpty()
                         || (i.labels() != null && !java.util.Collections.disjoint(i.labels(), q.labels())))
            .sorted(Comparator.comparing(Indicator::created, Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

        int total = filtered.size();
        int from  = Math.min(q.offset(), total);
        int to    = Math.min(from + q.limit(), total);
        return new QueryResult(total, q.limit(), q.offset(), filtered.subList(from, to));
    }

    public record Query(
        PatternType patternType,
        OffsetDateTime validFromAfter,
        Integer confidenceGte,
        List<String> labels,
        int limit,
        int offset
    ) {
        public Query {
            if (labels == null) labels = List.of();
            if (limit  <= 0)    limit  = 50;
            if (limit  > 200)   limit  = 200;
            if (offset < 0)     offset = 0;
        }
    }

    public record QueryResult(int total, int limit, int offset, List<Indicator> items) {}
}
