package com.nexus.rinde.incident.domain.model.queries;

import java.util.UUID;

/** Recupera el archivo binario de evidencia de una incidencia verificando permisos de acceso. */
public record GetIncidentEvidenceContentQuery(
    UUID tenantId, UUID incidentId, UUID evidenceId, UUID userId, String role) {}
