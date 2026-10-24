package com.sentinelx.backend.detection.engine;

import com.sentinelx.backend.ingestion.entity.SecurityEvent;
import com.sentinelx.backend.operations.metrics.SentinelXMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DetectionEngine {

    private static final Logger log = LoggerFactory.getLogger(DetectionEngine.class);

    private final List<DetectionRuleHandler> ruleHandlers;
    private final SentinelXMetrics metrics;

    public DetectionEngine(List<DetectionRuleHandler> ruleHandlers, SentinelXMetrics metrics) {
        this.ruleHandlers = ruleHandlers;
        this.metrics = metrics;
    }

    public List<AlertCandidate> evaluateEvent(SecurityEvent event) {
        return metrics.getDetectionTimer().record(() -> {
            List<AlertCandidate> candidates = new ArrayList<>();

            for (DetectionRuleHandler handler : ruleHandlers) {
                try {
                    Optional<AlertCandidate> result = handler.evaluate(event);
                    result.ifPresent(candidates::add);
                } catch (Exception ex) {
                    log.error("Error evaluating detection rule {}: {}", handler.getRuleKey(), ex.getMessage(), ex);
                }
            }

            return candidates;
        });
    }
}
