package com.nexus.rinde.incident.domain.model.commands;

import java.util.UUID;

/** Adjunta o reemplaza la evidencia binaria de una incidencia reportada por el conductor. */
public record AttachIncidentEvidenceCommand(
    UUID tenantId, UUID incidentId, UUID userId, String originalFilename, byte[] content) {}
