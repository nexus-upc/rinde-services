package com.nexus.rinde.expense.domain.services;

import com.nexus.rinde.expense.domain.model.aggregates.Expense;
import com.nexus.rinde.expense.domain.model.queries.GetEvidenceContentQuery;
import com.nexus.rinde.expense.domain.model.queries.GetExpenseByIdQuery;
import com.nexus.rinde.expense.domain.model.queries.GetExpensesByTripQuery;
import com.nexus.rinde.expense.domain.model.valueobjects.EvidenceContent;
import java.util.List;

/** Contrato de casos de uso de consulta de gastos. */
public interface ExpenseQueryService {

  List<Expense> handle(GetExpensesByTripQuery query);

  Expense handle(GetExpenseByIdQuery query);

  EvidenceContent handle(GetEvidenceContentQuery query);
}
