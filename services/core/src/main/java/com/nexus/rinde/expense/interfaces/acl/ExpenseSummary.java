package com.nexus.rinde.expense.interfaces.acl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Vista de un gasto para otros contextos. Categoría y estado viajan como nombre del enum. */
public record ExpenseSummary(
    UUID id,
    UUID tripId,
    String category,
    BigDecimal amount,
    String currency,
    LocalDate expenseDate,
    String status) {}
