package com.sentinelx.backend.detection.rules;

import com.sentinelx.backend.detection.engine.AlertCandidate;
import com.sentinelx.backend.detection.engine.DetectionRuleHandler;
import com.sentinelx.backend.ingestion.entity.SecurityEvent;
import com.sentinelx.backend.ingestion.repository.SecurityEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PortScanDetectionRuleHandler implements DetectionRuleHandler {

    private final SecurityEventRepository eventRepository;
    private final int threshold;
    private final int windowMinutes;

    public PortScanDetectionRuleHandler(
            SecurityEventRepository eventRepository,
            @Value("${detection.rules.port-scan.threshold:2}") int threshold,
            @Value("${detection.rules.port-scan.window-minutes:10}") int windowMinutes
    ) {
        this.eventRepository = eventRepository;
        this.threshold = threshold;
        this.windowMinutes = windowMinutes;
    }

    @Override
    public String getRuleKey() {
        return "RULE_NETWORK_PORT_SCAN";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getName() {
        return "Suspicious Port Scan / Dropped Traffic Activity";
    }

    @Override
    public String getDescription() {
        return "Detects multiple port scans or suspicious packet drops originating from the same source IP in a time window.";
    }

    @Override
    public String getSeverity() {
        return "MEDIUM";
    }

    private boolean isScanOrDropEvent(String eventType) {
        if (eventType == null) return false;
        String lower = eventType.toLowerCase().trim();
        return lower.equals("port_scan") || lower.equals("drop_packet") ||
               lower.equals("packet_drop") || lower.equals("firewall_drop") ||
               lower.equals("network_scan");
    }

    @Override
    public Optional<AlertCandidate> evaluate(SecurityEvent event) {
        if (!isScanOrDropEvent(event.getEventType())) {
            return Optional.empty();
        }

        if (event.getSourceIp() == null || event.getSourceIp().isBlank()) {
            return Optional.empty();
        }

        Instant sinceTime = event.getEventTime().minus(windowMinutes, ChronoUnit.MINUTES);
        List<SecurityEvent> events = eventRepository.findRecentEventsBySourceIp(
                event.getTenantId(),
                event.getSourceIp().trim(),
                sinceTime
        );

        List<SecurityEvent> scanEvents = events.stream()
                .filter(e -> isScanOrDropEvent(e.getEventType()))
                .toList();

        if (scanEvents.size() >= threshold) {
            String dedupKey = "PORT_SCAN:" + event.getTenantId() + ":" + event.getSourceIp().trim();
            List<UUID> supportingIds = scanEvents.stream().map(SecurityEvent::getId).toList();

            String description = String.format(
                    "Detected %d network port scan / dropped traffic events within %d minutes from source IP '%s' targeting host '%s'.",
                    scanEvents.size(),
                    windowMinutes,
                    event.getSourceIp(),
                    event.getHost() != null ? event.getHost() : "N/A"
            );

            AlertCandidate candidate = new AlertCandidate(
                    getRuleKey(),
                    getVersion(),
                    "Network Port Scan Activity Detected",
                    description,
                    getSeverity(),
                    0.85,
                    dedupKey,
                    event.getEventTime(),
                    supportingIds
            );

            return Optional.of(candidate);
        }

        return Optional.empty();
    }
}
