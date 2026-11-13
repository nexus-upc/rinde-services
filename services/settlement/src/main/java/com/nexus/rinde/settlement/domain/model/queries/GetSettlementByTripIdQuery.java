package com.nexus.rinde.settlement.domain.model.queries;

import java.util.UUID;

/** Consulta la liquidación de un viaje específico con aislamiento por empresa. */
public record GetSettlementByTripIdQuery(UUID tenantId, UUID tripId) {}
