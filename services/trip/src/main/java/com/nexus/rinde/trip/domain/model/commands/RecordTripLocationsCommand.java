package com.nexus.rinde.trip.domain.model.commands;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Ordena registrar un lote de ubicaciones capturadas durante un viaje en ruta. */
public record RecordTripLocationsCommand(
    UUID tenantId, UUID tripId, UUID userId, List<LocationSample> points) {

  /** Muestra puntual enviada por el dispositivo del conductor. */
  public record LocationSample(
      BigDecimal latitude, BigDecimal longitude, BigDecimal accuracyMeters, Instant recordedAt) {}
}
