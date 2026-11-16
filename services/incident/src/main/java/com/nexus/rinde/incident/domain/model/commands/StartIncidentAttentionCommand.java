package com.nexus.rinde.incident.domain.model.commands;

import java.util.UUID;

/** Pasa una incidencia abierta a atención. */
public record StartIncidentAttentionCommand(UUID tenantId, UUID incidentId) {}
