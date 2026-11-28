package com.nexus.rinde.dashboard.interfaces.rest;

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
  void dashboardChartsReturn501AndNotificationCenterAnswersAndEnforcesRoles() throws Exception {
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

    // C6: centro de notificaciones implementado en S4-25
    mockMvc
        .perform(get("/api/v1/notifications").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.totalItems").value(0));
    mockMvc
        .perform(
            get("/api/v1/notifications")
                .param("type", "INCIDENT_REPORTED,EXPENSE_PENDING_REVIEW")
                .param("unreadOnly", "true")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray());
    mockMvc
        .perform(
            get("/api/v1/notifications")
                .param("type", "INVALID_TYPE")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.detail").value("El parámetro type contiene un tipo de aviso inválido."));
    mockMvc
        .perform(get("/api/v1/notifications").header("Authorization", "Bearer " + driverToken))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            get("/api/v1/notifications/unread-count")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count").value(0));
    mockMvc
        .perform(
            get("/api/v1/notifications/unread-count")
                .header("Authorization", "Bearer " + driverToken))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            patch("/api/v1/notifications/{id}/read", notificationId)
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("La notificación no existe."));
    mockMvc
        .perform(
            patch("/api/v1/notifications/{id}/read", notificationId)
                .header("Authorization", "Bearer " + driverToken))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/api/v1/notifications/read-all").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());
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
