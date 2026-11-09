package com.nexus.rinde.incident.domain.model.commands;

import java.util.UUID;

/** Cierra una incidencia registrando la acción tomada y quién la atendió. */
public record CloseIncidentCommand(UUID tenantId, UUID incidentId, UUID closedBy, String action) {}
