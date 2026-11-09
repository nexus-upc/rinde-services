package com.nexus.rinde.incident.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** Acción tomada y datos de quien cerró la incidencia. Nulo mientras no esté cerrada. */
public record IncidentResolutionResource(
    @Schema(example = "Se cambió la llanta de repuesto y el viaje continuó") String action,
    @Schema(example = "0b7d6e2a-4d5f-4a1e-9c3b-2f8e1a6d7c44") UUID closedBy,
    @Schema(example = "2026-11-09T15:05:00Z") Instant closedAt) {}
