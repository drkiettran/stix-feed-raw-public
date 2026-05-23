package com.kiettran.stix.feed.http.filters;

import com.kiettran.stix.feed.error.ErrorResponse;
import com.kiettran.stix.feed.error.ErrorType;
import com.kiettran.stix.feed.http.RequestContext;
import com.kiettran.stix.feed.http.ResponseWriter;
import com.kiettran.stix.feed.json.JsonMapper;
import com.kiettran.stix.feed.security.JwtTokenVerifier;

import java.io.IOException;
import java.util.Optional;

/**
 * Authenticates requests by verifying a Bearer token in the
 * {@code Authorization} header. Applied selectively to protected paths
 * (registered in {@code Main} as {@code /api/v1/indicators/**}); other
 * routes pass through without inspection.
 *
 * The filter short-circuits with a 401 if the header is missing,
 * malformed, or carries a token that fails verification — the chain
 * never advances and downstream handlers never run. On successful
 * verification, the resulting {@link
 * com.kiettran.stix.feed.security.JwtTokenVerifier.Principal Principal}
 * is attached to the {@link RequestContext} so handlers can perform
 * role-based authorization without re-parsing the token.
 *
 * Note the deliberate split: <i>authentication</i> (token valid?) is
 * here; <i>authorization</i> (role sufficient for this endpoint?) is
 * inside each handler. Different endpoints require different roles, so
 * a single role check at the filter level would not generalize.
 */
public final class JwtFilter implements Filter {

    private final JwtTokenVerifier verifier;
    private final ResponseWriter writer;

    public JwtFilter(JwtTokenVerifier verifier, JsonMapper json) {
        this.verifier = verifier;
        this.writer = new ResponseWriter(json);
    }

    @Override
    public void doFilter(RequestContext ctx, FilterChain chain) throws IOException {
        String auth = ctx.requestHeaders().getFirst("Authorization");
        if (auth == null || !auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            writer.writeJson(ctx.exchange(), 401,
                ErrorResponse.of(ErrorType.UNAUTHORIZED, "Missing Bearer token", ctx.traceId()),
                ctx.traceId());
            return;
        }
        String token = auth.substring(7).trim();
        Optional<JwtTokenVerifier.Principal> principal = verifier.verify(token);
        if (principal.isEmpty()) {
            writer.writeJson(ctx.exchange(), 401,
                ErrorResponse.of(ErrorType.UNAUTHORIZED, "Invalid or expired token", ctx.traceId()),
                ctx.traceId());
            return;
        }
        ctx.setPrincipal(principal.get());
        chain.next(ctx);
    }
}
