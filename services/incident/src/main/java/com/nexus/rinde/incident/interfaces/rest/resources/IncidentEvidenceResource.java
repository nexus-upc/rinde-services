package com.nexus.rinde.incident.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** Metadatos públicos de la evidencia adjunta a una incidencia. */
public record IncidentEvidenceResource(
    @Schema(example = "7d1c2f0e-3b8a-4d55-9a61-2e0f4c1b9a10") UUID id,
    @Schema(example = "foto-freno.jpg") String fileName,
    @Schema(example = "image/jpeg") String contentType,
    @Schema(example = "204800") long fileSizeBytes,
    @Schema(example = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
        String sha256,
    @Schema(
            example =
                "/api/v1/incidents/3c589391-d520-4d01-838a-1a574c7d2bb6/evidences/7d1c2f0e-3b8a-4d55-9a61-2e0f4c1b9a10/content")
        String contentUrl,
    @Schema(example = "2026-11-20T13:25:00Z") Instant uploadedAt) {}
