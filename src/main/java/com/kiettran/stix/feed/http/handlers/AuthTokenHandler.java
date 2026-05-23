package com.kiettran.stix.feed.http.handlers;

import com.kiettran.stix.feed.error.ErrorResponse;
import com.kiettran.stix.feed.error.ErrorType;
import com.kiettran.stix.feed.http.RequestContext;
import com.kiettran.stix.feed.http.ResponseWriter;
import com.kiettran.stix.feed.json.JsonMapper;
import com.kiettran.stix.feed.security.JwtTokenIssuer;
import com.kiettran.stix.feed.security.UserAuthenticator;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class AuthTokenHandler implements Handler {

    public record TokenRequest(String username, String password) {}

    private static final int MAX_BODY = 4 * 1024;

    private final UserAuthenticator authenticator;
    private final JwtTokenIssuer issuer;
    private final JsonMapper json;
    private final ResponseWriter writer;

    public AuthTokenHandler(UserAuthenticator authenticator, JwtTokenIssuer issuer, JsonMapper json) {
        this.authenticator = authenticator;
        this.issuer = issuer;
        this.json = json;
        this.writer = new ResponseWriter(json);
    }

    @Override
    public void handle(RequestContext ctx) throws IOException {
        TokenRequest req;
        try {
            byte[] body = ctx.readAllBytes(MAX_BODY);
            req = json.read(body, TokenRequest.class);
        } catch (Exception e) {
            writer.writeJson(ctx.exchange(), 400,
                ErrorResponse.of(ErrorType.BAD_REQUEST, "Malformed JSON body", ctx.traceId()),
                ctx.traceId());
            return;
        }
        if (req == null || req.username() == null || req.password() == null) {
            writer.writeJson(ctx.exchange(), 400,
                ErrorResponse.of(ErrorType.BAD_REQUEST, "username and password are required", ctx.traceId()),
                ctx.traceId());
            return;
        }
        Optional<UserAuthenticator.User> user = authenticator.authenticate(req.username(), req.password());
        if (user.isEmpty()) {
            writer.writeJson(ctx.exchange(), 401,
                ErrorResponse.of(ErrorType.UNAUTHORIZED, "Invalid credentials", ctx.traceId()),
                ctx.traceId());
            return;
        }
        String token = issuer.issue(user.get().username(), user.get().roles());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("access_token", token);
        body.put("token_type", "Bearer");
        body.put("expires_in", issuer.ttlSeconds());
        writer.writeJson(ctx.exchange(), 200, body, ctx.traceId());
    }
}
