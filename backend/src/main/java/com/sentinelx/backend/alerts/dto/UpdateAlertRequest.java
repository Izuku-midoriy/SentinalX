package com.sentinelx.backend.alerts.dto;

import com.sentinelx.backend.alerts.model.AlertStatus;
import java.util.UUID;

public record UpdateAlertRequest(
        AlertStatus status,
        UUID assignedTo
) {}
