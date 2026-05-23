package com.kiettran.stix.feed.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.kiettran.stix.feed.config.JwtConfig;
import com.kiettran.stix.feed.config.KafkaConfig;
import com.kiettran.stix.feed.config.ServerConfig;
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
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end test class. Boots a real HttpServer on a random port with a
 * MockProducer for Kafka, then exercises every route via java.net.http.HttpClient.
 *
 * Pulls coverage of the entire HTTP stack (handlers + filters + router + static
 * resources) up from 0% by going through real wire-protocol paths. Same logic as
 * scripts/smoke.sh, but in JUnit so JaCoCo can see it.
 */
class ServerIntegrationTest {

    private static HttpServer server;
    private static String baseUrl;
    private static HttpClient client;
    private static JsonMapper json;
    private static String analystToken;
    private static String readerToken;

    @BeforeAll
    static void startServer() throws Exception {
        json = new JsonMapper();

        IndicatorValidator validator = new IndicatorValidator();
        InMemoryIndicatorStore store = new InMemoryIndicatorStore();

        KafkaConfig kafkaCfg = new KafkaConfig("mock://test", "stix.indicators.test", 5000, 10000);
        MockProducer<String, byte[]> mock = new MockProducer<>(true,
            new StringSerializer(), new ByteArraySerializer());
        IndicatorPublisher publisher = new IndicatorPublisher(kafkaCfg, json, mock);

        UserAuthenticator authenticator = UserAuthenticator.withTestUsers();
        JwtConfig jwtCfg = new JwtConfig(
            "integration-test-secret-32-bytes-min!", Duration.ofMinutes(5), "stix-feed-test"
        );
        JwtTokenIssuer issuer = new JwtTokenIssuer(jwtCfg);
        JwtTokenVerifier verifier = new JwtTokenVerifier(jwtCfg);

        Router router = Router.builder(json)
            .filter(new TracingFilter())
            .filter(new ErrorFilter(json))
            .filter(new JwtFilter(verifier, json), "/api/v1/indicators/**")

            .route("GET",  "/docs",          new StaticResourceHandler("/swagger-ui.html", "text/html; charset=UTF-8", json))
            .route("GET",  "/openapi.yaml",  new StaticResourceHandler("/openapi.yaml",   "application/yaml; charset=UTF-8", json))

            .route("POST", "/api/v1/auth/token",
                new AuthTokenHandler(authenticator, issuer, json))
            .route("GET",  "/health",        new HealthHandler(json))
            .route("GET",  "/ready",         new ReadyHandler(publisher, store, json))

            .route("POST", "/api/v1/indicators/batch",
                new IndicatorBatchHandler(validator, publisher, store, json))
            .route("POST", "/api/v1/indicators",
                new IndicatorPostHandler(validator, publisher, store, json))
            .route("GET",  "/api/v1/indicators/{id}",
                new IndicatorGetHandler(store, json))
            .route("GET",  "/api/v1/indicators",
                new IndicatorQueryHandler(store, json))
            .build();

        // Port 0 → OS picks a free one
        ServerConfig srvCfg = new ServerConfig(0, 4, 0, Duration.ofSeconds(1));
        server = HttpServerBootstrap.create(srvCfg, router);
        server.start();

        int port = server.getAddress().getPort();
        baseUrl = "http://localhost:" + port;
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

        analystToken = fetchToken("analyst", "analyst-pass");
        readerToken  = fetchToken("reader",  "reader-pass");
    }

    @AfterAll
    static void stopServer() {
        if (server != null) server.stop(0);
    }

    // ────────────────────────── helpers ──────────────────────────

