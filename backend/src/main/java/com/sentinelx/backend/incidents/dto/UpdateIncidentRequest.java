package com.sentinelx.backend.incidents.dto;

import com.sentinelx.backend.incidents.model.IncidentStatus;
import java.util.UUID;

public record UpdateIncidentRequest(
        String title,
        String description,
        String severity,
        IncidentStatus status,
        UUID assignedTo,
        String assignedToEmail
) {}
