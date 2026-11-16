package com.nexus.rinde.incident.interfaces.rest.resources;

import com.nexus.rinde.incident.domain.model.valueobjects.IncidentStatus;
import com.nexus.rinde.incident.domain.model.valueobjects.IncidentType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** Representación pública de una incidencia. */
public record IncidentResource(
    @Schema(example = "3c589391-d520-4d01-838a-1a574c7d2bb6") UUID id,
    @Schema(example = "87460614-42ea-4cd5-9de1-8f38d9496434") UUID tripId,
    @Schema(example = "d6b9a0e4-1c2f-4f3a-9a77-3e1d2b5c8f10") UUID vehicleId,
    @Schema(example = "764a4968-403f-40d7-9172-ba637d10fb16") UUID reportedBy,
    @Schema(example = "BREAKDOWN") IncidentType type,
    @Schema(example = "Falla en el freno neumático") String description,
    @Schema(example = "https://storage.rinde.pe/evidences/incidencia-1.jpg", nullable = true)
        String evidenceUrl,
    @Schema(example = "OPEN") IncidentStatus status,
    @Schema(example = "2026-11-09T13:20:00Z") Instant reportedAt,
    @Schema(nullable = true) IncidentResolutionResource resolution,
    Instant updatedAt) {}
