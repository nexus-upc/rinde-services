package com.nexus.rinde.incident.interfaces.acl;

import java.time.Instant;
import java.util.UUID;

/** Vista de una incidencia abierta recibida desde Incident Management. */
public record OpenIncidentSummary(
    UUID id,
    UUID tripId,
    UUID vehicleId,
    String type,
    String description,
    String status,
    Instant reportedAt) {}
