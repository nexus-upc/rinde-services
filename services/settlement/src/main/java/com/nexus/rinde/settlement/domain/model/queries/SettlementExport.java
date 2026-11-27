package com.nexus.rinde.settlement.domain.model.queries;

import com.nexus.rinde.settlement.domain.model.aggregates.Settlement;
import com.nexus.rinde.settlement.domain.model.valueobjects.ApprovedExpense;
import java.util.List;

/** Resultado de ExportSettlementQuery: la liquidación con sus gastos aprobados. */
public record SettlementExport(Settlement settlement, List<ApprovedExpense> approvedExpenses) {}
