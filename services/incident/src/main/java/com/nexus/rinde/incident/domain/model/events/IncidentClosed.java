package com.nexus.rinde.incident.domain.model.events;

import com.nexus.rinde.shared.domain.model.events.IntegrationEvent;
import java.time.Instant;
import java.util.UUID;

/** Evento de integración publicado cuando una incidencia se cierra con su acción tomada. */
public record IncidentClosed(
    UUID eventId,
    Instant occurredAt,
    UUID tenantId,
    UUID incidentId,
    UUID tripId,
    UUID vehicleId,
    UUID closedBy,
    String action)
    implements IntegrationEvent {

  @Override
  public String type() {
    return "IncidentClosed";
  }
}
