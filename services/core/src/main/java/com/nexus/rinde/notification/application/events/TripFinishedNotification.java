package com.nexus.rinde.notification.application.events;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Contrato propio de Notifications para recibir trip.finished sin importar el dominio de Trip. */
public record TripFinishedNotification(
    UUID eventId,
    Instant occurredAt,
    UUID tenantId,
    UUID tripId,
    UUID vehicleId,
    UUID driverId) {

  public TripFinishedNotification {
    Objects.requireNonNull(eventId, "Falta el identificador del evento.");
    Objects.requireNonNull(occurredAt, "Falta la fecha del evento.");
    Objects.requireNonNull(tenantId, "Falta la empresa.");
    Objects.requireNonNull(tripId, "Falta el viaje.");
  }
}
