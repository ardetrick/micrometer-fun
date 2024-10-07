package com.ardetrick;

import com.sun.net.httpserver.HttpServer;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.health.v1.HealthGrpc.HealthBlockingStub;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionServiceV1;
import io.grpc.services.HealthStatusManager;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.binder.grpc.ObservationGrpcServerInterceptor;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;

public class Example5_GrpcSlo {

    /**
     * https://github.com/micrometer-metrics/micrometer/pull/3427/files#diff-b5293aa56eae60894b17cbf8af93661ce6d1e885be86b240d05222cc500a0788
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        exposeRegistryViaHttp(rsegistry);

        ObservationRegistry observationRegistry = ObservationRegistry.create();
        observationRegistry.observationConfig().observationHandler(new DefaultMeterObservationHandler(registry));

        MeterFilter meterFilter = new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if (!id.getType().equals(Meter.Type.TIMER)) {
                    return config;
                }

                if (!id.getName().startsWith("grpc.server")) {
                    return config;
                }

                return DistributionStatisticConfig.builder()
                        .serviceLevelObjectives(Duration.ofMillis(100).toNanos(),
                                                Duration.ofMillis(500).toNanos(),
                                                Duration.ofSeconds(1).toNanos(),
                                                Duration.ofSeconds(5).toNanos(),
                                                Duration.ofSeconds(30).toNanos(),
                                                Duration.ofSeconds(60).toNanos(),
                                                Duration.ofMinutes(5).toNanos(),
                                                Duration.ofMinutes(10).toNanos())
                        // Enable to dynamically create buckets - algorithm uses min/max configs to set limits.
                        .percentilesHistogram(true)
                        .minimumExpectedValue(Double.valueOf(Duration.ofMillis(100).toNanos()))
                        .maximumExpectedValue(Double.valueOf(Duration.ofMinutes(5).toNanos()))
                        .build()
                        .merge(config);
            }

        };

        registry.config()
                .meterFilter(meterFilter);

        HealthStatusManager service = new HealthStatusManager();

        Server server = InProcessServerBuilder.forName("sample")
                .addService(service.getHealthService())
                .addService(ProtoReflectionServiceV1.newInstance()) // Add reflection service
                .intercept(new ObservationGrpcServerInterceptor(observationRegistry)).build();
        server.start();

        ManagedChannel channel = InProcessChannelBuilder.forName("sample").build();

        // query health check
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
        System.out.println("http://localhost:8080/prometheus");
    }

}
