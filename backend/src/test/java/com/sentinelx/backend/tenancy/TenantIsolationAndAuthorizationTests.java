package com.sentinelx.backend.tenancy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelx.backend.identity.dto.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantIsolationAndAuthorizationTests {

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
    @DisplayName("SX-0203: Role-based authorization - ANALYST is denied access to PLATFORM_ADMIN endpoint (403)")
    void testRolePermissionForbidden() throws Exception {
        String analystToken = obtainToken("analyst@alpha.local", "DevAnalyst123!");

        mockMvc.perform(get("/api/v1/admin/platform-overview")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("authorization_error"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("SX-0203: Role-based authorization - PLATFORM_ADMIN is granted access to admin endpoint (200)")
    void testPlatformAdminAuthorized() throws Exception {
        String platformToken = obtainToken("platform@sentinelx.local", "DevPlatform123!");

        mockMvc.perform(get("/api/v1/admin/platform-overview")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("GLOBAL"));
    }

    @Test
    @DisplayName("SX-0204: Cross-tenant isolation - Tenant A and Tenant B users are strictly partitioned")
    void testCrossTenantIsolation() throws Exception {
        String alphaToken = obtainToken("analyst@alpha.local", "DevAnalyst123!");
        String betaToken = obtainToken("analyst@beta.local", "DevBetaAnalyst123!");

        String alphaResponse = mockMvc.perform(get("/api/v1/tenant/resource")
                        .header("Authorization", "Bearer " + alphaToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String betaResponse = mockMvc.perform(get("/api/v1/tenant/resource")
                        .header("Authorization", "Bearer " + betaToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String alphaTenantId = objectMapper.readTree(alphaResponse).get("tenantId").asText();
        String betaTenantId = objectMapper.readTree(betaResponse).get("tenantId").asText();

        // Ensure distinct tenant contexts
        assertThat(alphaTenantId).isNotEmpty();
        assertThat(betaTenantId).isNotEmpty();
        assertThat(alphaTenantId).isNotEqualTo(betaTenantId);
    }
}
