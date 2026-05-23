package com.kiettran.stix.feed.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Thin facade over Jackson ObjectMapper. Centralizes configuration so the rest
 * of the application doesn't depend on Jackson directly.
 */
public final class JsonMapper {

    private final ObjectMapper mapper;

    public JsonMapper() {
        this.mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public <T> T read(InputStream in, Class<T> type) throws IOException {
        return mapper.readValue(in, type);
    }

    public <T> T read(byte[] bytes, Class<T> type) throws IOException {
        return mapper.readValue(bytes, type);
    }

    public <T> T read(String json, Class<T> type) throws IOException {
        return mapper.readValue(json, type);
    }

    public byte[] writeBytes(Object value) throws IOException {
        return mapper.writeValueAsBytes(value);
    }

    public String writeString(Object value) throws IOException {
        return mapper.writeValueAsString(value);
    }

    public void write(OutputStream out, Object value) throws IOException {
        mapper.writeValue(out, value);
    }

    public ObjectMapper unwrap() { return mapper; }
}