    private static HttpResponse<String> get(String path) throws Exception {
        return client.send(
            HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    private static HttpResponse<String> getAuth(String path, String token) throws Exception {
        return client.send(
            HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Authorization", "Bearer " + token)
                .GET().build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    private static HttpResponse<String> postJson(String path, String body, String token) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl + path))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body));
        if (token != null) b.header("Authorization", "Bearer " + token);
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String fetchToken(String user, String pass) throws Exception {
        HttpResponse<String> r = postJson("/api/v1/auth/token",
            "{\"username\":\"" + user + "\",\"password\":\"" + pass + "\"}", null);
        if (r.statusCode() != 200) throw new IllegalStateException("token fetch failed: " + r.statusCode() + " " + r.body());
        return json.unwrap().readTree(r.body()).get("access_token").asText();
    }

    private static String validIndicatorJson(String id) {
        String now = OffsetDateTime.now().toString();
        return """
            {
              "type": "indicator",
              "spec_version": "2.1",
              "id": "%s",
              "created": "%s",
              "modified": "%s",
              "name": "integration-test indicator",
              "indicator_types": ["malicious-activity"],
              "pattern": "[ipv4-addr:value = '203.0.113.1']",
              "pattern_type": "stix",
              "valid_from": "%s",
              "labels": ["integration"],
              "confidence": 80
            }
            """.formatted(id, now, now, now);
    }

    private static String freshId() {
        return "indicator--" + UUID.randomUUID();
    }

    // ────────────────────────── tests ──────────────────────────

