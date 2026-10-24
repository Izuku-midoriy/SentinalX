package com.sentinelx.backend.ai.service;

import com.sentinelx.backend.ai.dto.*;
import com.sentinelx.backend.alerts.repository.AlertRepository;
import com.sentinelx.backend.incidents.entity.Incident;
import com.sentinelx.backend.incidents.entity.IncidentAlert;
import com.sentinelx.backend.incidents.repository.IncidentAlertRepository;
import com.sentinelx.backend.incidents.repository.IncidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.*;

@Service
public class AiClientService {

    private static final Logger log = LoggerFactory.getLogger(AiClientService.class);

    private final RestClient restClient;
    private final String serviceUrl;
    private final boolean enabled;
    private final IncidentRepository incidentRepository;
    private final IncidentAlertRepository incidentAlertRepository;
    private final AlertRepository alertRepository;

    public AiClientService(
            @Value("${sentinelx.ai.service-url:http://localhost:8000}") String serviceUrl,
            @Value("${sentinelx.ai.enabled:true}") boolean enabled,
            @Value("${sentinelx.ai.timeout-ms:1500}") int timeoutMs,
            IncidentRepository incidentRepository,
            IncidentAlertRepository incidentAlertRepository,
            AlertRepository alertRepository
    ) {
        this.serviceUrl = serviceUrl;
        this.enabled = enabled;
        this.incidentRepository = incidentRepository;
        this.incidentAlertRepository = incidentAlertRepository;
        this.alertRepository = alertRepository;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));

        this.restClient = RestClient.builder()
                .baseUrl(serviceUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public AiServiceStatusResponse getStatus() {
        if (!enabled) {
            return new AiServiceStatusResponse(
                    false, serviceUrl, false, "disabled", "none", List.of(), false,
                    "AI service integration is disabled by configuration."
            );
        }

        try {
            Map<?, ?> health = restClient.get()
                    .uri("/health")
                    .retrieve()
                    .body(Map.class);

            if (health != null) {
                boolean modelLoaded = Boolean.TRUE.equals(health.get("model_loaded"));
                String modelName = Objects.toString(health.get("model_name"), "unknown");
                String modelVersion = Objects.toString(health.get("model_version"), "unknown");
                boolean ollama = Boolean.TRUE.equals(health.get("ollama_connected"));
                List<String> classes = (health.get("classes") instanceof List<?> list)
                        ? list.stream().map(Object::toString).toList()
                        : List.of();

                return new AiServiceStatusResponse(
                        true, serviceUrl, modelLoaded, modelName, modelVersion, classes, ollama,
                        "AI microservice is online and accessible."
                );
            }
        } catch (Exception ex) {
            log.debug("AI microservice unreachable at {}: {}", serviceUrl, ex.getMessage());
        }

        return AiServiceStatusResponse.offline(serviceUrl);
    }

    public FlowScoreResponse scoreFlow(FlowScoreRequest request) {
        if (!enabled) {
            return FlowScoreResponse.offlineFallback();
        }

        try {
            FlowScoreResponse response = restClient.post()
                    .uri("/api/v1/score")
                    .body(request != null ? request : FlowScoreRequest.defaults())
                    .retrieve()
                    .body(FlowScoreResponse.class);

            if (response != null) {
                return response;
            }
        } catch (Exception ex) {
            log.debug("AI score request failed (falling back to offline response): {}", ex.getMessage());
        }

        return FlowScoreResponse.offlineFallback();
    }

    public IncidentSummaryResponse summarizeIncident(UUID tenantId, UUID incidentId) {
        Incident incident = incidentRepository.findByTenantIdAndId(tenantId, incidentId)
                .orElseThrow(() -> new NoSuchElementException("Incident not found: " + incidentId));

        List<UUID> alertIds = incidentAlertRepository.findByTenantIdAndIncidentId(tenantId, incidentId).stream()
                .map(IncidentAlert::getAlertId)
                .toList();

        List<String> alertTitles = alertRepository.findAllById(alertIds).stream()
                .map(a -> a.getTitle())
                .toList();

        IncidentSummaryRequest summaryReq = new IncidentSummaryRequest(
                incident.getId().toString(),
                incident.getTitle(),
                incident.getSeverity(),
                incident.getDescription(),
                alertIds.size(),
                alertTitles,
                List.of(),
                List.of()
        );

        if (!enabled) {
            return IncidentSummaryResponse.offlineFallback(
                    incident.getId().toString(), incident.getTitle(), incident.getSeverity()
            );
        }

        try {
            IncidentSummaryResponse response = restClient.post()
                    .uri("/api/v1/summarize")
                    .body(summaryReq)
                    .retrieve()
                    .body(IncidentSummaryResponse.class);

            if (response != null) {
                return response;
            }
        } catch (Exception ex) {
            log.debug("AI summarization failed (using fallback template): {}", ex.getMessage());
        }

        return IncidentSummaryResponse.offlineFallback(
                incident.getId().toString(), incident.getTitle(), incident.getSeverity()
        );
    }
}
