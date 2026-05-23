package com.kiettran.stix.feed.http;

import com.kiettran.stix.feed.error.ErrorResponse;
import com.kiettran.stix.feed.error.ErrorType;
import com.kiettran.stix.feed.http.filters.Filter;
import com.kiettran.stix.feed.http.filters.FilterChain;
import com.kiettran.stix.feed.http.handlers.Handler;
import com.kiettran.stix.feed.json.JsonMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;

/**
 * HTTP dispatch by method plus compiled-regex path matching.
 *
 * Routes and filters are registered explicitly through the builder; nothing
 * is discovered at runtime. The advantage over annotation-driven routing is
 * traceability — every dispatch path is visible in the registration code in
 * {@code Main} and matchable to a single {@link Handler} without reflection.
 *
 * The router also serves as the last line of defense for unhandled errors:
 * an I/O failure or unexpected exception during dispatch is caught here and
 * mapped to a 500 with a trace id, ensuring no request ever silently drops.
 * 404 (no route) and 405 (method mismatch on a known path) are distinguished
 * so clients can react appropriately.
 */
public final class Router implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(Router.class);

    private final List<RouteBinding> bindings;
    private final List<FilterBinding> filters;
    private final ResponseWriter writer;

    private record RouteBinding(Route route, Handler handler) {}
    private record FilterBinding(Filter filter, String pathPrefix) {
        boolean appliesTo(String path) {
            return pathPrefix == null || matchesPrefix(pathPrefix, path);
        }
    }

    private static boolean matchesPrefix(String pattern, String path) {
        if (pattern.endsWith("/**")) {
            String base = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(base);
        }
        return path.equals(pattern);
    }

    private Router(List<RouteBinding> bindings, List<FilterBinding> filters, ResponseWriter writer) {
        this.bindings = bindings;
        this.filters = filters;
        this.writer = writer;
    }

    @Override
    public void handle(HttpExchange ex) {
        String traceId = UUID.randomUUID().toString();
        try {
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();

            RouteBinding match = null;
            Map<String, String> pathParams = Map.of();

            for (RouteBinding b : bindings) {
                Matcher m = b.route().matcher(path);
                if (m != null && b.route().method().equalsIgnoreCase(method)) {
                    match = b;
                    pathParams = extractParams(b.route(), m);
                    break;
                }
            }

            if (match == null) {
                if (anyPathMatch(path)) {
                    writer.writeJson(ex, 405,
                        ErrorResponse.of(ErrorType.METHOD_NOT_ALLOWED,
                            "Method " + method + " not allowed on " + path, traceId), traceId);
                } else {
                    writer.writeJson(ex, 404,
                        ErrorResponse.of(ErrorType.NOT_FOUND,
                            "No route for " + method + " " + path, traceId), traceId);
                }
                return;
            }

            RequestContext ctx = new RequestContext(ex, pathParams, traceId);
            List<Filter> applicable = new ArrayList<>();
            for (FilterBinding fb : filters) if (fb.appliesTo(path)) applicable.add(fb.filter());

            Handler h = match.handler();
            FilterChain chain = new FilterChain(applicable, h::handle);
            chain.next(ctx);

        } catch (IOException io) {
            log.error("I/O failure handling {} {}: {}", ex.getRequestMethod(), ex.getRequestURI(), io.toString());
            try {
                writer.writeJson(ex, 500,
                    ErrorResponse.of(ErrorType.INTERNAL_ERROR, "I/O failure", traceId), traceId);
            } catch (IOException ignored) { /* connection already gone */ }
        } catch (Exception other) {
            log.error("Unhandled error in router for {} {}", ex.getRequestMethod(), ex.getRequestURI(), other);
            try {
                writer.writeJson(ex, 500,
                    ErrorResponse.of(ErrorType.INTERNAL_ERROR, "Unexpected error", traceId), traceId);
            } catch (IOException ignored) { }
        } finally {
            ex.close();
        }
    }

    private boolean anyPathMatch(String path) {
        for (RouteBinding b : bindings) {
            if (b.route().compiled().matcher(path).matches()) return true;
        }
        return false;
    }

    private static Map<String, String> extractParams(Route route, Matcher matcher) {
        Map<String, String> out = new HashMap<>();
        // The compiled regex doesn't expose its declared group names cheaply,
        // so the names are recovered from the original path template instead.
        String tpl = route.pathTemplate();
        java.util.regex.Matcher pm = java.util.regex.Pattern
            .compile("\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}").matcher(tpl);
        while (pm.find()) {
            String name = pm.group(1);
            try { out.put(name, matcher.group(name)); }
            catch (IllegalArgumentException ignored) { /* group not present in regex */ }
        }
        return out;
    }

    public static Builder builder(JsonMapper json) {
        return new Builder(new ResponseWriter(json));
    }

    public static final class Builder {
        private final List<RouteBinding> bindings = new ArrayList<>();
        private final List<FilterBinding> filters = new ArrayList<>();
        private final ResponseWriter writer;

        Builder(ResponseWriter writer) { this.writer = writer; }

        public Builder filter(Filter f) {
            filters.add(new FilterBinding(f, null));
            return this;
        }

        public Builder filter(Filter f, String pathPrefix) {
            filters.add(new FilterBinding(f, pathPrefix));
            return this;
        }

        public Builder route(String method, String pathTemplate, Handler handler) {
            bindings.add(new RouteBinding(Route.of(method, pathTemplate), handler));
            return this;
        }

        public Router build() {
            return new Router(List.copyOf(bindings), List.copyOf(filters), writer);
        }

        public ResponseWriter writer() { return writer; }
    }
}
