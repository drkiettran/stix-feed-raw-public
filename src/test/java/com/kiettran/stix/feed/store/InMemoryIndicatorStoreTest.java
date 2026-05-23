package com.kiettran.stix.feed.store;

import com.kiettran.stix.feed.domain.Indicator;
import com.kiettran.stix.feed.domain.PatternType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryIndicatorStoreTest {

    private final InMemoryIndicatorStore store = new InMemoryIndicatorStore();

    private Indicator newIndicator(PatternType pt, int confidence, List<String> labels) {
        OffsetDateTime t = OffsetDateTime.of(2026, 5, 5, 14, 0, 0, 0, ZoneOffset.UTC);
        return new Indicator(
            "indicator", "2.1",
            "indicator--" + UUID.randomUUID(),
            t, t, "test", null,
            List.of("malicious-activity"),
            "[file:hashes.'SHA-256' = 'aec070645fe53ee3b3763059376134f058cc337247c978add178b6ccdfb0019f']",
            pt, "2.1", t, t.plusDays(30),
            labels, confidence
        );
    }

    @Test
    @DisplayName("putIfAbsent returns true on first store, false on duplicate")
    void putIfAbsentDetectsConflict() {
        Indicator i = newIndicator(PatternType.STIX, 80, List.of("phishing"));
        assertTrue(store.putIfAbsent(i));
        assertFalse(store.putIfAbsent(i));
        assertEquals(1, store.size());
    }

    @Test
    @DisplayName("findById returns the stored indicator")
    void findByIdRoundTrip() {
        Indicator i = newIndicator(PatternType.STIX, 70, List.of("apt"));
        store.putIfAbsent(i);
        assertTrue(store.findById(i.id()).isPresent());
        assertEquals(i, store.findById(i.id()).get());
    }

    @Test
    @DisplayName("query filters by pattern_type")
    void queryFiltersByPatternType() {
        store.putIfAbsent(newIndicator(PatternType.STIX,  70, List.of("a")));
        store.putIfAbsent(newIndicator(PatternType.STIX,  80, List.of("b")));
        store.putIfAbsent(newIndicator(PatternType.YARA,  90, List.of("c")));

        InMemoryIndicatorStore.QueryResult r = store.query(new InMemoryIndicatorStore.Query(
            PatternType.STIX, null, null, List.of(), 50, 0
        ));
        assertEquals(2, r.total());
        r.items().forEach(it -> assertEquals(PatternType.STIX, it.patternType()));
    }

    @Test
    @DisplayName("query filters by confidence_gte")
    void queryFiltersByConfidence() {
        store.putIfAbsent(newIndicator(PatternType.STIX, 50, List.of("x")));
        store.putIfAbsent(newIndicator(PatternType.STIX, 80, List.of("y")));
        store.putIfAbsent(newIndicator(PatternType.STIX, 95, List.of("z")));

        InMemoryIndicatorStore.QueryResult r = store.query(new InMemoryIndicatorStore.Query(
            null, null, 80, List.of(), 50, 0
        ));
        assertEquals(2, r.total());
        r.items().forEach(it -> assertTrue(it.confidence() >= 80));
    }

    @Test
    @DisplayName("query filters by label intersection")
    void queryFiltersByLabel() {
        store.putIfAbsent(newIndicator(PatternType.STIX, 70, List.of("phishing", "apt")));
        store.putIfAbsent(newIndicator(PatternType.STIX, 70, List.of("malware")));
        store.putIfAbsent(newIndicator(PatternType.STIX, 70, List.of("ddos", "apt")));

        InMemoryIndicatorStore.QueryResult r = store.query(new InMemoryIndicatorStore.Query(
            null, null, null, List.of("apt"), 50, 0
        ));
        assertEquals(2, r.total());
        r.items().forEach(it -> assertTrue(it.labels().contains("apt")));
    }

    @Test
    @DisplayName("query honors pagination (limit and offset)")
    void queryPaginates() {
        for (int i = 0; i < 7; i++) {
            store.putIfAbsent(newIndicator(PatternType.STIX, 70, List.of("p")));
        }
        InMemoryIndicatorStore.QueryResult page1 = store.query(new InMemoryIndicatorStore.Query(
            null, null, null, List.of(), 3, 0
        ));
        InMemoryIndicatorStore.QueryResult page2 = store.query(new InMemoryIndicatorStore.Query(
            null, null, null, List.of(), 3, 3
        ));
        InMemoryIndicatorStore.QueryResult page3 = store.query(new InMemoryIndicatorStore.Query(
            null, null, null, List.of(), 3, 6
        ));
        assertEquals(7, page1.total());
        assertEquals(3, page1.items().size());
        assertEquals(3, page2.items().size());
        assertEquals(1, page3.items().size());
    }

    @Test
    @DisplayName("Query record clamps invalid limit values")
    void queryRecordClampsLimits() {
        InMemoryIndicatorStore.Query q1 = new InMemoryIndicatorStore.Query(null, null, null, null, -5, -5);
        assertEquals(50, q1.limit());
        assertEquals(0,  q1.offset());

        InMemoryIndicatorStore.Query q2 = new InMemoryIndicatorStore.Query(null, null, null, null, 500, 0);
        assertEquals(200, q2.limit());
    }
}
