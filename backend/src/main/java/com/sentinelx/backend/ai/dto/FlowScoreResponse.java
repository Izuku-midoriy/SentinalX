package com.sentinelx.backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record FlowScoreResponse(
        @JsonProperty("model_name") String modelName,
        @JsonProperty("model_version") String modelVersion,
        @JsonProperty("prediction") String prediction,
        @JsonProperty("is_threat") boolean isThreat,
        @JsonProperty("risk_score") int riskScore,
        @JsonProperty("confidence") double confidence,
        @JsonProperty("class_probabilities") Map<String, Double> classProbabilities,
        @JsonProperty("explanation") String explanation,
        @JsonProperty("model_status") String modelStatus
) {
    public static FlowScoreResponse offlineFallback() {
        return new FlowScoreResponse(
                "sentinelx-offline-fallback",
                "0.0.0",
                "BENIGN",
                false,
                0,
                0.0,
                Map.of("BENIGN", 1.0),
                "AI service offline or unreachable; deterministic rule engine remains operational.",
                "offline"
        );
    }
}
