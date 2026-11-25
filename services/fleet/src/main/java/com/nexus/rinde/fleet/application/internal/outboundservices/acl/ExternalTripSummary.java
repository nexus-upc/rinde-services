package com.nexus.rinde.fleet.application.internal.outboundservices.acl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Resumen de viaje recibido desde Trip para el historial del vehículo. */
public record ExternalTripSummary(
    UUID tripId,
    String code,
    String status,
    String origin,
    String destination,
    String cargoDescription,
    BigDecimal cargoWeightKg,
    LocalDate departureDate,
    UUID vehicleId,
    UUID driverId,
    Instant startedAt,
    Instant finishedAt) {}
