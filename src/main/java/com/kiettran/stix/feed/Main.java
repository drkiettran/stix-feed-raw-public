package com.kiettran.stix.feed;

import com.kiettran.stix.feed.config.AppConfig;
import com.kiettran.stix.feed.http.HttpServerBootstrap;
import com.kiettran.stix.feed.http.Router;
import com.kiettran.stix.feed.http.filters.ErrorFilter;
import com.kiettran.stix.feed.http.filters.JwtFilter;
import com.kiettran.stix.feed.http.filters.TracingFilter;
import com.kiettran.stix.feed.http.handlers.AuthTokenHandler;
import com.kiettran.stix.feed.http.handlers.HealthHandler;
import com.kiettran.stix.feed.http.handlers.IndicatorBatchHandler;
import com.kiettran.stix.feed.http.handlers.IndicatorGetHandler;
import com.kiettran.stix.feed.http.handlers.IndicatorPostHandler;
import com.kiettran.stix.feed.http.handlers.IndicatorQueryHandler;
import com.kiettran.stix.feed.http.handlers.ReadyHandler;
import com.kiettran.stix.feed.http.handlers.StaticResourceHandler;
import com.kiettran.stix.feed.json.JsonMapper;
import com.kiettran.stix.feed.kafka.IndicatorPublisher;
import com.kiettran.stix.feed.security.JwtTokenIssuer;
import com.kiettran.stix.feed.security.JwtTokenVerifier;
import com.kiettran.stix.feed.security.UserAuthenticator;
import com.kiettran.stix.feed.store.InMemoryIndicatorStore;
import com.kiettran.stix.feed.validation.IndicatorValidator;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application entry point and composition root.
 *
 * Without a framework there is no annotation processor or classpath scanner
 * to construct collaborators automatically; this class plays both roles.
 * Every singleton is built and connected here, so the full dependency graph
 * is visible in one method. Compare against
 * {@code stix-feed-boot/FeedApplication.java}, where Spring discovers and
 * wires the same set via {@code @SpringBootApplication}.
 *
 * The advantage of explicit wiring is auditability: a reviewer can trace any
 * request from socket to response without leaving the source tree. The cost
 * is roughly 50 lines of construction code that a framework would absorb.
 */
public final class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private Main() {}

    public static void main(String[] args) throws Exception {

        // ── 1. Load configuration ──────────────────────────────────────────
        AppConfig config = AppConfig.fromEnvironment();

        // ── 2. Construct collaborators ─────────────────────────────────────
        JsonMapper json                  = new JsonMapper();
        IndicatorValidator validator     = new IndicatorValidator();
        InMemoryIndicatorStore store     = new InMemoryIndicatorStore();
        IndicatorPublisher publisher     = new IndicatorPublisher(config.kafka(), json);
        UserAuthenticator authenticator  = UserAuthenticator.withTestUsers();
        JwtTokenIssuer issuer            = new JwtTokenIssuer(config.jwt());
        JwtTokenVerifier verifier        = new JwtTokenVerifier(config.jwt());

        // ── 3. Build router with handlers and filters ──────────────────────
        // Filter order matters: TracingFilter generates the trace id before
        // anything else can fail; ErrorFilter wraps everything downstream so
        // unhandled exceptions become structured 500s; JwtFilter applies only
        // to /api/v1/indicators/** so the token endpoint stays open.
        Router router = Router.builder(json)
            .filter(new TracingFilter())
            .filter(new ErrorFilter(json))
            .filter(new JwtFilter(verifier, json), "/api/v1/indicators/**")

            // OpenAPI docs (no auth)
            .route("GET",  "/docs",
                new StaticResourceHandler("/swagger-ui.html", "text/html; charset=UTF-8", json))
            .route("GET",  "/openapi.yaml",
                new StaticResourceHandler("/openapi.yaml", "application/yaml; charset=UTF-8", json))

            // Auth + health (no JWT)
            .route("POST", "/api/v1/auth/token",
                new AuthTokenHandler(authenticator, issuer, json))
            .route("GET",  "/health",
                new HealthHandler(json))
            .route("GET",  "/ready",
                new ReadyHandler(publisher, store, json))

            // Indicators (JWT-protected)
            .route("POST", "/api/v1/indicators/batch",
                new IndicatorBatchHandler(validator, publisher, store, json))
            .route("POST", "/api/v1/indicators",
                new IndicatorPostHandler(validator, publisher, store, json))
            .route("GET",  "/api/v1/indicators/{id}",
                new IndicatorGetHandler(store, json))
            .route("GET",  "/api/v1/indicators",
                new IndicatorQueryHandler(store, json))

            .build();

        // ── 4. Bring up the HTTP server ────────────────────────────────────
        HttpServer server = HttpServerBootstrap.create(config.server(), router);

        // ── 5. Graceful shutdown ───────────────────────────────────────────
        // The shutdown hook stops accepting new connections, drains in-flight
        // requests up to the configured grace period, and closes the Kafka
        // producer so any pending sends are flushed before the JVM exits.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down (grace={}s)...", config.server().shutdownGrace().toSeconds());
            server.stop((int) config.server().shutdownGrace().toSeconds());
            try { publisher.close(); } catch (Exception ignored) {}
            log.info("Bye.");
        }, "shutdown-hook"));

        server.start();
        log.info("STIX feed listening on http://0.0.0.0:{}", config.server().port());
        log.info("API docs at http://localhost:{}/docs", config.server().port());
    }
}
