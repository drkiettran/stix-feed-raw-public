package com.kiettran.stix.feed.config;

import java.time.Duration;
import java.util.function.Function;

/**
 * Aggregate application configuration, loaded from environment variables.
 *
 * Without {@code @ConfigurationProperties} or {@code @Value} to do the
 * binding, configuration loading is a small set of explicit env lookups
 * with sensible defaults. The benefit is that every config value has a
 * single, traceable origin: a reader can grep for {@code SERVER_PORT} and
 * find both the env-var name and the default in one line. The cost is the
 * handful of helper methods at the bottom of this file.
 *
 * The {@link #fromEnvironment(Function)} overload accepts an env-source
 * function so tests can inject a {@code Map}-backed lookup without
 * mutating the real process environment. {@link #fromEnvironment()} is the
 * production entry point and delegates to the overload with
 * {@code System::getenv}.
 *
 * {@code JWT_SECRET} is the only required variable; everything else has a
 * default suitable for local development.
 */
public record AppConfig(
    ServerConfig server,
    KafkaConfig  kafka,
    JwtConfig    jwt
) {

    public static AppConfig fromEnvironment() {
        return fromEnvironment(System::getenv);
    }

    public static AppConfig fromEnvironment(Function<String, String> env) {
        ServerConfig server = new ServerConfig(
            envInt(env, "SERVER_PORT", 8080),
            envInt(env, "THREAD_POOL_SIZE", 50),
            envInt(env, "HTTP_BACKLOG", 0),
            Duration.ofSeconds(envInt(env, "SHUTDOWN_GRACE_SECONDS", 5))
        );

        KafkaConfig kafka = new KafkaConfig(
            env(env, "KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
            env(env, "KAFKA_TOPIC", "stix.indicators.v1"),
            envInt(env, "KAFKA_REQUEST_TIMEOUT_MS", 5000),
            envInt(env, "KAFKA_DELIVERY_TIMEOUT_MS", 10000)
        );

        JwtConfig jwt = new JwtConfig(
            envRequired(env, "JWT_SECRET"),
            Duration.ofSeconds(envInt(env, "JWT_TTL_SECONDS", 3600)),
            env(env, "JWT_ISSUER", "stix-feed-raw")
        );

        return new AppConfig(server, kafka, jwt);
    }

    private static String env(Function<String, String> env, String key, String fallback) {
        String v = env.apply(key);
        return (v == null || v.isBlank()) ? fallback : v;
    }

    private static String envRequired(Function<String, String> env, String key) {
        String v = env.apply(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Required env var missing: " + key);
        }
        return v;
    }

    private static int envInt(Function<String, String> env, String key, int fallback) {
        String v = env.apply(key);
        if (v == null || v.isBlank()) return fallback;
        try { return Integer.parseInt(v.trim()); }
        catch (NumberFormatException e) {
            throw new IllegalStateException("Env var " + key + " is not an integer: " + v);
        }
    }
}
