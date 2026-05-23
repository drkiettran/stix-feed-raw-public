package com.kiettran.stix.feed.http;

import com.kiettran.stix.feed.json.JsonMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class ResponseWriter {

    private final JsonMapper json;

    public ResponseWriter(JsonMapper json) { this.json = json; }

    public void writeJson(HttpExchange ex, int status, Object body, String traceId) throws IOException {
        byte[] bytes = body == null ? new byte[0] : json.writeBytes(body);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        if (traceId != null) ex.getResponseHeaders().set("X-Trace-Id", traceId);
        ex.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
        }
    }

    public void writeText(HttpExchange ex, int status, String text, String traceId) throws IOException {
        byte[] bytes = text == null ? new byte[0] : text.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        if (traceId != null) ex.getResponseHeaders().set("X-Trace-Id", traceId);
        ex.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
        }
    }
}
