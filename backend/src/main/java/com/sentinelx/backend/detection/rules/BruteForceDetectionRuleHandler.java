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
public class BruteForceDetectionRuleHandler implements DetectionRuleHandler {

    private final SecurityEventRepository eventRepository;
    private final int threshold;
    private final int windowMinutes;

    public BruteForceDetectionRuleHandler(
            SecurityEventRepository eventRepository,
            @Value("${detection.rules.brute-force.threshold:3}") int threshold,
            @Value("${detection.rules.brute-force.window-minutes:5}") int windowMinutes
    ) {
        this.eventRepository = eventRepository;
        this.threshold = threshold;
        this.windowMinutes = windowMinutes;
    }

    @Override
    public String getRuleKey() {
        return "RULE_AUTH_BRUTE_FORCE";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getName() {
        return "Repeated Authentication Failure (Brute-Force)";
    }

    @Override
    public String getDescription() {
        return "Triggers when repeated authentication failures occur from the same source IP or targeting the same username within a configurable window.";
    }

    @Override
    public String getSeverity() {
        return "HIGH";
    }

    @Override
    public Optional<AlertCandidate> evaluate(SecurityEvent event) {
        if (!"authentication_failure".equalsIgnoreCase(event.getEventType())) {
            return Optional.empty();
        }

        Instant sinceTime = event.getEventTime().minus(windowMinutes, ChronoUnit.MINUTES);
        List<SecurityEvent> matchingEvents = List.of();
        String targetKey = null;

        if (event.getSourceIp() != null && !event.getSourceIp().isBlank()) {
            matchingEvents = eventRepository.findRecentMatchingEvents(
                    event.getTenantId(),
                    "authentication_failure",
                    event.getSourceIp().trim(),
                    null,
                    sinceTime
            );
            targetKey = event.getSourceIp().trim();
        }

        if (matchingEvents.size() < threshold && event.getUsername() != null && !event.getUsername().isBlank()) {
            matchingEvents = eventRepository.findRecentMatchingEvents(
                    event.getTenantId(),
                    "authentication_failure",
                    null,
                    event.getUsername().trim(),
                    sinceTime
            );
            targetKey = event.getUsername().trim();
        }

        if (matchingEvents.size() >= threshold && targetKey != null) {
            String dedupKey = "AUTH_BRUTE_FORCE:" + event.getTenantId() + ":" + targetKey;
            List<UUID> supportingIds = matchingEvents.stream().map(SecurityEvent::getId).toList();

            String description = String.format(
                    "Detected %d authentication failures within %d minutes for user '%s' / source IP '%s' on host '%s'.",
                    matchingEvents.size(),
                    windowMinutes,
                    event.getUsername() != null ? event.getUsername() : "N/A",
                    event.getSourceIp() != null ? event.getSourceIp() : "N/A",
                    event.getHost() != null ? event.getHost() : "N/A"
            );

            AlertCandidate candidate = new AlertCandidate(
                    getRuleKey(),
                    getVersion(),
                    "Brute-Force Authentication Attempt Detected",
                    description,
                    getSeverity(),
                    0.95,
                    dedupKey,
                    event.getEventTime(),
                    supportingIds
            );

            return Optional.of(candidate);
        }

        return Optional.empty();
    }
}
