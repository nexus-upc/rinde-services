package com.nexus.rinde.incident.interfaces.rest.resources;

import com.nexus.rinde.incident.domain.model.valueobjects.IncidentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos del reporte de una incidencia durante un viaje en ruta. */
public record ReportIncidentResource(
    @Schema(example = "BREAKDOWN", allowableValues = {"BREAKDOWN", "DELAY", "OTHER"})
        @NotNull
        IncidentType type,
    @Schema(example = "Falla en el freno neumático") @NotBlank @Size(max = 500) String description,
    @Schema(example = "https://storage.rinde.pe/evidences/incidencia-1.jpg", nullable = true)
        @Size(max = 500)
        String evidenceUrl,
    @Schema(example = "INC-OFFLINE-0001", nullable = true)
        @Size(max = 64)
        String idempotencyKey) {}
