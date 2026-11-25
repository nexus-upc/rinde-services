package com.nexus.rinde.incident.interfaces.rest;

import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexus.rinde.support.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class Sprint4FoundationsIntegrationTest extends AbstractIntegrationTest {

  @Test
  void incidentEvidenceEndpointsEnforceRolesReturn501AndAppearInOpenApi() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID incidentId = UUID.randomUUID();
    UUID evidenceId = UUID.randomUUID();
    String driverToken = tokens.issue(UUID.randomUUID(), tenantId, "DRIVER", "driver@rinde.pe");
    String adminToken = tokens.issue(UUID.randomUUID(), tenantId, "ADMINISTRATOR", "admin@rinde.pe");
    MockMultipartFile file =
        new MockMultipartFile("file", "foto.jpg", "image/jpeg", new byte[] {1, 2, 3});

    mockMvc
        .perform(
            multipart("/api/v1/incidents/{id}/evidences", incidentId)
                .file(file)
                .header("Authorization", "Bearer " + driverToken))
        .andExpect(status().isNotImplemented())
        .andExpect(jsonPath("$.detail").value("Funcionalidad pendiente de implementar en S4-14."));

    mockMvc
        .perform(
            multipart("/api/v1/incidents/{id}/evidences", incidentId)
                .file(file)
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            get("/api/v1/incidents/{id}/evidences/{evidenceId}/content", incidentId, evidenceId)
                .header("Authorization", "Bearer " + driverToken))
        .andExpect(status().isNotImplemented())
        .andExpect(jsonPath("$.detail").value("Funcionalidad pendiente de implementar en S4-14."));

    mockMvc
        .perform(
            get("/api/v1/incidents/{id}/evidences/{evidenceId}/content", incidentId, evidenceId))
        .andExpect(status().isUnauthorized());

    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths", hasKey("/api/v1/incidents/{id}/evidences")))
        .andExpect(
            jsonPath("$.paths", hasKey("/api/v1/incidents/{id}/evidences/{evidenceId}/content")));
  }
}
