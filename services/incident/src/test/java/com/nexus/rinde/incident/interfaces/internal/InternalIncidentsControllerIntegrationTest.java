package com.nexus.rinde.incident.interfaces.internal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexus.rinde.incident.domain.model.aggregates.Incident;
import com.nexus.rinde.incident.domain.model.commands.ReportIncidentCommand;
import com.nexus.rinde.incident.domain.model.valueobjects.IncidentType;
import com.nexus.rinde.incident.domain.model.valueobjects.TripSnapshot;
import com.nexus.rinde.incident.infrastructure.persistence.jpa.repositories.IncidentRepository;
import com.nexus.rinde.support.AbstractIntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Pruebas de integración del controlador interno de incidencias. */
class InternalIncidentsControllerIntegrationTest extends AbstractIntegrationTest {

  @Autowired private IncidentRepository incidentRepository;

  @Test
  @DisplayName("Lista todas las incidencias del vehículo en cualquier estado y ordenadas por fecha")
  void listsAllIncidentsByVehicleInDescendingOrder() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID otherTenantId = UUID.randomUUID();
    UUID vehicleA = UUID.randomUUID();
    UUID vehicleB = UUID.randomUUID();
    Instant base = Instant.now();

    Incident open =
        report(tenantId, vehicleA, IncidentType.DELAY, "Retraso en garita", base.minusSeconds(300));
    Incident inProgress =
        report(
            tenantId,
            vehicleA,
            IncidentType.OTHER,
            "Revisión de documentos",
            base.minusSeconds(200));
    inProgress.startAttention();
    Incident closed =
        report(
            tenantId,
            vehicleA,
            IncidentType.BREAKDOWN,
            "Falla en el freno neumático",
            base.minusSeconds(100));
    closed.close(UUID.randomUUID(), "Se reemplazó la válvula", base.minusSeconds(50));

    Incident otherVehicle =
        report(tenantId, vehicleB, IncidentType.DELAY, "Otra unidad", base.minusSeconds(10));
    Incident otherTenant =
        report(otherTenantId, vehicleA, IncidentType.DELAY, "Otra empresa", base.minusSeconds(5));

    incidentRepository.saveAllAndFlush(
        java.util.List.of(open, inProgress, closed, otherVehicle, otherTenant));

    mockMvc
        .perform(
            get("/api/internal/v1/incidents/tenants/{tenantId}", tenantId)
                .param("vehicleId", vehicleA.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].id").value(closed.getId().toString()))
        .andExpect(jsonPath("$[0].status").value("CLOSED"))
        .andExpect(jsonPath("$[0].description").value("Falla en el freno neumático"))
        .andExpect(jsonPath("$[1].id").value(inProgress.getId().toString()))
        .andExpect(jsonPath("$[1].status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$[2].id").value(open.getId().toString()))
        .andExpect(jsonPath("$[2].status").value("OPEN"));

    mockMvc
        .perform(get("/api/internal/v1/incidents/tenants/{tenantId}/open", tenantId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].description").value("Otra unidad"));
  }

  @Test
  @DisplayName("Exige el parámetro vehicleId al consultar por vehículo")
  void requiresVehicleIdQueryParam() throws Exception {
    mockMvc
        .perform(get("/api/internal/v1/incidents/tenants/{tenantId}", UUID.randomUUID()))
        .andExpect(status().isBadRequest());
  }

  private Incident report(
      UUID tenantId, UUID vehicleId, IncidentType type, String description, Instant reportedAt) {
    UUID tripId = UUID.randomUUID();
    UUID driverId = UUID.randomUUID();
    TripSnapshot trip = new TripSnapshot(tripId, "IN_ROUTE", vehicleId, driverId);
    ReportIncidentCommand command =
        new ReportIncidentCommand(
            tenantId, tripId, UUID.randomUUID(), type, description, null, null);
    return Incident.report(trip, driverId, command, reportedAt);
  }
}
