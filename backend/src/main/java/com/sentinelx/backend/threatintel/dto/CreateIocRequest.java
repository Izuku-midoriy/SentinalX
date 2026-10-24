package com.sentinelx.backend.threatintel.dto;

import com.sentinelx.backend.threatintel.model.IocType;
import com.sentinelx.backend.threatintel.model.ThreatSeverity;
import com.sentinelx.backend.threatintel.model.ThreatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateIocRequest(
        @NotNull(message = "iocType is required")
        IocType iocType,

        @NotBlank(message = "iocValue is required")
        @Size(max = 255, message = "iocValue must not exceed 255 characters")
        String iocValue,

        @NotNull(message = "threatType is required")
        ThreatType threatType,

        ThreatSeverity severity,

        Double confidence,

        @Size(max = 100, message = "sourceFeed must not exceed 100 characters")
        String sourceFeed,

        @Size(max = 1000, message = "description must not exceed 1000 characters")
        String description,

        Instant expiresAt,

        Boolean isGlobal
) {}
