package com.sentinelx.backend.detection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelx.backend.alerts.dto.UpdateAlertRequest;
import com.sentinelx.backend.alerts.model.AlertStatus;
import com.sentinelx.backend.identity.dto.LoginRequest;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DetectionIntegrationTests {

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

    private void ingestEvent(String token, String eventType, String severity, String sourceIp, String username) throws Exception {
        EventIngestRequest request = new EventIngestRequest(
                Instant.now(),
                "linux",
                "sensor-01",
                eventType,
                severity,
                "host-alpha",
                sourceIp,
                "10.0.0.1",
                username,
                null,
                null,
                "Synthetic security event message",
                null,
                UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("SX-0401/SX-0403: Brute force rule triggers alert on 3 failures and deduplicates on 4th")
    void testBruteForceDetectionAndDeduplication() throws Exception {
        String token = obtainToken("analyst@alpha.local", "DevAnalyst123!");
        String attackerIp = "198.51.100.77";

        // Step 1: Ingest 2 authentication failures -> below threshold (3)
        ingestEvent(token, "authentication_failure", "HIGH", attackerIp, "admin");
        ingestEvent(token, "authentication_failure", "HIGH", attackerIp, "admin");

        // Verify no alerts yet for this specific dedup key
        String initialAlerts = mockMvc.perform(get("/api/v1/alerts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(initialAlerts).doesNotContain(attackerIp);

        // Step 2: Ingest 3rd authentication failure -> triggers threshold
        ingestEvent(token, "authentication_failure", "HIGH", attackerIp, "admin");

        String alertsAfterThreshold = mockMvc.perform(get("/api/v1/alerts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var tree = objectMapper.readTree(alertsAfterThreshold);
        var alerts = tree.get("content");
        assertThat(alerts.isArray()).isTrue();

        var alertOpt = java.util.stream.StreamSupport.stream(alerts.spliterator(), false)
                .filter(a -> a.get("ruleKey").asText().equals("RULE_AUTH_BRUTE_FORCE") &&
                             a.get("deduplicationKey").asText().contains(attackerIp))
                .findFirst();

        assertThat(alertOpt).isPresent();
        var alert = alertOpt.get();
        String alertId = alert.get("id").asText();
        assertThat(alert.get("severity").asText()).isEqualTo("HIGH");
        assertThat(alert.get("status").asText()).isEqualTo("OPEN");
        assertThat(alert.get("eventCount").asInt()).isEqualTo(1);
        assertThat(alert.get("supportingEventIds").size()).isGreaterThanOrEqualTo(3);

        // Step 3: Ingest 4th failure from same IP -> deduplicates into existing alert
        ingestEvent(token, "authentication_failure", "HIGH", attackerIp, "admin");

        String singleAlertJson = mockMvc.perform(get("/api/v1/alerts/" + alertId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var updatedAlert = objectMapper.readTree(singleAlertJson);
        assertThat(updatedAlert.get("id").asText()).isEqualTo(alertId);
        assertThat(updatedAlert.get("eventCount").asInt()).isEqualTo(2);
        assertThat(updatedAlert.get("supportingEventIds").size()).isGreaterThanOrEqualTo(4);
    }

    @Test
    @DisplayName("SX-0402: Port scan rule triggers medium severity alert on threshold")
    void testPortScanDetectionRule() throws Exception {
        String token = obtainToken("analyst@alpha.local", "DevAnalyst123!");
        String scannerIp = "203.0.113.88";

        // Ingest 2 port scan events (threshold is 2)
        ingestEvent(token, "port_scan", "MEDIUM", scannerIp, null);
        ingestEvent(token, "port_scan", "MEDIUM", scannerIp, null);

        String alertsResponse = mockMvc.perform(get("/api/v1/alerts?severity=MEDIUM")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var tree = objectMapper.readTree(alertsResponse);
        var alerts = tree.get("content");

        var scanAlertOpt = java.util.stream.StreamSupport.stream(alerts.spliterator(), false)
                .filter(a -> a.get("ruleKey").asText().equals("RULE_NETWORK_PORT_SCAN") &&
                             a.get("deduplicationKey").asText().contains(scannerIp))
                .findFirst();

        assertThat(scanAlertOpt).isPresent();
        var scanAlert = scanAlertOpt.get();
        assertThat(scanAlert.get("severity").asText()).isEqualTo("MEDIUM");
        assertThat(scanAlert.get("title").asText()).contains("Port Scan");
        assertThat(scanAlert.get("supportingEventIds").size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("SX-0404: Alert lifecycle management - transition through states and update assignment")
    void testAlertLifecycleManagement() throws Exception {
        String token = obtainToken("analyst@alpha.local", "DevAnalyst123!");
        String attackerIp = "198.51.100.99";

        // Generate alert by 3 failed logins
        ingestEvent(token, "authentication_failure", "HIGH", attackerIp, "root");
        ingestEvent(token, "authentication_failure", "HIGH", attackerIp, "root");
        ingestEvent(token, "authentication_failure", "HIGH", attackerIp, "root");

        String alertsResponse = mockMvc.perform(get("/api/v1/alerts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var alerts = objectMapper.readTree(alertsResponse).get("content");
        var alert = java.util.stream.StreamSupport.stream(alerts.spliterator(), false)
                .filter(a -> a.get("deduplicationKey").asText().contains(attackerIp))
                .findFirst()
                .orElseThrow();

        String alertId = alert.get("id").asText();
        assertThat(alert.get("status").asText()).isEqualTo("OPEN");

        // Transition: OPEN -> INVESTIGATING
        UpdateAlertRequest investigateReq = new UpdateAlertRequest(AlertStatus.INVESTIGATING, null);
        mockMvc.perform(patch("/api/v1/alerts/" + alertId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(investigateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVESTIGATING"));

        // Transition: INVESTIGATING -> RESOLVED
        UpdateAlertRequest resolveReq = new UpdateAlertRequest(AlertStatus.RESOLVED, null);
        mockMvc.perform(patch("/api/v1/alerts/" + alertId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    @DisplayName("SX-0403/SX-0404: Cross-tenant isolation - Tenant Beta cannot view or modify Tenant Alpha alerts")
    void testCrossTenantAlertIsolation() throws Exception {
        String alphaToken = obtainToken("analyst@alpha.local", "DevAnalyst123!");
        String betaToken = obtainToken("analyst@beta.local", "DevBetaAnalyst123!");
        String targetIp = "192.0.2.111";

        // Generate alert for Tenant Alpha
        ingestEvent(alphaToken, "authentication_failure", "HIGH", targetIp, "target-alpha");
        ingestEvent(alphaToken, "authentication_failure", "HIGH", targetIp, "target-alpha");
        ingestEvent(alphaToken, "authentication_failure", "HIGH", targetIp, "target-alpha");

        String alphaAlertsResponse = mockMvc.perform(get("/api/v1/alerts")
                        .header("Authorization", "Bearer " + alphaToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var alerts = objectMapper.readTree(alphaAlertsResponse).get("content");
        var alphaAlert = java.util.stream.StreamSupport.stream(alerts.spliterator(), false)
                .filter(a -> a.get("deduplicationKey").asText().contains(targetIp))
                .findFirst()
                .orElseThrow();

        String alphaAlertId = alphaAlert.get("id").asText();

        // 1. Tenant Beta queries alerts list -> alpha alert must not be present
        String betaAlertsResponse = mockMvc.perform(get("/api/v1/alerts")
                        .header("Authorization", "Bearer " + betaToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(betaAlertsResponse).doesNotContain(alphaAlertId);
        assertThat(betaAlertsResponse).doesNotContain(targetIp);

        // 2. Tenant Beta attempts direct GET on Tenant Alpha alert -> 404
        mockMvc.perform(get("/api/v1/alerts/" + alphaAlertId)
                        .header("Authorization", "Bearer " + betaToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("not_found_error"));

        // 3. Tenant Beta attempts PATCH on Tenant Alpha alert -> 404
        UpdateAlertRequest updateReq = new UpdateAlertRequest(AlertStatus.CLOSED, null);
        mockMvc.perform(patch("/api/v1/alerts/" + alphaAlertId)
                        .header("Authorization", "Bearer " + betaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("not_found_error"));
    }
}