    @Test
    @DisplayName("GET /health returns 200 with status UP")
    void healthOk() throws Exception {
        HttpResponse<String> r = get("/health");
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("\"status\":\"UP\""));
    }

    @Test
    @DisplayName("GET /ready returns 200 when Kafka and store are healthy")
    void readyOk() throws Exception {
        HttpResponse<String> r = get("/ready");
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("\"status\":\"READY\""));
    }

    @Test
    @DisplayName("POST /api/v1/auth/token returns access_token for valid credentials")
    void authValid() throws Exception {
        HttpResponse<String> r = postJson("/api/v1/auth/token",
            "{\"username\":\"analyst\",\"password\":\"analyst-pass\"}", null);
        assertEquals(200, r.statusCode());
        JsonNode body = json.unwrap().readTree(r.body());
        assertNotNull(body.get("access_token"));
        assertEquals("Bearer", body.get("token_type").asText());
    }

    @Test
    @DisplayName("POST /api/v1/auth/token returns 401 for invalid credentials")
    void authInvalid() throws Exception {
        HttpResponse<String> r = postJson("/api/v1/auth/token",
            "{\"username\":\"analyst\",\"password\":\"WRONG\"}", null);
        assertEquals(401, r.statusCode());
    }

    @Test
    @DisplayName("POST /api/v1/auth/token returns 400 for missing fields")
    void authMissingFields() throws Exception {
        HttpResponse<String> r = postJson("/api/v1/auth/token",
            "{\"username\":\"analyst\"}", null);
        assertEquals(400, r.statusCode());
    }

    @Test
    @DisplayName("POST /api/v1/auth/token returns 400 for malformed JSON")
    void authMalformed() throws Exception {
        HttpResponse<String> r = postJson("/api/v1/auth/token", "not json", null);
        assertEquals(400, r.statusCode());
    }

    @Test
    @DisplayName("POST /api/v1/indicators without token returns 401")
    void postIndicatorNoToken() throws Exception {
        HttpResponse<String> r = postJson("/api/v1/indicators",
            validIndicatorJson(freshId()), null);
        assertEquals(401, r.statusCode());
    }

    @Test
    @DisplayName("POST /api/v1/indicators with invalid token returns 401")
    void postIndicatorBadToken() throws Exception {
        HttpResponse<String> r = postJson("/api/v1/indicators",
            validIndicatorJson(freshId()), "not.a.real.token");
        assertEquals(401, r.statusCode());
    }

    @Test
    @DisplayName("POST /api/v1/indicators as reader (no analyst role) returns 403")
    void postIndicatorWrongRole() throws Exception {
        HttpResponse<String> r = postJson("/api/v1/indicators",
            validIndicatorJson(freshId()), readerToken);
        assertEquals(403, r.statusCode());
    }

    @Test
    @DisplayName("POST /api/v1/indicators with valid body returns 201")
    void postIndicatorCreated() throws Exception {
        String id = freshId();
        HttpResponse<String> r = postJson("/api/v1/indicators",
            validIndicatorJson(id), analystToken);
        assertEquals(201, r.statusCode(), "body=" + r.body());
        assertTrue(r.body().contains(id));
    }

    @Test
    @DisplayName("POST /api/v1/indicators with duplicate id returns 409")
    void postIndicatorConflict() throws Exception {
        String id = freshId();
        HttpResponse<String> first  = postJson("/api/v1/indicators", validIndicatorJson(id), analystToken);
        HttpResponse<String> second = postJson("/api/v1/indicators", validIndicatorJson(id), analystToken);
        assertEquals(201, first.statusCode());
        assertEquals(409, second.statusCode());
    }

    @Test
    @DisplayName("POST /api/v1/indicators with invalid body returns 400 with details")
    void postIndicatorValidationError() throws Exception {
        String invalidBody = """
            {
              "type": "malware",
              "spec_version": "2.0",
              "id": "indicator--bad",
              "pattern": "",
              "pattern_type": "stix",
              "confidence": 200
            }
            """;
        HttpResponse<String> r = postJson("/api/v1/indicators", invalidBody, analystToken);
        assertEquals(400, r.statusCode());
        JsonNode body = json.unwrap().readTree(r.body());
        assertEquals("validation_failed", body.get("error").asText());
        assertTrue(body.get("details").isArray());
        assertTrue(body.get("details").size() > 3, "should have multiple validation issues");
    }

    @Test
    @DisplayName("POST /api/v1/indicators with unknown pattern_type returns 400 bad_request")
    void postIndicatorUnknownEnum() throws Exception {
        String body = """
            {
              "type": "indicator",
              "spec_version": "2.1",
              "id": "indicator--8e2e2d2b-17d4-4cbf-938f-98ee46b3cd3f",
              "created": "2026-05-05T14:00:00.000Z",
              "modified": "2026-05-05T14:00:00.000Z",
              "pattern": "[ipv4-addr:value = '1.2.3.4']",
              "pattern_type": "fake",
              "valid_from": "2026-05-05T14:00:00.000Z"
            }
            """;
        HttpResponse<String> r = postJson("/api/v1/indicators", body, analystToken);
        assertEquals(400, r.statusCode());
        JsonNode root = json.unwrap().readTree(r.body());
        assertEquals("bad_request", root.get("error").asText());
    }
    
    @Test
    @DisplayName("POST /api/v1/indicators/batch returns 201 when all items valid")
    void postBatchAllValid() throws Exception {
        String body = "[" + validIndicatorJson(freshId()) + "," + validIndicatorJson(freshId()) + "]";
        HttpResponse<String> r = postJson("/api/v1/indicators/batch", body, analystToken);
        assertEquals(201, r.statusCode(), "body=" + r.body());
        JsonNode root = json.unwrap().readTree(r.body());
        assertEquals(2, root.get("total").asInt());
        assertEquals(2, root.get("accepted").asInt());
        assertEquals(0, root.get("rejected").asInt());
    }

    @Test
    @DisplayName("POST /api/v1/indicators/batch returns 207 for partial success")
    void postBatchPartial() throws Exception {
        String good = validIndicatorJson(freshId());
        String bad  = validIndicatorJson("indicator--malformed");   // bad UUID format
        String body = "[" + good + "," + bad + "]";
        HttpResponse<String> r = postJson("/api/v1/indicators/batch", body, analystToken);
        assertEquals(207, r.statusCode(), "body=" + r.body());
        JsonNode root = json.unwrap().readTree(r.body());
        assertEquals(1, root.get("accepted").asInt());
        assertEquals(1, root.get("rejected").asInt());
    }

    @Test
    @DisplayName("GET /api/v1/indicators/{id} returns 200 for stored indicator")
    void getByIdFound() throws Exception {
        String id = freshId();
        postJson("/api/v1/indicators", validIndicatorJson(id), analystToken);
        HttpResponse<String> r = getAuth("/api/v1/indicators/" + id, analystToken);
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains(id));
    }

    @Test
    @DisplayName("GET /api/v1/indicators/{id} returns 404 for unknown id")
    void getByIdNotFound() throws Exception {
        HttpResponse<String> r = getAuth(
            "/api/v1/indicators/indicator--00000000-0000-0000-0000-000000000000", analystToken);
        assertEquals(404, r.statusCode());
    }

    @Test
    @DisplayName("GET /api/v1/indicators/{id} returns 400 for malformed id")
    void getByIdMalformed() throws Exception {
        HttpResponse<String> r = getAuth("/api/v1/indicators/not-a-stix-id", analystToken);
        assertEquals(400, r.statusCode());
    }

    @Test
    @DisplayName("GET /api/v1/indicators returns 200 with paginated results")
    void queryReturnsResults() throws Exception {
        // Ensure at least one is present
        postJson("/api/v1/indicators", validIndicatorJson(freshId()), analystToken);

        HttpResponse<String> r = getAuth("/api/v1/indicators?limit=10&offset=0", analystToken);
        assertEquals(200, r.statusCode());
        JsonNode root = json.unwrap().readTree(r.body());
        assertTrue(root.get("total").asInt() >= 1);
        assertTrue(root.has("items") && root.get("items").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/indicators with bad pattern_type returns 400")
    void queryBadPatternType() throws Exception {
        HttpResponse<String> r = getAuth("/api/v1/indicators?pattern_type=banana", analystToken);
        assertEquals(400, r.statusCode());
    }

    @Test
    @DisplayName("GET /api/v1/indicators with non-numeric confidence_gte returns 400")
    void queryBadConfidence() throws Exception {
        HttpResponse<String> r = getAuth("/api/v1/indicators?confidence_gte=abc", analystToken);
        assertEquals(400, r.statusCode());
    }

    @Test
    @DisplayName("GET /openapi.yaml returns 200 with YAML content type")
    void openapiYamlServed() throws Exception {
        HttpResponse<String> r = get("/openapi.yaml");
        assertEquals(200, r.statusCode());
        assertTrue(r.headers().firstValue("Content-Type").orElse("").contains("yaml"));
        assertTrue(r.body().startsWith("openapi:") || r.body().contains("openapi: 3.0"));
    }

    @Test
    @DisplayName("GET /docs returns 200 with HTML")
    void docsPageServed() throws Exception {
        HttpResponse<String> r = get("/docs");
        assertEquals(200, r.statusCode());
        assertTrue(r.headers().firstValue("Content-Type").orElse("").contains("html"));
        assertTrue(r.body().contains("swagger-ui"));
    }

    @Test
    @DisplayName("Unknown path returns 404 with structured error body")
    void unknownPath404() throws Exception {
        HttpResponse<String> r = get("/no-such-route");
        assertEquals(404, r.statusCode());
        JsonNode root = json.unwrap().readTree(r.body());
        assertEquals("not_found", root.get("error").asText());
    }

    @Test
    @DisplayName("Wrong method on known path returns 405")
    void methodNotAllowed405() throws Exception {
        // /health only supports GET; sending DELETE should hit method-not-allowed branch
        HttpResponse<String> r = client.send(
            HttpRequest.newBuilder(URI.create(baseUrl + "/health"))
                .method("DELETE", HttpRequest.BodyPublishers.noBody()).build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(405, r.statusCode());
    }

    @Test
    @DisplayName("Trace id is returned in X-Trace-Id header on every response")
    void traceIdHeaderPresent() throws Exception {
        HttpResponse<String> r = get("/health");
        assertTrue(r.headers().firstValue("X-Trace-Id").isPresent());
        String traceId = r.headers().firstValue("X-Trace-Id").orElse("");
        assertFalse(traceId.isBlank());
    }
}
