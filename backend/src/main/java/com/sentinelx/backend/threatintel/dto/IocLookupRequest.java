package com.sentinelx.backend.threatintel.dto;

import jakarta.validation.constraints.NotBlank;

public record IocLookupRequest(
        @NotBlank(message = "value is required")
        String value
) {}
