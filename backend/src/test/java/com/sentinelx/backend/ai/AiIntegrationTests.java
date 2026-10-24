package com.sentinelx.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelx.backend.ai.dto.FlowScoreRequest;
import com.sentinelx.backend.identity.dto.LoginRequest;
import com.sentinelx.backend.incidents.dto.CreateIncidentRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiIntegrationTests {

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
    @DisplayName("SX-0802: AI Status endpoint returns gracefully when Python service is offline")
    void aiStatusReturnsOfflineGracefully() throws Exception {
        mockMvc.perform(get("/api/v1/ai/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.online").value(false))
                .andExpect(jsonPath("$.model_loaded").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("SX-0802: Flow scoring endpoint returns offline fallback without throwing exception")
    void scoreFlowReturnsFallbackWhenServiceOffline() throws Exception {
        String token = obtainToken("analyst@alpha.local", "DevAnalyst123!");

        FlowScoreRequest req = FlowScoreRequest.defaults();

        mockMvc.perform(post("/api/v1/ai/score-flow")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prediction").value("BENIGN"))
                .andExpect(jsonPath("$.is_threat").value(false))
                .andExpect(jsonPath("$.model_status").value("offline"))
                .andExpect(jsonPath("$.explanation").isNotEmpty());
    }

    @Test
    @DisplayName("SX-0807: Incident triage summarization returns structured template when AI is offline")
    void summarizeIncidentReturnsFallbackTemplate() throws Exception {
        String token = obtainToken("analyst@alpha.local", "DevAnalyst123!");

        // Create an incident first
        CreateIncidentRequest createReq = new CreateIncidentRequest(
                "Suspicious Outbound Tunneling Detected",
                "Multiple anomalous high-volume flows observed",
                "HIGH",
                null,
                null,
                List.of()
        );

        String createResp = mockMvc.perform(post("/api/v1/incidents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String incidentId = objectMapper.readTree(createResp).get("id").asText();

        // Query AI summary
        mockMvc.perform(post("/api/v1/ai/incidents/" + incidentId + "/summarize")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incident_id").value(incidentId))
                .andExpect(jsonPath("$.summary_markdown").isNotEmpty())
                .andExpect(jsonPath("$.suggested_actions").isArray())
                .andExpect(jsonPath("$.disclaimer").isNotEmpty());
    }
}
