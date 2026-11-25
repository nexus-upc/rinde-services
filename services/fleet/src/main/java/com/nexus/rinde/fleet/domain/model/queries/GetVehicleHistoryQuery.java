package com.nexus.rinde.fleet.domain.model.queries;

import java.util.UUID;

/** Consulta el historial consolidado de un vehículo en un periodo opcional. */
public record GetVehicleHistoryQuery(UUID tenantId, UUID vehicleId, String from, String to) {}
