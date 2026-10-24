package com.sentinelx.backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record IncidentSummaryRequest(
        @JsonProperty("incident_id") String incidentId,
        @JsonProperty("title") String title,
        @JsonProperty("severity") String severity,
        @JsonProperty("description") String description,
        @JsonProperty("alert_count") int alertCount,
        @JsonProperty("sample_alert_titles") List<String> sampleAlertTitles,
        @JsonProperty("source_ips") List<String> sourceIps,
        @JsonProperty("destination_ips") List<String> destinationIps
) {}
