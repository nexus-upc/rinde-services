package com.nexus.rinde.expense.interfaces.acl;

import com.nexus.rinde.expense.domain.model.aggregates.Expense;
import com.nexus.rinde.expense.domain.model.valueobjects.ExpenseStatus;
import com.nexus.rinde.expense.infrastructure.persistence.jpa.repositories.ExpenseRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Implementación de la fachada que expone resúmenes de gastos filtrados por empresa. */
@Component
public class ExpenseContextFacadeImpl implements ExpenseContextFacade {

  private final ExpenseRepository expenseRepository;

  public ExpenseContextFacadeImpl(ExpenseRepository expenseRepository) {
    this.expenseRepository = expenseRepository;
  }

  @Override
  public List<ExpenseSummary> findApprovedExpensesByTripId(UUID tenantId, UUID tripId) {
    return expenseRepository
        .findByTenantIdAndTripIdAndStatus(tenantId, tripId, ExpenseStatus.APPROVED)
        .stream()
        .map(ExpenseContextFacadeImpl::toSummary)
        .toList();
  }

  @Override
  public List<ExpenseSummary> findByTenantIdAndTripId(UUID tenantId, UUID tripId) {
    return expenseRepository.findByTenantIdAndTripId(tenantId, tripId).stream()
        .map(ExpenseContextFacadeImpl::toSummary)
        .toList();
  }

  @Override
  public List<ExpenseSummary> findByTenantId(UUID tenantId) {
    return expenseRepository.findByTenantId(tenantId).stream()
        .map(ExpenseContextFacadeImpl::toSummary)
        .toList();
  }

  private static ExpenseSummary toSummary(Expense expense) {
    return new ExpenseSummary(
        expense.getId(),
        expense.getTripId(),
        expense.getCategory().name(),
        expense.getAmount().getAmount(),
        expense.getAmount().getCurrency(),
        expense.getExpenseDate(),
        expense.getStatus().name());
  }
}
