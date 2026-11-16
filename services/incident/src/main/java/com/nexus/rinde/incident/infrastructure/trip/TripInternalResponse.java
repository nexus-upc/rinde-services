package com.nexus.rinde.incident.infrastructure.trip;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/**
 * Cuerpo de GET /api/internal/v1/trips/tenants/{tenantId}/{tripId} en Trip. DTO propio de Incident:
 * solo se leen los campos que necesita para validar un reporte.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TripInternalResponse(
    UUID tripId, String status, UUID vehicleId, UUID driverId) {}
