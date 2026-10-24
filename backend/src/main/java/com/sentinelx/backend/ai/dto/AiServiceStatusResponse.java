package com.sentinelx.backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AiServiceStatusResponse(
        @JsonProperty("online") boolean online,
        @JsonProperty("service_url") String serviceUrl,
        @JsonProperty("model_loaded") boolean modelLoaded,
        @JsonProperty("model_name") String modelName,
        @JsonProperty("model_version") String modelVersion,
        @JsonProperty("classes") List<String> classes,
        @JsonProperty("ollama_connected") boolean ollamaConnected,
        @JsonProperty("message") String message
) {
    public static AiServiceStatusResponse offline(String serviceUrl) {
        return new AiServiceStatusResponse(
                false,
                serviceUrl,
                false,
                "none",
                "0.0.0",
                List.of(),
                false,
                "AI microservice is offline at " + serviceUrl + ". Deterministic detection is 100% operational."
        );
    }
}
