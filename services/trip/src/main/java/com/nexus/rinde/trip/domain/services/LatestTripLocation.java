package com.nexus.rinde.trip.domain.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Última ubicación conocida de un viaje en ruta. */
public record LatestTripLocation(
    UUID tripId,
    String code,
    UUID vehicleId,
    BigDecimal latitude,
    BigDecimal longitude,
    BigDecimal accuracyMeters,
    Instant recordedAt) {}
