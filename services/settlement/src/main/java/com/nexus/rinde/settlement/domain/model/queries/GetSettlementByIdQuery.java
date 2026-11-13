package com.nexus.rinde.settlement.domain.model.queries;

import java.util.UUID;

/** Consulta la liquidación por su identificador único con aislamiento por empresa. */
public record GetSettlementByIdQuery(UUID tenantId, UUID settlementId) {}
