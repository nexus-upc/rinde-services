package com.nexus.rinde.trip.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Última ubicación registrada de un viaje en ruta. */
public record LatestTripLocationResource(
    @Schema(example = "7b0e4f52-1c9a-4f3e-8d21-5a6c9e0b3d44") UUID tripId,
    @Schema(example = "TRP-000014") String code,
    @Schema(example = "deac3d8e-12d4-4a72-98f2-1e2fc4d0d230", nullable = true) UUID vehicleId,
    @Schema(example = "-12.046374", nullable = true) BigDecimal latitude,
    @Schema(example = "-77.042793", nullable = true) BigDecimal longitude,
    @Schema(example = "12.50", nullable = true) BigDecimal accuracyMeters,
    @Schema(example = "2026-11-28T14:30:00Z", nullable = true) Instant recordedAt) {}
