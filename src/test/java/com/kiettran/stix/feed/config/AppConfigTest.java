package com.kiettran.stix.feed.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppConfigTest {

    private static final String SECRET = "test-secret-32-bytes-minimum-len!!";

    private static Function<String, String> env(Map<String, String> map) {
        return map::get;
    }

    @Test
    @DisplayName("Defaults applied when only JWT_SECRET is set")
    void defaultsApplied() {
        Map<String, String> m = new HashMap<>();
        m.put("JWT_SECRET", SECRET);
        AppConfig c = AppConfig.fromEnvironment(env(m));
        assertEquals(8080, c.server().port());
        assertEquals(50, c.server().threadPoolSize());
        assertEquals("localhost:9092", c.kafka().bootstrapServers());
        assertEquals("stix.indicators.v1", c.kafka().topic());
        assertEquals(SECRET, c.jwt().secret());
        assertEquals(3600, c.jwt().ttl().toSeconds());
        assertEquals("stix-feed-raw", c.jwt().issuer());
    }

    @Test
    @DisplayName("Overrides applied when env vars are set")
    void overridesApplied() {
        Map<String, String> m = new HashMap<>();
        m.put("JWT_SECRET", SECRET);
        m.put("SERVER_PORT", "9090");
        m.put("THREAD_POOL_SIZE", "16");
        m.put("KAFKA_BOOTSTRAP_SERVERS", "broker:9092");
        m.put("KAFKA_TOPIC", "custom.topic");
        m.put("JWT_TTL_SECONDS", "7200");
        m.put("JWT_ISSUER", "custom-issuer");
        AppConfig c = AppConfig.fromEnvironment(env(m));
        assertEquals(9090, c.server().port());
        assertEquals(16, c.server().threadPoolSize());
        assertEquals("broker:9092", c.kafka().bootstrapServers());
        assertEquals("custom.topic", c.kafka().topic());
        assertEquals(7200, c.jwt().ttl().toSeconds());
        assertEquals("custom-issuer", c.jwt().issuer());
    }

    @Test
    @DisplayName("Missing JWT_SECRET fails fast")
    void missingSecretFails() {
        IllegalStateException e = assertThrows(
            IllegalStateException.class,
            () -> AppConfig.fromEnvironment(env(Map.of()))
        );
        assert e.getMessage().contains("JWT_SECRET") : e.getMessage();
    }

    @Test
    @DisplayName("Blank JWT_SECRET fails fast")
    void blankSecretFails() {
        Map<String, String> m = new HashMap<>();
        m.put("JWT_SECRET", "   ");
        assertThrows(IllegalStateException.class, () -> AppConfig.fromEnvironment(env(m)));
    }

    @Test
    @DisplayName("Non-integer numeric env var fails fast")
    void nonIntegerFails() {
        Map<String, String> m = new HashMap<>();
        m.put("JWT_SECRET", SECRET);
        m.put("SERVER_PORT", "not-a-number");
        IllegalStateException e = assertThrows(
            IllegalStateException.class,
            () -> AppConfig.fromEnvironment(env(m))
        );
        assert e.getMessage().contains("SERVER_PORT") : e.getMessage();
    }

    @Test
    @DisplayName("Empty string env vars fall back to defaults")
    void emptyStringFallsBack() {
        Map<String, String> m = new HashMap<>();
        m.put("JWT_SECRET", SECRET);
        m.put("KAFKA_TOPIC", "");
        AppConfig c = AppConfig.fromEnvironment(env(m));
        assertEquals("stix.indicators.v1", c.kafka().topic());
    }
}
