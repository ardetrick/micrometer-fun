package com.ardetrick;

public class Example5_NotCovered {

    /**
     * Topics not covered in this project but are worth calling out.
     */
    public static void main(String[] args) {
        // All supported meter types:
        // - https://docs.micrometer.io/micrometer/reference/concepts/meters.html
        // Includes:
        // - Timer
        // - Counter
        // - Gauge
        // - DistributionSummary
        // - LongTaskTimer
        // - FunctionCounter
        // - FunctionTimer
        // - TimeGauge
        // LongTaskTimer is really cool, it is similar to a Timer however it will report incremental times. This means
        // you can see it making progress as the task runs rather than having to wait until the end to see the metric.

        // All these examples use the new prometheus client rather than the simpleclient.
        // - https://docs.micrometer.io/micrometer/reference/implementations/prometheus.html
        // - https://prometheus.github.io/client_java/migration/simpleclient/#migration-using-the-simpleclient-bridge
        // A bridge makes this easier.

        // Additional features of a MeterFilter
        // - https://docs.micrometer.io/micrometer/reference/concepts/meter-filters.html
        // Including:
        // - Deny or Accept Meters - conditionally applying meters
        // - Transforming Metrics, for example renaming

        // Projects instrumented with the Observation API:
        // - https://docs.micrometer.io/micrometer/reference/observation/projects.html
        // Includes:
        // - gRPC
        // - Kafka
        // - JDBC
        // - OkHttp
        // - RabbitMQ

        // Testing!
        // - https://docs.micrometer.io/micrometer/reference/observation/testing.html
        //  A nice API for asserting the Observation is invoked as expected.

        // Documentation Generation.
        // - https://docs.micrometer.io/micrometer-docs-generator/reference/
        // Really neat idea, but also a bit clunky.

        // Custom observation conventions.
        // - https://docs.micrometer.io/micrometer/reference/observation/components.html#micrometer-observation-convention-example
        // Ensures tags/names are used consistently in similar contexts.

        // Context propagation.
        // - https://docs.micrometer.io/micrometer/reference/contextpropagation/purpose.html
        // Unsure how/if this would work with Vertx.

        // Tracing.
        // - https://docs.micrometer.io/tracing/reference/index.html
    }

}
