package com.kiettran.stix.feed.http;

import com.kiettran.stix.feed.config.ServerConfig;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public final class HttpServerBootstrap {

    private static final Logger log = LoggerFactory.getLogger(HttpServerBootstrap.class);

    private HttpServerBootstrap() {}

    public static HttpServer create(ServerConfig cfg, Router router) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(cfg.port()), cfg.backlog());
        // Single root context; the router does its own path matching.
        server.createContext("/", router);
        server.setExecutor(Executors.newFixedThreadPool(cfg.threadPoolSize()));
        log.info("HttpServer ready: port={} threads={}", cfg.port(), cfg.threadPoolSize());
        return server;
    }
}
