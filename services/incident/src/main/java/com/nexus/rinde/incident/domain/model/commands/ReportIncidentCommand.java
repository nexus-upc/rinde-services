package com.nexus.rinde.incident.domain.model.commands;

import com.nexus.rinde.incident.domain.model.valueobjects.IncidentType;
import java.util.UUID;

/** Datos de un reporte de incidencia. El conductor se resuelve a partir del usuario autenticado. */
public record ReportIncidentCommand(
    UUID tenantId,
    UUID tripId,
    UUID reportedBy,
    IncidentType type,
    String description,
    String evidenceUrl,
    String idempotencyKey) {}
