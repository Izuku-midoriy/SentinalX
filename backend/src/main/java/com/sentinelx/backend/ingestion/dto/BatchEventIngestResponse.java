package com.sentinelx.backend.ingestion.dto;

import java.util.List;
import java.util.UUID;

public record BatchEventIngestResponse(
        int totalReceived,
        int acceptedCount,
        int duplicatesCount,
        List<UUID> eventIds
) {}
