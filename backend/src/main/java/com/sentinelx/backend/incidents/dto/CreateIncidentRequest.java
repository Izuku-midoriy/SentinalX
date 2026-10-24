package com.sentinelx.backend.incidents.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateIncidentRequest(
        @NotBlank @Size(max = 200) String title,
        String description,
        String severity,
        UUID assignedTo,
        String assignedToEmail,
        List<UUID> alertIds
) {}
