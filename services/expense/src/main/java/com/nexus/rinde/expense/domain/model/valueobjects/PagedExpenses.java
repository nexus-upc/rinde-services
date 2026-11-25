package com.nexus.rinde.expense.domain.model.valueobjects;

import com.nexus.rinde.expense.domain.model.aggregates.Expense;
import java.util.List;

/** Resultado paginado de gastos filtrados para la bandeja. */
public record PagedExpenses(
    List<Expense> items, int page, int size, long totalItems, int totalPages) {}
