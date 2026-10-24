package com.sentinelx.backend.incidents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelx.backend.alerts.dto.AlertResponse;
import com.sentinelx.backend.audit.entity.AuditLog;
import com.sentinelx.backend.audit.repository.AuditLogRepository;
import com.sentinelx.backend.identity.dto.LoginRequest;
import com.sentinelx.backend.incidents.dto.AddIncidentNoteRequest;
import com.sentinelx.backend.incidents.dto.CreateIncidentFromAlertRequest;
import com.sentinelx.backend.incidents.dto.CreateIncidentRequest;
import com.sentinelx.backend.incidents.dto.UpdateIncidentRequest;
import com.sentinelx.backend.incidents.model.IncidentStatus;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IncidentIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private String obtainToken(String email, String password) throws Exception {
        LoginRequest login = new LoginRequest(email, password);
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private String createAlert(String token, String sourceIp) throws Exception {
        // Ingest 3 failed logins to trigger brute force alert
        for (int i = 0; i < 3; i++) {
            EventIngestRequest request = new EventIngestRequest(
                    Instant.now(),
                    "linux",
                    "bastion-01",
                    "authentication_failure",
                    "HIGH",
                    "host-alpha",
                    sourceIp,
                    "10.0.0.1",
                    "admin",
                    null,
                    null,
                    "Failed password attempt #" + i,
                    null,
                    UUID.randomUUID().toString()
            );
            mockMvc.perform(post("/api/v1/events")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        String alertsJson = mockMvc.perform(get("/api/v1/alerts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var tree = objectMapper.readTree(alertsJson);
        for (var alertNode : tree.get("content")) {
            if (alertNode.get("deduplicationKey").asText().contains(sourceIp)) {
                return alertNode.get("id").asText();
            }
        }
        throw new IllegalStateException("Alert not found for source IP: " + sourceIp);
    }

    @Test
    @DisplayName("SX-0601: Create incident directly from an alert")
    void testCreateIncidentFromAlert() throws Exception {
        String token = obtainToken("analyst@alpha.local", "DevAnalyst123!");
        String alertId = createAlert(token, "198.51.100.15");

        CreateIncidentFromAlertRequest req = new CreateIncidentFromAlertRequest(
                "Critical Compromise Investigation",
                "Investigating active brute-force intrusion",
                "CRITICAL",
                null,
                null
        );

        String incidentJson = mockMvc.perform(post("/api/v1/alerts/" + alertId + "/incidents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Critical Compromise Investigation"))
                .andExpect(jsonPath("$.severity").value("CRITICAL"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.linkedAlertIds[0]").value(alertId))
                .andReturn().getResponse().getContentAsString();

        var incident = objectMapper.readTree(incidentJson);
        String incidentId = incident.get("id").asText();

        // Verify retrieval via GET /api/v1/incidents/{incidentId}
        mockMvc.perform(get("/api/v1/incidents/" + incidentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incidentId))
                .andExpect(jsonPath("$.linkedAlertIds[0]").value(alertId));
    }

    @Test
    @DisplayName("SX-0602: Status transitions, assignment, and resolution timestamps")
    void testIncidentStatusTransitionsAndAssignment() throws Exception {
        String token = obtainToken("analyst@alpha.local", "DevAnalyst123!");

        // 1. Create a manual incident
        CreateIncidentRequest createReq = new CreateIncidentRequest(
                "Suspicious Outbound Tunnel",
                "Unusual DNS beaconing observed",
                "HIGH",
                null,
                null,
                List.of()
        );

        String res = mockMvc.perform(post("/api/v1/incidents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String incidentId = objectMapper.readTree(res).get("id").asText();

        // 2. Assign to analyst & transition OPEN -> INVESTIGATING
        UUID assignedUserId = UUID.randomUUID();
        UpdateIncidentRequest updateReq = new UpdateIncidentRequest(
                null,
                null,
                null,
                IncidentStatus.INVESTIGATING,
                assignedUserId,
                "analyst@alpha.local"
        );

        mockMvc.perform(patch("/api/v1/incidents/" + incidentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVESTIGATING"))
                .andExpect(jsonPath("$.assignedToEmail").value("analyst@alpha.local"))
                .andExpect(jsonPath("$.resolvedAt").doesNotExist());

        // 3. Transition INVESTIGATING -> RESOLVED and verify resolvedAt is populated
        UpdateIncidentRequest resolveReq = new UpdateIncidentRequest(
                null,
                null,
                null,
                IncidentStatus.RESOLVED,
                null,
                null
        );

        mockMvc.perform(patch("/api/v1/incidents/" + incidentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty());
    }

    @Test
    @DisplayName("SX-0603: Add investigation notes and inspect chronological timeline")
    void testInvestigationNotesAndTimeline() throws Exception {
        String token = obtainToken("analyst@alpha.local", "DevAnalyst123!");

        CreateIncidentRequest createReq = new CreateIncidentRequest(
                "Malware Outbreak Lab Host",
                "Suspected ransomware dropper detected on host-01",
                "CRITICAL",
                null,
                null,
                List.of()
        );

        String createRes = mockMvc.perform(post("/api/v1/incidents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String incidentId = objectMapper.readTree(createRes).get("id").asText();

        // Add 2 investigation notes
        AddIncidentNoteRequest note1 = new AddIncidentNoteRequest("Isolated host-01 network port and dumped memory image.");
        mockMvc.perform(post("/api/v1/incidents/" + incidentId + "/notes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(note1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value(note1.body()))
                .andExpect(jsonPath("$.authorEmail").value("analyst@alpha.local"));

        AddIncidentNoteRequest note2 = new AddIncidentNoteRequest("Identified C2 destination IP as 198.51.100.99.");
        mockMvc.perform(post("/api/v1/incidents/" + incidentId + "/notes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(note2)))
                .andExpect(status().isCreated());

        // Verify notes retrieval
        mockMvc.perform(get("/api/v1/incidents/" + incidentId + "/notes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Verify timeline contains both CREATED and NOTE_ADDED events
        String timelineJson = mockMvc.perform(get("/api/v1/incidents/" + incidentId + "/timeline")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(timelineJson).contains("INCIDENT_CREATED");
        assertThat(timelineJson).contains("NOTE_ADDED");
    }

    @Test
    @DisplayName("SX-0604: Verify audit logging for incident mutations and cross-tenant access denial")
    void testIncidentAuditAndCrossTenantIsolation() throws Exception {
        String alphaToken = obtainToken("analyst@alpha.local", "DevAnalyst123!");
        String betaToken = obtainToken("analyst@beta.local", "DevBetaAnalyst123!");

        // 1. Create incident under Tenant Alpha
        CreateIncidentRequest createReq = new CreateIncidentRequest(
                "Tenant Alpha Secret Incident",
                "Confidential internal investigation",
                "HIGH",
                null,
                null,
                List.of()
        );

        String createRes = mockMvc.perform(post("/api/v1/incidents")
                        .header("Authorization", "Bearer " + alphaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String alphaIncidentId = objectMapper.readTree(createRes).get("id").asText();

        // 2. Verify audit log entry exists
        List<AuditLog> auditLogs = auditLogRepository.findByActorEmailOrderByCreatedAtDesc("analyst@alpha.local");
        boolean hasIncidentAudit = auditLogs.stream()
                .anyMatch(a -> a.getAction().equals("INCIDENT_CREATED") && a.getResourceId().equals(alphaIncidentId));
        assertThat(hasIncidentAudit).isTrue();

        // 3. Tenant Beta list incidents -> must not see Tenant Alpha incident
        String betaListJson = mockMvc.perform(get("/api/v1/incidents")
                        .header("Authorization", "Bearer " + betaToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(betaListJson).doesNotContain(alphaIncidentId);
        assertThat(betaListJson).doesNotContain("Tenant Alpha Secret Incident");

        // 4. Tenant Beta direct GET -> 404
        mockMvc.perform(get("/api/v1/incidents/" + alphaIncidentId)
                        .header("Authorization", "Bearer " + betaToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("not_found_error"));

        // 5. Tenant Beta direct PATCH -> 404
        UpdateIncidentRequest updateReq = new UpdateIncidentRequest(
                "Hacked by Beta",
                null,
                null,
                IncidentStatus.CLOSED,
                null,
                null
        );
        mockMvc.perform(patch("/api/v1/incidents/" + alphaIncidentId)
                        .header("Authorization", "Bearer " + betaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        // 6. Tenant Beta add note -> 404
        AddIncidentNoteRequest noteReq = new AddIncidentNoteRequest("Unauthorized note from Beta");
        mockMvc.perform(post("/api/v1/incidents/" + alphaIncidentId + "/notes")
                        .header("Authorization", "Bearer " + betaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noteReq)))
                .andExpect(status().isNotFound());
    }
}
