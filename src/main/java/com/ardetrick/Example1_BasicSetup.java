package com.ardetrick;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class Example1_BasicSetup {

    private static final Logger logger = LoggerFactory.getLogger(Example1_BasicSetup.class);

    /**
     * Creates a prometheus registry and exposes it over HTTP.
     */
    public static void main(String[] args) {
        PrometheusMeterRegistry prometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        exposeRegistryViaHttp(prometheusMeterRegistry);
    }

    private static void exposeRegistryViaHttp(PrometheusMeterRegistry prometheusMeterRegistry) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/prometheus", httpExchange -> {
                String response = prometheusMeterRegistry.scrape();
                httpExchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = httpExchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            });

            new Thread(server::start).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logger.info("http://localhost:8080/prometheus");
    }

}
