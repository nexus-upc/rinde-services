package com.nexus.rinde.fleet.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** Elemento unificado de historial operativo de un vehículo. */
public record VehicleHistoryItemResource(
    @Schema(example = "INCIDENT") String type,
    @Schema(example = "3c589391-d520-4d01-838a-1a574c7d2bb6") UUID id,
    @Schema(example = "2026-11-20T13:20:00Z") Instant date,
    @Schema(example = "Incidencia BREAKDOWN") String title,
    @Schema(nullable = true, example = "CLOSED") String status,
    @Schema(nullable = true, example = "Falla en el freno neumático") String detail) {}
