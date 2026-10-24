package com.sentinelx.backend.operations.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class SentinelXMetrics {

    private final MeterRegistry meterRegistry;
    private final Counter duplicateEventsCounter;
    private final Timer detectionTimer;

    public SentinelXMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.duplicateEventsCounter = Counter.builder("sentinelx.events.duplicates.total")
                .description("Total number of deduplicated security event submissions")
                .register(meterRegistry);

        this.detectionTimer = Timer.builder("sentinelx.detection.latency.seconds")
                .description("Execution duration of real-time detection rule evaluation")
                .publishPercentiles(0.5, 0.95, 0.99)
                .sla(Duration.ofMillis(50), Duration.ofMillis(100), Duration.ofMillis(250))
                .register(meterRegistry);
    }

    public void recordEventIngested(String eventType, String severity, String sourceType) {
        Counter.builder("sentinelx.events.ingested.total")
                .description("Total number of normalized security events ingested")
                .tag("event_type", eventType != null ? eventType.toLowerCase() : "unknown")
                .tag("severity", severity != null ? severity.toUpperCase() : "INFO")
                .tag("source_type", sourceType != null ? sourceType.toLowerCase() : "unknown")
                .register(meterRegistry)
                .increment();
    }

    public void recordEventDuplicate() {
        duplicateEventsCounter.increment();
    }

    public void recordAlertGenerated(String ruleKey, String severity) {
        Counter.builder("sentinelx.alerts.generated.total")
                .description("Total number of security alerts triggered by detection rules")
                .tag("rule_key", ruleKey != null ? ruleKey : "unknown")
                .tag("severity", severity != null ? severity.toUpperCase() : "MEDIUM")
                .register(meterRegistry)
                .increment();
    }

    public void recordIncidentCreated(String severity) {
        Counter.builder("sentinelx.incidents.created.total")
                .description("Total number of security incidents opened for investigation")
                .tag("severity", severity != null ? severity.toUpperCase() : "HIGH")
                .register(meterRegistry)
                .increment();
    }

    public Timer getDetectionTimer() {
        return detectionTimer;
    }
}
