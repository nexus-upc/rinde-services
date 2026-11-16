package com.nexus.rinde.settlement.application.internal.outboundservices.acl;

import com.nexus.rinde.expense.interfaces.acl.ExpenseContextFacade;
import com.nexus.rinde.expense.interfaces.acl.ExpenseSummary;
import com.nexus.rinde.settlement.domain.model.valueobjects.ApprovedExpense;
import com.nexus.rinde.settlement.domain.model.valueobjects.TripExpenseReview;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Adaptador ACL que delega en ExpenseContextFacade para obtener los gastos del viaje. */
@Service
public class ExternalExpenseService {

  private static final String APPROVED = "APPROVED";
  private static final String OBSERVED = "OBSERVED";

  private final ExpenseContextFacade expenseContextFacade;

  public ExternalExpenseService(ExpenseContextFacade expenseContextFacade) {
    this.expenseContextFacade = expenseContextFacade;
  }

  public List<ApprovedExpense> findApprovedExpensesByTripId(UUID tenantId, UUID tripId) {
    return expenseContextFacade.findApprovedExpensesByTripId(tenantId, tripId).stream()
        .map(ExternalExpenseService::toApprovedExpense)
        .toList();
  }

  /** Gastos aprobados del viaje y cantidad de gastos que no están aprobados ni observados. */
  public TripExpenseReview reviewByTripId(UUID tenantId, UUID tripId) {
    List<ExpenseSummary> expenses = expenseContextFacade.findByTenantIdAndTripId(tenantId, tripId);
    List<ApprovedExpense> approved =
        expenses.stream()
            .filter(expense -> APPROVED.equals(expense.status()))
            .map(ExternalExpenseService::toApprovedExpense)
            .toList();
    int pending =
        (int)
            expenses.stream()
                .filter(
                    expense ->
                        !APPROVED.equals(expense.status()) && !OBSERVED.equals(expense.status()))
                .count();
    return new TripExpenseReview(approved, pending);
  }

  private static ApprovedExpense toApprovedExpense(ExpenseSummary summary) {
    return new ApprovedExpense(
        summary.id(),
        summary.category(),
        summary.amount(),
        summary.currency(),
        summary.expenseDate(),
        summary.status());
  }
}
