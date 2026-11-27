package com.nexus.rinde.settlement.domain.model.queries;

import java.util.UUID;

/** Pide los datos de la liquidación con sus gastos aprobados para exportar como CSV. */
public record ExportSettlementQuery(UUID tenantId, UUID settlementId) {}
