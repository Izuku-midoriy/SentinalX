package com.sentinelx.backend.threatintel.dto;

public record IocLookupResponse(
        boolean matched,
        String queryValue,
        ThreatIntelIocResponse ioc
) {}
