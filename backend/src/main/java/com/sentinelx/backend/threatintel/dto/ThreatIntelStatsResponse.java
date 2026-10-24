package com.sentinelx.backend.threatintel.dto;

public record ThreatIntelStatsResponse(
        long totalIocs,
        long highCriticalIocs,
        long totalMatches,
        int activeFeedsCount,
        String feedStatus
) {}
