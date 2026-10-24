package com.sentinelx.backend.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelx.backend.audit.entity.AuditLog;
import com.sentinelx.backend.audit.repository.AuditLogRepository;
import com.sentinelx.backend.identity.dto.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    @DisplayName("SX-0202: Successful login returns JWT token and records audit log")
    void testLoginSuccess() throws Exception {
        LoginRequest request = new LoginRequest("analyst@alpha.local", "DevAnalyst123!");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value("analyst@alpha.local"))
                .andExpect(jsonPath("$.role").value("ANALYST"))
                .andExpect(jsonPath("$.tenantSlug").value("alpha-lab"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).isNotEmpty();

        // Verify audit log
        List<AuditLog> auditLogs = auditLogRepository.findByActorEmailOrderByCreatedAtDesc("analyst@alpha.local");
        assertThat(auditLogs).isNotEmpty();
        assertThat(auditLogs.get(0).getAction()).isEqualTo("AUTH_LOGIN_SUCCESS");
    }

    @Test
    @DisplayName("SX-0202: Failed login with invalid password returns 401 and records audit failure")
    void testLoginFailureWrongPassword() throws Exception {
        LoginRequest request = new LoginRequest("analyst@alpha.local", "WrongPassword999!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("authentication_error"))
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));

        List<AuditLog> auditLogs = auditLogRepository.findByActorEmailOrderByCreatedAtDesc("analyst@alpha.local");
        assertThat(auditLogs).anyMatch(log -> log.getAction().equals("AUTH_LOGIN_FAILURE"));
    }

    @Test
    @DisplayName("SX-0202: Unauthenticated request to protected route returns 401")
    void testUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("authentication_error"));
    }

    @Test
    @DisplayName("SX-0202: Authenticated request to /api/v1/auth/me returns current user profile")
    void testAuthenticatedMeEndpoint() throws Exception {
        // Step 1: Login to get token
        LoginRequest login = new LoginRequest("admin@alpha.local", "DevAdmin123!");
        String responseJson = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(responseJson).get("token").asText();

        // Step 2: Access /api/v1/auth/me with Bearer token
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@alpha.local"))
                .andExpect(jsonPath("$.role").value("TENANT_ADMIN"))
                .andExpect(jsonPath("$.tenantSlug").value("alpha-lab"));
    }

    @Test
    @DisplayName("SX-0205: Logout endpoint records audit log successfully")
    void testLogoutRecordsAudit() throws Exception {
        LoginRequest login = new LoginRequest("analyst@alpha.local", "DevAnalyst123!");
        String responseJson = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(responseJson).get("token").asText();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully logged out"));

        List<AuditLog> logs = auditLogRepository.findByActorEmailOrderByCreatedAtDesc("analyst@alpha.local");
        assertThat(logs).anyMatch(log -> log.getAction().equals("AUTH_LOGOUT"));
    }
}
