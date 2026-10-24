package com.sentinelx.backend.ingestion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchEventIngestRequest(
        @NotEmpty(message = "Events list cannot be empty")
        @Size(max = 100, message = "Batch size cannot exceed 100 events")
        List<@Valid EventIngestRequest> events
) {}
