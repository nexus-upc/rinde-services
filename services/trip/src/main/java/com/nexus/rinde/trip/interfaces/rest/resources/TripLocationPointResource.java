package com.nexus.rinde.trip.interfaces.rest.resources;

import com.nexus.rinde.trip.interfaces.rest.validation.NotFutureLocationTime;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

/** Punto geográfico capturado por el dispositivo del conductor. */
public record TripLocationPointResource(
    @Schema(example = "-12.046374")
        @NotNull(message = "La latitud es obligatoria.")
        @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90.")
        @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90.")
        BigDecimal latitude,
    @Schema(example = "-77.042793")
        @NotNull(message = "La longitud es obligatoria.")
        @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180.")
        @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180.")
        BigDecimal longitude,
    @Schema(example = "12.50", nullable = true)
        @DecimalMin(value = "0.0", message = "La precisión debe ser mayor o igual a 0.")
        BigDecimal accuracyMeters,
    @Schema(example = "2026-11-28T14:30:00Z")
        @NotNull(message = "La fecha de captura es obligatoria.")
        @NotFutureLocationTime
        Instant recordedAt) {}
