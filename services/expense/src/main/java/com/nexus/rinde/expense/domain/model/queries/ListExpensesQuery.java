package com.nexus.rinde.expense.domain.model.queries;

import java.util.UUID;

/** Consulta paginada y filtrada de gastos de la empresa. */
public record ListExpensesQuery(
    UUID tenantId,
    String status,
    UUID tripId,
    UUID driverId,
    String from,
    String to,
    int page,
    int size) {}
