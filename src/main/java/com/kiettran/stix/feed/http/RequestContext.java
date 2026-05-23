package com.kiettran.stix.feed.http;

import com.kiettran.stix.feed.security.JwtTokenVerifier.Principal;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Per-request state passed through filters and into handlers. */
public final class RequestContext {

    private final HttpExchange exchange;
    private final Map<String, String> pathParams;
    private final Map<String, List<String>> queryParams;
    private final String traceId;
    private Principal principal;

    public RequestContext(HttpExchange exchange, Map<String, String> pathParams, String traceId) {
        this.exchange = exchange;
        this.pathParams = pathParams == null ? Map.of() : Map.copyOf(pathParams);
        this.queryParams = parseQuery(exchange.getRequestURI().getRawQuery());
        this.traceId = traceId;
    }

    public HttpExchange exchange() { return exchange; }
    public String method() { return exchange.getRequestMethod().toUpperCase(Locale.ROOT); }
    public String path() { return exchange.getRequestURI().getPath(); }
    public Headers requestHeaders() { return exchange.getRequestHeaders(); }
    public InputStream requestBody() { return exchange.getRequestBody(); }
    public String pathParam(String name) { return pathParams.get(name); }
    public Map<String, List<String>> queryParams() { return queryParams; }
    public List<String> queryParamValues(String name) { return queryParams.getOrDefault(name, List.of()); }
    public String queryParam(String name) {
        List<String> v = queryParams.get(name);
        return v == null || v.isEmpty() ? null : v.get(0);
    }
    public String traceId() { return traceId; }
    public Principal principal() { return principal; }
    public void setPrincipal(Principal p) { this.principal = p; }

    public byte[] readAllBytes(int maxBytes) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            return in.readNBytes(maxBytes);
        }
    }

    private static Map<String, List<String>> parseQuery(String raw) {
        Map<String, List<String>> out = new HashMap<>();
        if (raw == null || raw.isEmpty()) return out;
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            String k = eq < 0 ? pair : pair.substring(0, eq);
            String v = eq < 0 ? "" : pair.substring(eq + 1);
            String dk = URLDecoder.decode(k, StandardCharsets.UTF_8);
            String dv = URLDecoder.decode(v, StandardCharsets.UTF_8);
            out.computeIfAbsent(dk, x -> new ArrayList<>()).add(dv);
        }
        return out;
    }

    @Override public String toString() {
        return method() + " " + path() + " trace=" + traceId
             + " principal=" + Objects.toString(principal, "anonymous");
    }
}
