package com.nexus.rinde.settlement.domain.model.valueobjects;

import com.nexus.rinde.settlement.domain.model.aggregates.Settlement;

/** Resultado del recálculo: la liquidación actualizada y los gastos que faltan por revisar. */
public record SettlementRecalculation(Settlement settlement, int pendingExpenseCount) {}
