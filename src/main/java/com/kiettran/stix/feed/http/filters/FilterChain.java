package com.kiettran.stix.feed.http.filters;

import com.kiettran.stix.feed.http.RequestContext;

import java.io.IOException;
import java.util.List;

public final class FilterChain {

    private final List<Filter> filters;
    private final TerminalHandler terminal;
    private int index = 0;

    public interface TerminalHandler {
        void handle(RequestContext ctx) throws IOException;
    }

    public FilterChain(List<Filter> filters, TerminalHandler terminal) {
        this.filters = filters;
        this.terminal = terminal;
    }

    public void next(RequestContext ctx) throws IOException {
        if (index < filters.size()) {
            Filter f = filters.get(index++);
            f.doFilter(ctx, this);
        } else {
            terminal.handle(ctx);
        }
    }
}
