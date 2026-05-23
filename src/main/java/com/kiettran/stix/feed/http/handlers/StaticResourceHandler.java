package com.kiettran.stix.feed.http.handlers;

import com.kiettran.stix.feed.error.ErrorResponse;
import com.kiettran.stix.feed.error.ErrorType;
import com.kiettran.stix.feed.http.RequestContext;
import com.kiettran.stix.feed.http.ResponseWriter;
import com.kiettran.stix.feed.json.JsonMapper;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Serves a single classpath resource at a fixed route. Used for `/openapi.yaml`
 * and `/docs`. Kept deliberately minimal — no directory listing, no path
 * traversal, no caching headers — because the surface area is small and known.
 */
public final class StaticResourceHandler implements Handler {

    private static final Logger log = LoggerFactory.getLogger(StaticResourceHandler.class);

    private final String resourcePath;
    private final String contentType;
    private final ResponseWriter writer;

    public StaticResourceHandler(String resourcePath, String contentType, JsonMapper json) {
        this.resourcePath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        this.contentType = contentType;
        this.writer = new ResponseWriter(json);
    }

    @Override
    public void handle(RequestContext ctx) throws IOException {
        HttpExchange ex = ctx.exchange();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                log.warn("Static resource not found on classpath: {}", resourcePath);
                writer.writeJson(ex, 404,
                    ErrorResponse.of(ErrorType.NOT_FOUND, "Resource not found", ctx.traceId()),
                    ctx.traceId());
                return;
            }
            byte[] bytes = in.readAllBytes();
            ex.getResponseHeaders().set("Content-Type", contentType);
            if (ctx.traceId() != null) ex.getResponseHeaders().set("X-Trace-Id", ctx.traceId());
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
        }
    }
}
