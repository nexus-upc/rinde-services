package com.nexus.rinde.iam.interfaces.rest;

import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexus.rinde.iam.domain.model.events.UserInvited;
import com.nexus.rinde.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class Sprint4FoundationsIntegrationTest extends AbstractIntegrationTest {

  @Test
  void profileAndCompanyEndpointsEnforceRolesReturn501AndAppearInOpenApi() throws Exception {
    api.registerAndVerifyTenant("20123456789", "admin@andes.pe", "ClaveAdmin2026");
    String adminToken = api.signInForToken("admin@andes.pe", "ClaveAdmin2026");
    api.inviteUser(adminToken, "Luis Quispe", "driver@andes.pe", "DRIVER");
    String inviteToken = capturedEvents.last(UserInvited.class).orElseThrow().invitationToken();
    api.definePassword(inviteToken, "ClaveDriver2026");
    String driverToken = api.signInForToken("driver@andes.pe", "ClaveDriver2026");

    mockMvc
        .perform(get("/api/v1/tenants/current").header("Authorization", "Bearer " + driverToken))
        .andExpect(status().isNotImplemented())
        .andExpect(jsonPath("$.detail").value("Funcionalidad pendiente de implementar en S4-28."));
    mockMvc.perform(get("/api/v1/tenants/current")).andExpect(status().isUnauthorized());

    mockMvc
        .perform(
            patch("/api/v1/tenants/current")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tradeName\":\"Andes SAC\"}"))
        .andExpect(status().isNotImplemented())
        .andExpect(jsonPath("$.detail").value("Funcionalidad pendiente de implementar en S4-28."));
    mockMvc
        .perform(
            patch("/api/v1/tenants/current")
                .header("Authorization", "Bearer " + driverToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tradeName\":\"Andes SAC\"}"))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            patch("/api/v1/users/me")
                .header("Authorization", "Bearer " + driverToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Luis Quispe Mamani\"}"))
        .andExpect(status().isNotImplemented())
        .andExpect(jsonPath("$.detail").value("Funcionalidad pendiente de implementar en S4-28."));
    mockMvc
        .perform(
            patch("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Luis Quispe Mamani\"}"))
        .andExpect(status().isUnauthorized());

    mockMvc
        .perform(
            post("/api/v1/users/me/password")
                .header("Authorization", "Bearer " + driverToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"currentPassword\":\"ClaveDriver2026\",\"newPassword\":\"ClaveNueva2026\"}"))
        .andExpect(status().isNotImplemented())
        .andExpect(jsonPath("$.detail").value("Funcionalidad pendiente de implementar en S4-28."));
    mockMvc
        .perform(
            post("/api/v1/users/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"currentPassword\":\"ClaveDriver2026\",\"newPassword\":\"ClaveNueva2026\"}"))
        .andExpect(status().isUnauthorized());

    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths", hasKey("/api/v1/tenants/current")))
        .andExpect(jsonPath("$.paths", hasKey("/api/v1/users/me")))
        .andExpect(jsonPath("$.paths", hasKey("/api/v1/users/me/password")));
  }
}
