package com.kiettran.stix.feed.kafka;

import com.kiettran.stix.feed.config.KafkaConfig;
import com.kiettran.stix.feed.domain.Indicator;
import com.kiettran.stix.feed.json.JsonMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publishes indicator JSON to Kafka via the plain {@code kafka-clients}
 * producer.
 *
 * Using {@code kafka-clients} directly rather than {@code spring-kafka}
 * trades a small amount of explicit configuration for a much smaller
 * dependency footprint and full visibility into producer behavior. The
 * producer is configured for at-least-once-with-idempotence semantics:
 * {@code acks=all} forces commit on all in-sync replicas, and
 * {@code enable.idempotence=true} prevents duplicates on retry. The two
 * combined give exactly-once-per-partition delivery without needing a
 * transactional producer.
 *
 * Each record carries three Kafka headers — {@code content-type},
 * {@code stix-version}, and {@code trace-id} — so consumers can route or
 * correlate without parsing the body.
 */
public final class IndicatorPublisher implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(IndicatorPublisher.class);

    private final KafkaConfig config;
    private final JsonMapper json;
    private final Producer<String, byte[]> producer;
    private volatile boolean healthy = true;

    public IndicatorPublisher(KafkaConfig config, JsonMapper json) {
        this(config, json, defaultProducer(config));
    }

    /** Test seam — pass an injected producer (e.g., MockProducer). */
    public IndicatorPublisher(KafkaConfig config, JsonMapper json, Producer<String, byte[]> producer) {
        this.config = config;
        this.json = json;
        this.producer = producer;
    }

    private static Producer<String, byte[]> defaultProducer(KafkaConfig c) {
        Properties p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,         c.bootstrapServers());
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,      StringSerializer.class.getName());
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,    ByteArraySerializer.class.getName());
        p.put(ProducerConfig.ACKS_CONFIG,                      "all");
        p.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,        "true");
        p.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        p.put(ProducerConfig.RETRIES_CONFIG,                   Integer.MAX_VALUE);
        p.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,        c.requestTimeoutMs());
        p.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG,       c.deliveryTimeoutMs());
        return new KafkaProducer<>(p);
    }

    /**
     * Publishes synchronously, returning the broker-confirmed
     * {@link RecordMetadata} on success. Throws {@link PublishException}
     * on any failure mode (timeout, broker error, serialization error);
     * the calling handler maps that to a 503 {@code kafka_unavailable}
     * response.
     *
     * The {@code healthy} flag flips false on failure and true on success
     * so {@code /ready} reflects current Kafka state without an extra probe.
     */
    public RecordMetadata publish(Indicator indicator, String traceId) {
        try {
            byte[] value = json.writeBytes(indicator);
            ProducerRecord<String, byte[]> rec = new ProducerRecord<>(
                config.topic(), null, indicator.id(), value
            );
            rec.headers().add(new RecordHeader("content-type", "application/json".getBytes(StandardCharsets.UTF_8)));
            rec.headers().add(new RecordHeader("stix-version", "2.1".getBytes(StandardCharsets.UTF_8)));
            if (traceId != null) {
                rec.headers().add(new RecordHeader("trace-id", traceId.getBytes(StandardCharsets.UTF_8)));
            }
            RecordMetadata md = producer.send(rec).get(
                config.deliveryTimeoutMs() + 1000L, TimeUnit.MILLISECONDS
            );
            healthy = true;
            return md;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            healthy = false;
            throw new PublishException("Kafka publish interrupted", e);
        } catch (ExecutionException | TimeoutException | IOException e) {
            healthy = false;
            log.warn("Kafka publish failed for id={}: {}", indicator.id(), e.toString());
            throw new PublishException("Kafka publish failed: " + e.getMessage(), e);
        }
    }

    public boolean isHealthy() { return healthy; }

    @Override
    public void close() {
        try { producer.close(Duration.ofSeconds(5)); }
        catch (Exception e) { log.warn("Producer close raised: {}", e.toString()); }
    }

    public static final class PublishException extends RuntimeException {
        public PublishException(String msg, Throwable cause) { super(msg, cause); }
    }
}
