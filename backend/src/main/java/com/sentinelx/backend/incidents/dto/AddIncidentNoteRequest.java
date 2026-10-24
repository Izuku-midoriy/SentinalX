package com.sentinelx.backend.incidents.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddIncidentNoteRequest(
        @NotBlank @Size(max = 4000) String body
) {}
