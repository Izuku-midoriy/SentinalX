package com.sentinelx.backend.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelx.backend.identity.dto.LoginRequest;
import com.sentinelx.backend.ingestion.dto.BatchEventIngestRequest;
import com.sentinelx.backend.ingestion.dto.EventIngestRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EventIngestionIntegrationTests {

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
    @DisplayName("SX-0301/SX-0303: Ingest valid security event returns normalized representation")
    void testSuccessfulEventIngestion() throws Exception {
        String token = obtainToken("analyst@alpha.local", "DevAnalyst123!");

        EventIngestRequest request = new EventIngestRequest(
                Instant.now(),
                "linux",
                "srv-auth-01",
                "authentication_failure",
                "HIGH",
                "srv-auth-01",
                "192.0.2.10",
                null,
                "admin",
                "sshd",
                1024,
                "Failed password for invalid user admin",
                "{\"raw\":\"ssh log line\"}",
                "test-ssh-1"
        );

        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.schemaVersion").value("1.0"))
                .andExpect(jsonPath("$.sourceType").value("linux"))
                .andExpect(jsonPath("$.eventType").value("authentication_failure"))
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.duplicate").value(false));
    }

    @Test
    @DisplayName("SX-0304: Duplicate event with same idempotency key is handled idempotently")
    void testIdempotencyHandling() throws Exception {
        String token = obtainToken("analyst@alpha.local", "DevAnalyst123!");
        String idempotencyKey = "unique-key-" + UUID.randomUUID();

        EventIngestRequest request = new EventIngestRequest(
                Instant.now(),
                "firewall",
                "fw-core-1",
                "drop_packet",
                "LOW",
                "fw-core-1",
                "198.51.100.22",
                "10.0.0.5",
                null,
                null,
                null,
                "Dropped unauthorized packet",
                null,
                idempotencyKey
        );

        // First submission -> Created (201)
        String firstResponse = mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.duplicate").value(false))
                .andReturn().getResponse().getContentAsString();

        String firstId = objectMapper.readTree(firstResponse).get("id").asText();

        // Second submission with identical key -> OK (200), duplicate: true, same ID
        String secondResponse = mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicate").value(true))
                .andExpect(jsonPath("$.id").value(firstId))
                .andReturn().getResponse().getContentAsString();

        assertThat(firstId).isEqualTo(objectMapper.readTree(secondResponse).get("id").asText());
    }

    @Test
    @DisplayName("SX-0302: Malformed or invalid event payload is rejected with 400 Bad Request")
    void testInvalidEventPayload() throws Exception {
        String token = obtainToken("analyst@alpha.local", "DevAnalyst123!");

        // Missing eventType and sourceType
        String invalidJson = """
                {
                  "timestamp": "2026-10-12T12:00:00Z",
                  "sourceName": "test-source"
                }
                """;

        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("validation_error"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    @DisplayName("SX-0303: Cross-tenant isolation - Tenant A cannot see Tenant B events")
    void testCrossTenantEventIsolation() throws Exception {
        String alphaToken = obtainToken("analyst@alpha.local", "DevAnalyst123!");
        String betaToken = obtainToken("analyst@beta.local", "DevBetaAnalyst123!");

        // Ingest event for Tenant Beta
        EventIngestRequest betaRequest = new EventIngestRequest(
                Instant.now(),
                "windows",
                "ad-controller-beta",
                "ntlm_failure",
                "MEDIUM",
                "ad-controller-beta",
                "10.200.1.5",
                null,
                "victim_user",
                "lsass.exe",
                512,
                "NTLM authentication failure on Beta DC",
                null,
                "beta-event-" + UUID.randomUUID()
        );

        String betaResponse = mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + betaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(betaRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String betaEventId = objectMapper.readTree(betaResponse).get("id").asText();

        // Tenant Alpha queries events list -> beta event must NOT appear
        String alphaEventsList = mockMvc.perform(get("/api/v1/events")
                        .header("Authorization", "Bearer " + alphaToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(alphaEventsList).doesNotContain(betaEventId);

        // Tenant Alpha attempts direct GET on Tenant Beta's event ID -> rejected / not found (404)
        mockMvc.perform(get("/api/v1/events/" + betaEventId)
                        .header("Authorization", "Bearer " + alphaToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("not_found_error"));
    }

    @Test
    @DisplayName("SX-0303: Batch event ingestion endpoint accepts bounded event list")
    void testBatchEventIngestion() throws Exception {
        String token = obtainToken("analyst@alpha.local", "DevAnalyst123!");

        BatchEventIngestRequest batch = new BatchEventIngestRequest(List.of(
                new EventIngestRequest(Instant.now(), "web", "nginx-ingress", "http_404", "INFO", "web-1", "1.2.3.4", null, null, null, null, "Not found", null, null),
                new EventIngestRequest(Instant.now(), "web", "nginx-ingress", "http_403", "MEDIUM", "web-1", "1.2.3.4", null, null, null, null, "Forbidden access", null, null)
        ));

        mockMvc.perform(post("/api/v1/events/batch")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReceived").value(2))
                .andExpect(jsonPath("$.acceptedCount").value(2))
                .andExpect(jsonPath("$.eventIds").isArray());
    }
}
