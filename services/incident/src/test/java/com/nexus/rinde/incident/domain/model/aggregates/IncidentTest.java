package com.nexus.rinde.incident.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexus.rinde.incident.domain.model.commands.ReportIncidentCommand;
import com.nexus.rinde.incident.domain.model.events.IncidentClosed;
import com.nexus.rinde.incident.domain.model.events.IncidentReported;
import com.nexus.rinde.incident.domain.model.valueobjects.IncidentStatus;
import com.nexus.rinde.incident.domain.model.valueobjects.IncidentType;
import com.nexus.rinde.incident.domain.model.valueobjects.TripSnapshot;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import com.nexus.rinde.shared.domain.exceptions.ConflictException;
import com.nexus.rinde.shared.domain.exceptions.ForbiddenOperationException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IncidentTest {

  private final UUID tenantId = UUID.randomUUID();
  private final UUID tripId = UUID.randomUUID();
  private final UUID vehicleId = UUID.randomUUID();
  private final UUID driverId = UUID.randomUUID();
  private final UUID driverUserId = UUID.randomUUID();

  @Test
  @DisplayName("Un reporte del conductor asignado en ruta nace abierto y publica IncidentReported")
  void shouldReportOpenIncidentAndRegisterEvent() {
    Incident incident = reportInRoute("Falla en el freno", "https://storage.rinde.pe/foto.jpg");

    assertThat(incident.getStatus()).isEqualTo(IncidentStatus.OPEN);
    assertThat(incident.getVehicleId()).isEqualTo(vehicleId);
    assertThat(incident.getTripId()).isEqualTo(tripId);
    assertThat(incident.getEvidenceUrl()).isEqualTo("https://storage.rinde.pe/foto.jpg");
    assertThat(incident.getResolution()).isNull();
    assertThat(incident.pendingDomainEvents())
        .singleElement()
        .isInstanceOf(IncidentReported.class)
        .satisfies(
            event -> {
              IncidentReported reported = (IncidentReported) event;
              assertThat(reported.type()).isEqualTo("IncidentReported");
              assertThat(reported.incidentType()).isEqualTo("BREAKDOWN");
              assertThat(reported.reportedBy()).isEqualTo(driverUserId);
            });
  }

  @Test
  @DisplayName("La evidencia es opcional y los textos vacíos no se guardan")
  void shouldAcceptReportWithoutEvidence() {
    Incident incident = reportInRoute("Retraso por cierre de vía", "   ");

    assertThat(incident.getEvidenceUrl()).isNull();
    assertThat(incident.getDescription()).isEqualTo("Retraso por cierre de vía");
  }

  @Test
  @DisplayName("Rechaza un reporte sin descripción")
  void shouldRejectReportWithoutDescription() {
    assertThatThrownBy(() -> reportInRoute("  ", null))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("La descripción de la incidencia es obligatoria.");
  }

  @Test
  @DisplayName("Rechaza un reporte de un conductor que no está asignado al viaje")
  void shouldRejectReportFromAnotherDriver() {
    TripSnapshot trip = new TripSnapshot(tripId, "IN_ROUTE", vehicleId, driverId);

    assertThatThrownBy(
            () ->
                Incident.report(
                    trip, UUID.randomUUID(), command("Falla", null, null), Instant.now()))
        .isInstanceOf(ForbiddenOperationException.class);
    assertThatThrownBy(() -> Incident.report(trip, null, command("Falla", null, null), Instant.now()))
        .isInstanceOf(ForbiddenOperationException.class);
  }

  @Test
  @DisplayName("Rechaza un reporte cuando el viaje no está en ruta")
  void shouldRejectReportWhenTripIsNotInRoute() {
    TripSnapshot trip = new TripSnapshot(tripId, "ASSIGNED", vehicleId, driverId);

    assertThatThrownBy(
            () -> Incident.report(trip, driverId, command("Falla", null, null), Instant.now()))
        .isInstanceOf(ConflictException.class)
        .hasMessage("Solo se puede reportar una incidencia cuando el viaje está en ruta.");
  }

  @Test
  @DisplayName("Una incidencia abierta pasa a atención una sola vez")
  void shouldMoveOpenIncidentToAttention() {
    Incident incident = reportInRoute("Falla", null);

    incident.startAttention();

    assertThat(incident.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
    assertThatThrownBy(incident::startAttention).isInstanceOf(ConflictException.class);
  }

  @Test
  @DisplayName("Cerrar registra la acción, quién la atendió y la fecha, y publica IncidentClosed")
  void shouldCloseIncidentWithResolutionAndEvent() {
    Incident incident = reportInRoute("Falla", null);
    UUID operator = UUID.randomUUID();
    Instant closedAt = Instant.parse("2026-11-09T10:00:00Z");

    incident.close(operator, "  Se cambió la llanta de repuesto  ", closedAt);

    assertThat(incident.getStatus()).isEqualTo(IncidentStatus.CLOSED);
    assertThat(incident.getResolution().getAction()).isEqualTo("Se cambió la llanta de repuesto");
    assertThat(incident.getResolution().getClosedBy()).isEqualTo(operator);
    assertThat(incident.getResolution().getClosedAt()).isEqualTo(closedAt);
    List<Object> events = incident.pendingDomainEvents();
    assertThat(events).hasSize(2);
    assertThat(events.get(1)).isInstanceOf(IncidentClosed.class);
  }

  @Test
  @DisplayName("No se cierra dos veces ni sin acción tomada")
  void shouldRejectInvalidClose() {
    Incident incident = reportInRoute("Falla", null);

    assertThatThrownBy(() -> incident.close(UUID.randomUUID(), " ", Instant.now()))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("La acción tomada es obligatoria.");

    incident.close(UUID.randomUUID(), "Revisado en taller", Instant.now());

    assertThatThrownBy(() -> incident.close(UUID.randomUUID(), "Otra acción", Instant.now()))
        .isInstanceOf(ConflictException.class)
        .hasMessage("La incidencia ya está cerrada.");
  }

  @Test
  @DisplayName("Una incidencia cerrada no puede pasar a atención")
  void shouldNotStartAttentionOnClosedIncident() {
    Incident incident = reportInRoute("Falla", null);
    incident.close(UUID.randomUUID(), "Atendida", Instant.now());

    assertThatThrownBy(incident::startAttention)
        .isInstanceOf(ConflictException.class)
        .hasMessage("Solo una incidencia abierta puede pasar a atención.");
  }

  private Incident reportInRoute(String description, String evidenceUrl) {
    TripSnapshot trip = new TripSnapshot(tripId, "IN_ROUTE", vehicleId, driverId);
    return Incident.report(
        trip, driverId, command(description, evidenceUrl, null), Instant.parse("2026-11-09T08:00:00Z"));
  }

  private ReportIncidentCommand command(String description, String evidenceUrl, String key) {
    return new ReportIncidentCommand(
        tenantId,
        tripId,
        driverUserId,
        IncidentType.BREAKDOWN,
        description,
        evidenceUrl,
        key);
  }
}
