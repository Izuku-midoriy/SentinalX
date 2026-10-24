package com.sentinelx.backend.detection.rules;

import com.sentinelx.backend.detection.engine.AlertCandidate;
import com.sentinelx.backend.detection.engine.DetectionRuleHandler;
import com.sentinelx.backend.ingestion.entity.SecurityEvent;
import com.sentinelx.backend.threatintel.entity.ThreatIntelIoc;
import com.sentinelx.backend.threatintel.service.ThreatIntelService;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class ThreatIntelDetectionRuleHandler implements DetectionRuleHandler {

    private final ThreatIntelService threatIntelService;

    public ThreatIntelDetectionRuleHandler(ThreatIntelService threatIntelService) {
        this.threatIntelService = threatIntelService;
    }

    @Override
    public String getRuleKey() {
        return "RULE_THREAT_INTEL_IOC_MATCH";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getName() {
        return "Threat Intelligence IOC Match";
    }

    @Override
    public String getDescription() {
        return "Identifies network traffic, connection attempts, or execution traces involving verified malicious IP addresses, C2 domains, or threat indicators indexed in Threat Intelligence feeds.";
    }

    @Override
    public String getSeverity() {
        return "HIGH";
    }

    @Override
    public Optional<AlertCandidate> evaluate(SecurityEvent event) {
        if (event == null || event.getTenantId() == null) {
            return Optional.empty();
        }

        // 1. Check destination IP (outbound C2 beaconing or exfiltration)
        if (event.getDestinationIp() != null && !event.getDestinationIp().isBlank()) {
            Optional<ThreatIntelIoc> match = threatIntelService.match(event.getTenantId(), event.getDestinationIp());
            if (match.isPresent()) {
                return Optional.of(buildAlertCandidate(event, match.get(), "Destination IP"));
            }
        }

        // 2. Check source IP (inbound attacker scan or attack)
        if (event.getSourceIp() != null && !event.getSourceIp().isBlank()) {
            Optional<ThreatIntelIoc> match = threatIntelService.match(event.getTenantId(), event.getSourceIp());
            if (match.isPresent()) {
                return Optional.of(buildAlertCandidate(event, match.get(), "Source IP"));
            }
        }

        // 3. Check message or host for malicious domain/indicator
        if (event.getMessage() != null && !event.getMessage().isBlank()) {
            String[] tokens = event.getMessage().split("[\\s,;:]+");
            for (String token : tokens) {
                if (token.length() >= 4 && (token.contains(".") || token.length() == 64)) {
                    Optional<ThreatIntelIoc> match = threatIntelService.match(event.getTenantId(), token);
                    if (match.isPresent()) {
                        return Optional.of(buildAlertCandidate(event, match.get(), "Payload/Message Token"));
                    }
                }
            }
        }

        return Optional.empty();
    }

    private AlertCandidate buildAlertCandidate(SecurityEvent event, ThreatIntelIoc ioc, String matchContext) {
        String title = String.format("Threat Intel Match: %s (%s)", ioc.getThreatType().name(), ioc.getIocValue());
        String description = String.format(
                "Telemetry event matched known threat indicator [%s] via %s. Feed: %s. Description: %s",
                ioc.getIocValue(),
                matchContext,
                ioc.getSourceFeed(),
                ioc.getDescription() != null ? ioc.getDescription() : "Identified malicious infrastructure."
        );

        String deduplicationKey = String.format("THREAT_INTEL:%s:%s", event.getTenantId(), ioc.getIocValue());

        return new AlertCandidate(
                getRuleKey(),
                getVersion(),
                title,
                description,
                ioc.getSeverity().name(),
                ioc.getConfidence(),
                deduplicationKey,
                event.getEventTime() != null ? event.getEventTime() : Instant.now(),
                List.of(event.getId())
        );
    }
}
