package com.nexus.rinde.settlement.domain.model.commands;

import java.util.UUID;

/** Vuelve a sumar los gastos aprobados y recalcula el balance del anticipo. */
public record RecalculateSettlementCommand(UUID tenantId, UUID settlementId) {}
