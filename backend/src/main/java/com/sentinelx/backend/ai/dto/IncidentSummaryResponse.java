package com.sentinelx.backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record IncidentSummaryResponse(
        @JsonProperty("incident_id") String incidentId,
        @JsonProperty("ai_provider") String aiProvider,
        @JsonProperty("model_name") String modelName,
        @JsonProperty("summary_markdown") String summaryMarkdown,
        @JsonProperty("suggested_actions") List<String> suggestedActions,
        @JsonProperty("disclaimer") String disclaimer
) {
    public static IncidentSummaryResponse offlineFallback(String incidentId, String title, String severity) {
        String fallbackMd = String.format(
                "### 🛡️ Incident Triage: %s\n\n" +
                "**Severity Level:** `%s`\n\n" +
                "*(Generated via local fallback — AI microservice is currently offline)*\n\n" +
                "- **Status:** Manual investigation recommended.\n" +
                "- **Evidence:** Refer to linked deterministic alerts and supporting security events.\n" +
                "- **Next Step:** Update incident status to `UNDER_INVESTIGATION` and review target host telemetry.",
                title, severity
        );

        return new IncidentSummaryResponse(
                incidentId,
                "sentinelx-offline-fallback",
                "deterministic-rule-evidence",
                fallbackMd,
                List.of(
                        "Review linked alerts and timeline entries",
                        "Verify host network logs",
                        "Start python ai-service to enable local LLM triage summaries"
                ),
                "AI service is offline. This summary was generated deterministically from incident status metadata."
        );
    }
}
