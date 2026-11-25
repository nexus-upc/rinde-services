package com.nexus.rinde.fleet.domain.model.valueobjects;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Historial unificado de un vehículo con trazabilidad de disponibilidad de fuentes remotas. */
public record VehicleHistory(
    UUID vehicleId,
    LocalDate nextMaintenanceDate,
    List<VehicleHistoryItem> items,
    boolean partial,
    List<String> unavailableSources) {

  /** Evento puntual dentro del historial del vehículo. */
  public record VehicleHistoryItem(
      String type, UUID id, Instant date, String title, String status, String detail) {}
}
