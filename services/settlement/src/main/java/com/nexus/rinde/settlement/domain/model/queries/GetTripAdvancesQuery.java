package com.nexus.rinde.settlement.domain.model.queries;

import java.util.UUID;

/** Consulta los adelantos registrados para un viaje de la empresa, según el rol solicitante. */
public record GetTripAdvancesQuery(
    UUID tenantId, UUID tripId, UUID requesterUserId, String requesterRole) {}
