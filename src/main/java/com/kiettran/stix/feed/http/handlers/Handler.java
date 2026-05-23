package com.kiettran.stix.feed.http.handlers;

import com.kiettran.stix.feed.http.RequestContext;

import java.io.IOException;

@FunctionalInterface
public interface Handler {
    void handle(RequestContext ctx) throws IOException;
}
