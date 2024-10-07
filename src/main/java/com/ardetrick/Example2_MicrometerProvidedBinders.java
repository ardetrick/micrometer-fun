package com.ardetrick;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.binder.jvm.*;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class Example2_MicrometerProvidedBinders {

    private static final Logger logger = LoggerFactory.getLogger(Example2_MicrometerProvidedBinders.class);

    /**
     * Exposes metrics provided by micrometer built in binders.
     */
    public static void main(String[] args) {
        PrometheusMeterRegistry prometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        exposeRegistryViaHttp(prometheusMeterRegistry);
        bindMicrometerProvidedMetrics(prometheusMeterRegistry);
    }

    private static void bindMicrometerProvidedMetrics(PrometheusMeterRegistry prometheusMeterRegistry) {
        // Some JVM metrics
        new JvmThreadMetrics().bindTo(prometheusMeterRegistry);
        new JvmCompilationMetrics().bindTo(prometheusMeterRegistry);
        new JvmGcMetrics().bindTo(prometheusMeterRegistry);
        new JvmHeapPressureMetrics().bindTo(prometheusMeterRegistry);
        new JvmInfoMetrics().bindTo(prometheusMeterRegistry);
        new JvmMemoryMetrics().bindTo(prometheusMeterRegistry);
        // Logging
        new LogbackMetrics().bindTo(prometheusMeterRegistry);
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
