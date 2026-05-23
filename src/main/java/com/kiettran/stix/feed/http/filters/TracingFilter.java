package com.kiettran.stix.feed.http.filters;

import com.kiettran.stix.feed.http.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;

/**
 * Logs request entry/exit and binds the trace id to MDC for the duration
 * of the request. Always the first filter.
 */
public final class TracingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger("access");

    @Override
    public void doFilter(RequestContext ctx, FilterChain chain) throws IOException {
        long start = System.nanoTime();
        MDC.put("traceId", ctx.traceId());
        try {
            log.info("--> {} {}", ctx.method(), ctx.path());
            chain.next(ctx);
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000L;
            int status = ctx.exchange().getResponseCode();
            log.info("<-- {} {} status={} {}ms", ctx.method(), ctx.path(), status, ms);
            MDC.remove("traceId");
        }
    }
}
