package com.nexus.rinde.dashboard.interfaces.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexus.rinde.support.AbstractIntegrationTest;
import com.nexus.rinde.support.RelativeDates;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class Sprint4FoundationsIntegrationTest extends AbstractIntegrationTest {

  @Test
  void dashboardChartsAndNotificationEndpointsReturn501AndEnforceRoles() throws Exception {
    UUID tenantId = UUID.randomUUID();
    String adminToken = tokens.issue(UUID.randomUUID(), tenantId, "ADMINISTRATOR", "admin@rinde.pe");
    String driverToken = tokens.issue(UUID.randomUUID(), tenantId, "DRIVER", "driver@rinde.pe");
    UUID notificationId = UUID.randomUUID();

    // C9: Dashboard charts
    mockMvc
        .perform(get("/api/v1/dashboard/charts").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNotImplemented())
        .andExpect(jsonPath("$.detail").value("Funcionalidad pendiente de implementar en S4-23."));
    mockMvc
        .perform(get("/api/v1/dashboard/charts").header("Authorization", "Bearer " + driverToken))
        .andExpect(status().isForbidden());

    // C5: Driver push devices
    mockMvc
        .perform(
            post("/api/v1/notifications/devices")
                .header("Authorization", "Bearer " + driverToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"fcm-token-01\",\"platform\":\"ANDROID\"}"))
        .andExpect(status().isNotImplemented())
        .andExpect(jsonPath("$.detail").value("Funcionalidad pendiente de implementar en S4-24."));
    mockMvc
        .perform(
            post("/api/v1/notifications/devices")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"fcm-token-01\",\"platform\":\"ANDROID\"}"))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            delete("/api/v1/notifications/devices/{token}", "fcm-token-01")
                .header("Authorization", "Bearer " + driverToken))
        .andExpect(status().isNotImplemented())
        .andExpect(jsonPath("$.detail").value("Funcionalidad pendiente de implementar en S4-24."));
    mockMvc
        .perform(
            delete("/api/v1/notifications/devices/{token}", "fcm-token-01")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isForbidden());

    // C6: Company notification center
    mockMvc
        .perform(get("/api/v1/notifications").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNotImplemented())
        .andExpect(jsonPath("$.detail").value("Funcionalidad pendiente de implementar en S4-25."));
    mockMvc
        .perform(get("/api/v1/notifications").header("Authorization", "Bearer " + driverToken))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            get("/api/v1/notifications/unread-count")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNotImplemented())
        .andExpect(jsonPath("$.detail").value("Funcionalidad pendiente de implementar en S4-25."));
    mockMvc
        .perform(
            get("/api/v1/notifications/unread-count")
                .header("Authorization", "Bearer " + driverToken))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            patch("/api/v1/notifications/{id}/read", notificationId)
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNotImplemented())
        .andExpect(jsonPath("$.detail").value("Funcionalidad pendiente de implementar en S4-25."));
    mockMvc
        .perform(
            patch("/api/v1/notifications/{id}/read", notificationId)
                .header("Authorization", "Bearer " + driverToken))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/api/v1/notifications/read-all").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNotImplemented())
        .andExpect(jsonPath("$.detail").value("Funcionalidad pendiente de implementar en S4-25."));
    mockMvc
        .perform(
            post("/api/v1/notifications/read-all")
                .header("Authorization", "Bearer " + driverToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void tripSummaryRemains200WithCompatibleSprint4Fields() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID tripId = UUID.randomUUID();
    contextDoubles.registerTenant(tenantId, "ops@rinde.pe");
    contextDoubles.scheduleTrip(
        tenantId, tripId, "Lima", "Trujillo", "Carga seca", LocalDate.parse(RelativeDates.resolve("HOY+2")));
    String opsToken =
        tokens.issue(UUID.randomUUID(), tenantId, "OPERATIONS_MANAGER", "ops@rinde.pe");

    mockMvc
        .perform(
            get("/api/v1/dashboard/trips/{tripId}/summary", tripId)
                .header("Authorization", "Bearer " + opsToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tripId").value(tripId.toString()))
        .andExpect(jsonPath("$.expenseCountsByStatus.REGISTERED").value(0))
        .andExpect(jsonPath("$.approvedTotal").value(0))
        .andExpect(jsonPath("$.pendingReviewCount").value(0))
        .andExpect(jsonPath("$.openIncidents").isArray())
        .andExpect(jsonPath("$.partial").value(false))
        .andExpect(jsonPath("$.unavailableSources").isArray());
  }
}
