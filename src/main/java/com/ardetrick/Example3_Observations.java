package com.ardetrick;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class Example3_Observations {

    private static final Logger logger = LoggerFactory.getLogger(Example3_Observations.class);

    /**
     * Explains the basics of the Observation API.
     *
     * Full docs: https://docs.micrometer.io/micrometer/reference/observation/introduction.html
     */
    public static void main(String[] args) {
        PrometheusMeterRegistry prometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        exposeRegistryViaHttp(prometheusMeterRegistry);

        ObservationRegistry observationRegistry = ObservationRegistry.create();
        observationRegistry.observationConfig()
                .observationHandler(new LoggingObservationHandler())
                .observationHandler(new DefaultMeterObservationHandler(prometheusMeterRegistry));

        Observation.createNotStarted("some-operation", observationRegistry)
                // tagging as low/high lets handles treat them differently.
                // Handler using a MeterRegistry may not include high cardinality keys in metrics.
                // However, a logging observation handler may want to include all keys.
                .lowCardinalityKeyValue("lowTag", "lowTagValue")
                .highCardinalityKeyValue("highTag", "highTagValue")
                .observe(() -> {});

        Observation.createNotStarted("erroring-operation", observationRegistry)
                .start()
                .error(new RuntimeException())
                .stop();

        Observation.start("some-operation", observationRegistry)
                // Can also signal arbitrary events between start/stop operations.
                // For example, this could be used to indicate incremental progress.
                // The observer could then consider if that incremental progress is worth acting on or not.
                .event(Observation.Event.of("my.event", "look what happened"))
                .stop();
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

    public static class LoggingObservationHandler implements ObservationHandler<Observation.Context> {

        @Override
        public void onStart(Observation.Context context) {
            logger.info("START: " + context);
        }

        @Override
        public void onError(Observation.Context context) {
            logger.info("ERROR: " + context);
        }

        @Override
        public void onEvent(Observation.Event event, Observation.Context context) {
            logger.info("EVENT " + "event: " + event + " context: " + context);
        }

        @Override
        public void onStop(Observation.Context context) {
            logger.info("STOP: " + context);
        }

        @Override
        public boolean supportsContext(Observation.Context handlerContext) {
            // All contexts are supported.
            return true;
        }

    }

}
