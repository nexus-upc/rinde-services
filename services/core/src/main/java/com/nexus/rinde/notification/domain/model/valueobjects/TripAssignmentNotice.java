package com.nexus.rinde.notification.domain.model.valueobjects;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Aviso de asignación preparado a partir del evento público TripAssigned. */
public record TripAssignmentNotice(
    UUID eventId,
    UUID tenantId,
    UUID tripId,
    UUID driverId,
    String tripCode,
    String destination,
    LocalDate departureDate,
    Instant occurredAt,
    String message,
    List<String> deviceTokens) {

  public TripAssignmentNotice {
    deviceTokens = deviceTokens == null ? List.of() : List.copyOf(deviceTokens);
  }

  public TripAssignmentNotice(
      UUID eventId,
      UUID tenantId,
      UUID tripId,
      UUID driverId,
      String tripCode,
      String destination,
      LocalDate departureDate,
      Instant occurredAt,
      String message) {
    this(
        eventId,
        tenantId,
        tripId,
        driverId,
        tripCode,
        destination,
        departureDate,
        occurredAt,
        message,
        List.of());
  }

  public static TripAssignmentNotice from(
      UUID eventId,
      UUID tenantId,
      UUID tripId,
      UUID driverId,
      String tripCode,
      String destination,
      LocalDate departureDate,
      Instant occurredAt) {
    String message =
        "Viaje "
            + tripCode
            + " asignado. Destino: "
            + destination
            + ". Fecha de salida: "
            + departureDate
            + ".";
    return new TripAssignmentNotice(
        eventId,
        tenantId,
        tripId,
        driverId,
        tripCode,
        destination,
        departureDate,
        occurredAt,
        message);
  }

  /** Devuelve una copia del aviso con los tokens de dispositivo del destinatario. */
  public TripAssignmentNotice withDeviceTokens(List<String> tokens) {
    return new TripAssignmentNotice(
        eventId,
        tenantId,
        tripId,
        driverId,
        tripCode,
        destination,
        departureDate,
        occurredAt,
        message,
        tokens);
  }
}
