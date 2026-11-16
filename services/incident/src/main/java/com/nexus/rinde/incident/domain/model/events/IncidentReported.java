package com.nexus.rinde.incident.domain.model.events;

import com.nexus.rinde.shared.domain.model.events.IntegrationEvent;
import java.time.Instant;
import java.util.UUID;

/** Evento de integración publicado cuando el conductor reporta una incidencia. */
public record IncidentReported(
    UUID eventId,
    Instant occurredAt,
    UUID tenantId,
    UUID incidentId,
    UUID tripId,
    UUID vehicleId,
    UUID reportedBy,
    String incidentType)
    implements IntegrationEvent {

  @Override
  public String type() {
    return "IncidentReported";
  }
}
