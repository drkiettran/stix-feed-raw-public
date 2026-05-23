package com.kiettran.stix.feed.http.filters;

import com.kiettran.stix.feed.http.RequestContext;

import java.io.IOException;

@FunctionalInterface
public interface Filter {
    /** Implementations call chain.next(ctx) to continue, or short-circuit by writing a response. */
    void doFilter(RequestContext ctx, FilterChain chain) throws IOException;
}
