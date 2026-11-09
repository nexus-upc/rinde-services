package com.nexus.rinde.support;

import com.nexus.rinde.expense.interfaces.acl.ExpenseContextFacade;
import com.nexus.rinde.expense.interfaces.acl.ExpenseSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Doble en memoria de Expense: los gastos de cada viaje y su estado de aprobación. */
@Component
public class ExpenseDouble implements ExpenseContextFacade {

  private record Expense(UUID tripId, ExpenseSummary summary) {}

  private final Map<UUID, Map<UUID, Expense>> expenses = new ConcurrentHashMap<>();

  public void reset() {
    expenses.clear();
  }

  public void addExpense(
      UUID tenantId,
      UUID tripId,
      UUID expenseId,
      String category,
      BigDecimal amount,
      LocalDate date,
      String status) {
    ExpenseSummary summary = new ExpenseSummary(expenseId, category, amount, "PEN", date, status);
    expenses
        .computeIfAbsent(tenantId, id -> new ConcurrentHashMap<>())
        .put(expenseId, new Expense(tripId, summary));
  }

  public void approveExpense(UUID expenseId) {
    changeStatus(expenseId, "APPROVED");
  }

  /** Cambia el estado de auditoría del gasto, como lo haría Expense al revisarlo. */
  public void changeStatus(UUID expenseId, String status) {
    expenses
        .values()
        .forEach(
            tenantExpenses ->
                tenantExpenses.computeIfPresent(
                    expenseId,
                    (id, expense) ->
                        new Expense(
                            expense.tripId(),
                            new ExpenseSummary(
                                expense.summary().id(),
                                expense.summary().category(),
                                expense.summary().amount(),
                                expense.summary().currency(),
                                expense.summary().expenseDate(),
                                status))));
  }

  @Override
  public List<ExpenseSummary> findApprovedExpensesByTripId(UUID tenantId, UUID tripId) {
    return findByTenantIdAndTripId(tenantId, tripId).stream()
        .filter(expense -> "APPROVED".equals(expense.status()))
        .toList();
  }

  @Override
  public List<ExpenseSummary> findByTenantIdAndTripId(UUID tenantId, UUID tripId) {
    return expenses.getOrDefault(tenantId, Map.of()).values().stream()
        .filter(expense -> expense.tripId().equals(tripId))
        .map(Expense::summary)
        .toList();
  }

  @Override
  public List<ExpenseSummary> findByTenantId(UUID tenantId) {
    return expenses.getOrDefault(tenantId, Map.of()).values().stream()
        .map(Expense::summary)
        .toList();
  }
}
