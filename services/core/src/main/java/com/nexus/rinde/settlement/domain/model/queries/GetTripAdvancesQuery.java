package com.nexus.rinde.settlement.domain.model.queries;

import java.util.UUID;

/** Consulta los adelantos registrados para un viaje de la empresa. */
public record GetTripAdvancesQuery(UUID tenantId, UUID tripId) {}
