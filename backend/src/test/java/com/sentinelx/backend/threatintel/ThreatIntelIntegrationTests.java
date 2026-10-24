package com.sentinelx.backend.threatintel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelx.backend.identity.dto.LoginRequest;
import com.sentinelx.backend.ingestion.dto.EventIngestRequest;
import com.sentinelx.backend.threatintel.dto.CreateIocRequest;
import com.sentinelx.backend.threatintel.dto.IocLookupRequest;
import com.sentinelx.backend.threatintel.model.IocType;
import com.sentinelx.backend.threatintel.model.ThreatSeverity;
import com.sentinelx.backend.threatintel.model.ThreatType;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ThreatIntelIntegrationTests {

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

    private void ingestEvent(String token, String destinationIp, String sourceIp, String message) throws Exception {
        EventIngestRequest request = new EventIngestRequest(
                Instant.now(),
                "network-tap",
                "core-fw-01",
                "outbound_connection",
                "HIGH",
                "edge-router",
                sourceIp != null ? sourceIp : "10.0.4.50",
                destinationIp,
                "system",
                "sshd",
                2201,
                message != null ? message : "Outbound TCP session established",
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
    @DisplayName("SX-0901: Ingested event contacting known seed IOC (198.51.100.23) triggers Threat Intel alert")
    void positiveDetectionOnKnownIoc() throws Exception {
        String token = obtainToken("analyst@alpha.local", "DevAnalyst123!");

        // Ingest event communicating with seed Cobalt Strike C2 (198.51.100.23)
        ingestEvent(token, "198.51.100.23", "10.0.1.15", "Outbound beacon to C2 node");

        // Verify alert was created with rule RULE_THREAT_INTEL_IOC_MATCH
        mockMvc.perform(get("/api/v1/alerts")
                        .header("Authorization", "Bearer " + token)
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.title contains 'Threat Intel Match')]").exists())
                .andExpect(jsonPath("$.content[?(@.ruleKey == 'RULE_THREAT_INTEL_IOC_MATCH')]").exists());
    }

    @Test
    @DisplayName("SX-0901: Benign traffic does not trigger threat intelligence alert")
    void negativeDetectionOnBenignTraffic() throws Exception {
        String token = obtainToken("analyst@alpha.local", "DevAnalyst123!");

        // Ingest benign internal DNS query / traffic
        ingestEvent(token, "8.8.8.8", "10.0.1.20", "Normal external DNS query to Google");

        // Verify no false positive threat alert for 8.8.8.8
        mockMvc.perform(get("/api/v1/alerts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.title contains '8.8.8.8')]").doesNotExist());
    }

    @Test
    @DisplayName("SX-0901: Analyst can query stats, lookup IOC, and add custom indicator")
    void customIocCreationAndReputationLookup() throws Exception {
        String token = obtainToken("analyst@alpha.local", "DevAnalyst123!");

        // 1. Check stats endpoint
        mockMvc.perform(get("/api/v1/threat-intel/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIocs", greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$.feedStatus", is("HEALTHY")));

        // 2. Perform reputation lookup for seed indicator
        IocLookupRequest lookupReq = new IocLookupRequest("198.51.100.23");
        mockMvc.perform(post("/api/v1/threat-intel/lookup")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lookupReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched", is(true)))
                .andExpect(jsonPath("$.ioc.threatType", is("MALWARE_C2")));

        // 3. Create a custom tenant IOC
        CreateIocRequest createReq = new CreateIocRequest(
                IocType.IP,
                "198.51.100.99",
                ThreatType.RANSOMWARE,
                ThreatSeverity.CRITICAL,
                0.99,
                "Custom Incident Response Triage",
                "Observed exfiltration IP in targeted adversary simulation.",
                null,
                false
        );

        mockMvc.perform(post("/api/v1/threat-intel/iocs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.iocValue", is("198.51.100.99")))
                .andExpect(jsonPath("$.threatType", is("RANSOMWARE")));

        // 4. Ingest traffic to custom IOC and verify detection
        ingestEvent(token, "198.51.100.99", "10.0.1.44", "Direct TLS connection to 198.51.100.99");

        mockMvc.perform(get("/api/v1/alerts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.title contains '198.51.100.99')]").exists());
    }

    @Test
    @DisplayName("SX-0901: Querying IOCs with pagination and filters returns 200 OK")
    void testGetIocsPaginationAndFiltering() throws Exception {
        String token = obtainToken("analyst@alpha.local", "DevAnalyst123!");

        // 1. Without filters
        mockMvc.perform(get("/api/v1/threat-intel/iocs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())));

        // 2. With type filter
        mockMvc.perform(get("/api/v1/threat-intel/iocs")
                        .header("Authorization", "Bearer " + token)
                        .param("type", "IP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())));

        // 3. With search filter
        mockMvc.perform(get("/api/v1/threat-intel/iocs")
                        .header("Authorization", "Bearer " + token)
                        .param("search", "Cobalt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())));
    }

    @Test
    @DisplayName("SX-0901: Threat Intelligence endpoints strictly require authentication")
    void threatIntelRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/threat-intel/iocs"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/threat-intel/stats"))
                .andExpect(status().isUnauthorized());
    }
}
