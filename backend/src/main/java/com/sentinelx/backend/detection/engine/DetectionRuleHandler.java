package com.sentinelx.backend.detection.engine;

import com.sentinelx.backend.ingestion.entity.SecurityEvent;

import java.util.Optional;

public interface DetectionRuleHandler {
    String getRuleKey();
    String getVersion();
    String getName();
    String getDescription();
    String getSeverity();
    Optional<AlertCandidate> evaluate(SecurityEvent event);
}
