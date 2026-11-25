package com.nexus.rinde.fleet.application.internal.outboundservices.acl;

import java.time.Instant;
import java.util.UUID;

/** Resumen de incidencia recibido desde Incident para el historial del vehículo. */
public record ExternalIncidentSummary(
    UUID id,
    UUID tripId,
    UUID vehicleId,
    String type,
    String description,
    String status,
    Instant reportedAt) {}
