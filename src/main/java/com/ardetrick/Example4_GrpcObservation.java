package com.ardetrick;

import com.sun.net.httpserver.HttpServer;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.health.v1.HealthGrpc.HealthBlockingStub;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.services.HealthStatusManager;
import io.micrometer.core.instrument.binder.grpc.ObservationGrpcClientInterceptor;
import io.micrometer.core.instrument.binder.grpc.ObservationGrpcServerInterceptor;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class Example4_GrpcObservation {

    private static final Logger logger = LoggerFactory.getLogger(Example4_GrpcObservation.class);

    /**
     * Shows how gRPC server operations can be instrumented and observed.
     * <p>
     * https://github.com/micrometer-metrics/micrometer/pull/3427/files#diff-b5293aa56eae60894b17cbf8af93661ce6d1e885be86b240d05222cc500a0788
     */
    public static void main(String[] args) throws IOException {
        PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        exposeRegistryViaHttp(prometheusRegistry);

        ObservationRegistry observationRegistry = ObservationRegistry.create();
        observationRegistry.observationConfig()
                .observationHandler(new DefaultMeterObservationHandler(prometheusRegistry));

        HealthStatusManager service = new HealthStatusManager();

        Server server = InProcessServerBuilder.forName("sample")
                .addService(service.getHealthService())
                .intercept(new ObservationGrpcServerInterceptor(observationRegistry))
                .build();
        server.start();

        ManagedChannel channel = InProcessChannelBuilder.forName("sample")
                .intercept(new ObservationGrpcClientInterceptor(observationRegistry))
                .build();

        HealthBlockingStub healthClient = HealthGrpc.newBlockingStub(channel);

        HealthCheckRequest validRequest = HealthCheckRequest.getDefaultInstance();
        HealthCheckRequest notFoundRequest = HealthCheckRequest.getDefaultInstance()
                .toBuilder()
                .setService("foo")
                .build();

        healthClient.check(validRequest);
        healthClient.check(notFoundRequest);

        channel.shutdownNow();
        server.shutdownNow();
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