package com.sentinelx.backend.incidents.dto;

import java.util.UUID;

public record CreateIncidentFromAlertRequest(
        String title,
        String description,
        String severity,
        UUID assignedTo,
        String assignedToEmail
) {}
