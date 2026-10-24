package com.sentinelx.backend.detection.engine;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AlertCandidate(
        String ruleKey,
        String ruleVersion,
        String title,
        String description,
        String severity,
        Double confidence,
        String deduplicationKey,
        Instant timestamp,
        List<UUID> supportingEventIds
) {}
