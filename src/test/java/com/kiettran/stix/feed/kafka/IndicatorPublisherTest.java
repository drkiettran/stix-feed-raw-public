package com.kiettran.stix.feed.kafka;

import com.kiettran.stix.feed.config.KafkaConfig;
import com.kiettran.stix.feed.domain.Indicator;
import com.kiettran.stix.feed.domain.PatternType;
import com.kiettran.stix.feed.json.JsonMapper;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndicatorPublisherTest {

    private static final KafkaConfig CFG = new KafkaConfig(
        "mock://test", "stix.indicators.test", 5000, 10000
    );

    private Indicator sample() {
        OffsetDateTime t = OffsetDateTime.of(2026, 5, 5, 14, 0, 0, 0, ZoneOffset.UTC);
        return new Indicator(
            "indicator", "2.1",
            "indicator--8e2e2d2b-17d4-4cbf-938f-98ee46b3cd3f",
            t, t, "test", null,
            List.of("malicious-activity"),
            "[ipv4-addr:value = '203.0.113.1']",
            PatternType.STIX, "2.1", t, t.plusDays(30),
            List.of("c2"), 75
        );
    }

    @Test
    @DisplayName("Publishes record to configured topic with id as key")
    void publishesToTopicWithIdAsKey() {
        MockProducer<String, byte[]> mock = new MockProducer<>(true,
            new StringSerializer(), new ByteArraySerializer());
        IndicatorPublisher publisher = new IndicatorPublisher(CFG, new JsonMapper(), mock);

        Indicator i = sample();
        RecordMetadata md = publisher.publish(i, "trace-123");

        assertNotNull(md);
        assertEquals(1, mock.history().size());
        ProducerRecord<String, byte[]> sent = mock.history().get(0);
        assertEquals(CFG.topic(), sent.topic());
        assertEquals(i.id(), sent.key());
    }

    @Test
    @DisplayName("Sets content-type, stix-version, and trace-id headers")
    void setsHeaders() {
        MockProducer<String, byte[]> mock = new MockProducer<>(true,
            new StringSerializer(), new ByteArraySerializer());
        IndicatorPublisher publisher = new IndicatorPublisher(CFG, new JsonMapper(), mock);

        publisher.publish(sample(), "trace-abc");

        ProducerRecord<String, byte[]> sent = mock.history().get(0);
        Header contentType = sent.headers().lastHeader("content-type");
        Header stixVersion = sent.headers().lastHeader("stix-version");
        Header traceId     = sent.headers().lastHeader("trace-id");

        assertNotNull(contentType);
        assertNotNull(stixVersion);
        assertNotNull(traceId);
        assertEquals("application/json", new String(contentType.value(), StandardCharsets.UTF_8));
        assertEquals("2.1",              new String(stixVersion.value(), StandardCharsets.UTF_8));
        assertEquals("trace-abc",        new String(traceId.value(),     StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Serializes indicator to JSON in the value bytes")
    void valueIsJsonOfIndicator() throws Exception {
        MockProducer<String, byte[]> mock = new MockProducer<>(true,
            new StringSerializer(), new ByteArraySerializer());
        IndicatorPublisher publisher = new IndicatorPublisher(CFG, new JsonMapper(), mock);

        publisher.publish(sample(), null);
        byte[] value = mock.history().get(0).value();

        Indicator deserialized = new JsonMapper().read(value, Indicator.class);
        assertEquals(sample(), deserialized);
    }

    @Test
    @DisplayName("Trace-id header omitted when traceId is null")
    void noTraceIdHeaderWhenNull() {
        MockProducer<String, byte[]> mock = new MockProducer<>(true,
            new StringSerializer(), new ByteArraySerializer());
        IndicatorPublisher publisher = new IndicatorPublisher(CFG, new JsonMapper(), mock);

        publisher.publish(sample(), null);

        ProducerRecord<String, byte[]> sent = mock.history().get(0);
        assert sent.headers().lastHeader("trace-id") == null
            : "trace-id header should not be present";
    }

    @Test
    @DisplayName("Wraps producer error as PublishException and marks unhealthy")
    void wrapsProducerError() {
        // autoComplete=false so we control completion explicitly
        MockProducer<String, byte[]> mock = new MockProducer<>(false,
            new StringSerializer(), new ByteArraySerializer());
        IndicatorPublisher publisher = new IndicatorPublisher(CFG, new JsonMapper(), mock);
        assertTrue(publisher.isHealthy());

        // Run send in a separate thread so we can errorNext from this one
        Thread t = new Thread(() -> {
            assertThrows(IndicatorPublisher.PublishException.class,
                () -> publisher.publish(sample(), "trace-x"));
        });
        t.start();

        // Wait briefly for send() to enqueue, then force an error
        try {
            // Spin until there's a record awaiting completion
            long deadline = System.currentTimeMillis() + 2000;
            while (mock.history().isEmpty() && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }
            mock.errorNext(new RuntimeException("simulated broker outage"));
            t.join(3000);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }

        assertTrue(!publisher.isHealthy(), "publisher should be marked unhealthy after failure");
    }

    @Test
    @DisplayName("close() does not throw")
    void closeQuietly() {
        MockProducer<String, byte[]> mock = new MockProducer<>(true,
            new StringSerializer(), new ByteArraySerializer());
        IndicatorPublisher publisher = new IndicatorPublisher(CFG, new JsonMapper(), mock);
        publisher.close();
        assertTrue(mock.closed());
    }
}
