package com.kiettran.stix.feed.http;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteTest {

    @Test
    void staticRouteMatches() {
        Route r = Route.of("GET", "/health");
        assertTrue(r.matches("GET", "/health"));
    }

    @Test
    void parameterizedRouteCapturesGroup() {
        Route r = Route.of("GET", "/api/v1/indicators/{id}");
        Matcher m = r.matcher("/api/v1/indicators/indicator--8e2e2d2b-17d4-4cbf-938f-98ee46b3cd3f");
        assertNotNull(m);
        assertEquals("indicator--8e2e2d2b-17d4-4cbf-938f-98ee46b3cd3f", m.group("id"));
    }

    @Test
    void parameterDoesNotMatchAcrossSlashes() {
        Route r = Route.of("GET", "/api/v1/indicators/{id}");
        Matcher m = r.matcher("/api/v1/indicators/abc/def");
        assertNull(m);
    }

    @Test
    void methodCaseInsensitive() {
        Route r = Route.of("post", "/api/v1/indicators");
        assertTrue(r.matches("POST", "/api/v1/indicators"));
    }
}
