package com.nexus.rinde.settlement.domain.model.commands;

import java.util.UUID;

/** Cierra la liquidación: suma gastos, bloquea anticipo y publica SettlementClosed. */
public record CloseSettlementCommand(UUID tenantId, UUID settlementId, UUID closedBy) {}
