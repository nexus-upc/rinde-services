package com.nexus.rinde.expense.interfaces.rest.resources;

import java.util.List;

/** Página de gastos de la bandeja operativa. */
public record ExpensePageResource(
    List<ExpenseResource> items, int page, int size, long totalItems, int totalPages) {}
