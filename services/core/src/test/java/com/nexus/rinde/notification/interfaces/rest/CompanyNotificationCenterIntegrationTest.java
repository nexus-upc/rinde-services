package com.nexus.rinde.notification.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.CompanyNotificationEntity;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.NotificationReadId;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.CompanyNotificationRepository;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.NotificationReadRepository;
import com.nexus.rinde.support.AbstractIntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Prueba el listado paginado, las lecturas por usuario y los errores del centro con PostgreSQL. */
class CompanyNotificationCenterIntegrationTest extends AbstractIntegrationTest {

  @Autowired private CompanyNotificationRepository notifications;
  @Autowired private NotificationReadRepository reads;

  @Test
  void pagesNewestFirstAndKeepsTheFirstReadMomentForEachUser() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID adminId = UUID.randomUUID();
    UUID operatorId = UUID.randomUUID();
    String admin = tokens.issue(adminId, tenantId, "ADMINISTRATOR", "ana@andes.pe");
    String operator = tokens.issue(operatorId, tenantId, "OPERATIONS_MANAGER", "jorge@andes.pe");
    CompanyNotificationEntity older = save(tenantId, "2026-11-20T10:00:00Z");
    save(tenantId, "2026-11-21T10:00:00Z");
    CompanyNotificationEntity newest = save(tenantId, "2026-11-22T10:00:00Z");

    mockMvc
        .perform(get("/api/v1/notifications").param("size", "2").header("Authorization", "Bearer " + admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalItems").value(3))
        .andExpect(jsonPath("$.totalPages").value(2))
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.items[0].id").value(newest.getId().toString()));

    mockMvc
        .perform(patch("/api/v1/notifications/{id}/read", older.getId()).header("Authorization", "Bearer " + admin))
        .andExpect(status().isNoContent());
    Instant firstRead = readAt(older.getId(), adminId);
    mockMvc
        .perform(patch("/api/v1/notifications/{id}/read", older.getId()).header("Authorization", "Bearer " + admin))
        .andExpect(status().isNoContent());
    assertThat(readAt(older.getId(), adminId)).isEqualTo(firstRead);

    mockMvc
        .perform(get("/api/v1/notifications").param("unreadOnly", "true").header("Authorization", "Bearer " + admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalItems").value(2));
    mockMvc
        .perform(get("/api/v1/notifications/unread-count").header("Authorization", "Bearer " + operator))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count").value(3));

    mockMvc
        .perform(post("/api/v1/notifications/read-all").header("Authorization", "Bearer " + operator))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(get("/api/v1/notifications/unread-count").header("Authorization", "Bearer " + operator))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count").value(0));
    assertThat(readAt(older.getId(), adminId)).isEqualTo(firstRead);

    // Una empresa ajena no ve ni puede marcar el aviso de esta empresa.
    String otherTenantAdmin =
        tokens.issue(UUID.randomUUID(), UUID.randomUUID(), "ADMINISTRATOR", "admin@sur.pe");
    mockMvc
        .perform(patch("/api/v1/notifications/{id}/read", newest.getId()).header("Authorization", "Bearer " + otherTenantAdmin))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("La notificación no existe."));
  }

  @Test
  void rejectsInvalidPagingAndTypeFilters() throws Exception {
    String admin =
        tokens.issue(UUID.randomUUID(), UUID.randomUUID(), "ADMINISTRATOR", "ana@andes.pe");

    mockMvc
        .perform(get("/api/v1/notifications").param("page", "-1").header("Authorization", "Bearer " + admin))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("El número de página no puede ser negativo."));
    mockMvc
        .perform(get("/api/v1/notifications").param("size", "0").header("Authorization", "Bearer " + admin))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("El tamaño de página debe estar entre 1 y 100."));
    mockMvc
        .perform(get("/api/v1/notifications").param("size", "101").header("Authorization", "Bearer " + admin))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("El tamaño de página debe estar entre 1 y 100."));
    mockMvc
        .perform(get("/api/v1/notifications").param("type", "CUALQUIERA").header("Authorization", "Bearer " + admin))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("El parámetro type contiene un tipo de aviso inválido."));
  }

  private CompanyNotificationEntity save(UUID tenantId, String createdAt) {
    return notifications.save(
        CompanyNotificationEntity.create(
            UUID.randomUUID(),
            tenantId,
            "EXPENSE_PENDING_REVIEW",
            "Gasto pendiente de revisión",
            "Se registró un gasto por PEN 10.00 para revisar.",
            "/expenses?tripId=" + UUID.randomUUID(),
            Instant.parse(createdAt)));
  }

  private Instant readAt(UUID notificationId, UUID userId) {
    return reads.findById(new NotificationReadId(notificationId, userId)).orElseThrow().getReadAt();
  }
}
