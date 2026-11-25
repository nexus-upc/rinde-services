package com.nexus.rinde.dashboard.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** Resumen de incidencia abierta asociada a un viaje. */
public record OpenIncidentItem(
    @Schema(example = "3c589391-d520-4d01-838a-1a574c7d2bb6") UUID id,
    @Schema(example = "DELAY") String type,
    @Schema(example = "OPEN") String status,
    @Schema(example = "2026-11-20T13:20:00Z") Instant reportedAt) {}
