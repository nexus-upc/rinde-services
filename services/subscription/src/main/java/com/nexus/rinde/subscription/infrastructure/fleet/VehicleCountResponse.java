package com.nexus.rinde.subscription.infrastructure.fleet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Cuerpo de GET /api/internal/v1/fleet/tenants/{tenantId}/vehicles/count en Fleet.
 * DTO propio del microservicio Subscription, sin depender de clases de Fleet.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VehicleCountResponse(long count) {}
