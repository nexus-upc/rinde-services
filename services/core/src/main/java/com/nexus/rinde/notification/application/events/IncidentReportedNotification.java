package com.nexus.rinde.notification.application.events;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Contrato propio de Notifications para recibir incident.reported sin importar el dominio de Incident. */
public record IncidentReportedNotification(
    UUID eventId,
    Instant occurredAt,
    UUID tenantId,
    UUID incidentId,
    UUID tripId,
    UUID vehicleId,
    UUID reportedBy,
    String incidentType) {

  public IncidentReportedNotification {
    Objects.requireNonNull(eventId, "Falta el identificador del evento.");
    Objects.requireNonNull(occurredAt, "Falta la fecha del evento.");
    Objects.requireNonNull(tenantId, "Falta la empresa.");
    Objects.requireNonNull(incidentId, "Falta la incidencia.");
    if (incidentType == null || incidentType.isBlank() || incidentType.length() > 60) {
      throw new IllegalArgumentException("El tipo de incidencia no es válido.");
    }
  }
}
