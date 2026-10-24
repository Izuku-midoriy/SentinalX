package com.sentinelx.backend.operations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelx.backend.identity.dto.LoginRequest;
import com.sentinelx.backend.ingestion.dto.EventIngestRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
@ActiveProfiles("test")
class ObservabilityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String obtainToken(String email, String password) throws Exception {
        LoginRequest login = new LoginRequest(email, password);
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    @Test
    @DisplayName("SX-0701: Actuator health, liveness, and readiness endpoints return UP")
    void testHealthAndReadinessEndpoints() throws Exception {
        // Discover actuator endpoints
        String actuatorDiscovery = mockMvc.perform(get("/actuator"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        System.out.println("=== ACTUATOR DISCOVERY ===");
        System.out.println(actuatorDiscovery);

        // Main health
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        // Liveness probe
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        // Readiness probe
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        // Custom API v1 health
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("SX-0701: Prometheus endpoint exposes metrics and custom SentinelX counters")
    void testPrometheusEndpointAndCustomMetrics() throws Exception {
        String token = obtainToken("analyst@alpha.local", "DevAnalyst123!");

        // Ingest sample security event to trigger custom metric increment
        EventIngestRequest request = new EventIngestRequest(
                Instant.now(),
                "linux",
                "bastion-observability-01",
                "authentication_failure",
                "HIGH",
                "host-obs",
                "198.51.100.200",
                "10.0.0.1",
                "admin",
                null,
                null,
                "Observability test authentication failure",
                null,
                UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Scrape /actuator/prometheus
        String prometheusScrape = mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Verify standard JVM & Spring metrics are present
        assertThat(prometheusScrape).contains("jvm_memory_used_bytes");
        assertThat(prometheusScrape).contains("process_cpu_usage");

        // Verify custom SentinelX business metrics are exposed
        assertThat(prometheusScrape).contains("sentinelx_events_ingested_total");
        assertThat(prometheusScrape).contains("sentinelx_detection_latency_seconds");

        // Security check: ensure passwords/tokens are NOT exposed in metrics scrape
        assertThat(prometheusScrape).doesNotContain("DevAnalyst123!");
        assertThat(prometheusScrape).doesNotContain("postgres_dev_password");
    }
}
