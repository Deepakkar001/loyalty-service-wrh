package com.loyaltyos.integration.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class IntegrationMetricsService {

    private final MeterRegistry registry;

    public IntegrationMetricsService(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordRequest(String tenantId, String endpoint, int httpStatus, long durationMs) {
        Counter.builder("integration.api.requests")
            .tag("tenant", safeTag(tenantId))
            .tag("endpoint", safeTag(endpoint))
            .tag("status", String.valueOf(httpStatus))
            .register(registry)
            .increment();

        Timer.builder("integration.api.processing.time")
            .tag("tenant", safeTag(tenantId))
            .tag("endpoint", safeTag(endpoint))
            .register(registry)
            .record(durationMs, TimeUnit.MILLISECONDS);

        if (httpStatus >= 400) {
            Counter.builder("integration.api.errors")
                .tag("tenant", safeTag(tenantId))
                .tag("status", String.valueOf(httpStatus))
                .register(registry)
                .increment();
        }
    }

    public void recordEventProcessed(String tenantId, boolean success) {
        Counter.builder("integration.events.processed")
            .tag("tenant", safeTag(tenantId))
            .tag("success", String.valueOf(success))
            .register(registry)
            .increment();
    }

    private static String safeTag(String value) {
        return value != null && !value.isBlank() ? value : "unknown";
    }
}
