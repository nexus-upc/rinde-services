package com.nexus.rinde.trip.domain.model.queries;

import java.util.UUID;

/** Consulta la última ubicación registrada de cada viaje en ruta de la empresa. */
public record GetLatestTripLocationsQuery(UUID tenantId) {}
