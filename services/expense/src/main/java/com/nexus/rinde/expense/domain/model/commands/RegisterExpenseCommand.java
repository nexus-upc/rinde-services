package com.nexus.rinde.expense.domain.model.commands;

import com.nexus.rinde.expense.domain.model.valueobjects.ExpenseCategory;
import com.nexus.rinde.expense.domain.model.valueobjects.Money;
import java.time.LocalDate;
import java.util.UUID;

/** Comando para registrar un gasto operativo durante un viaje. El comprobante se sube aparte. */
public record RegisterExpenseCommand(
    UUID tenantId,
    UUID tripId,
    UUID driverId,
    ExpenseCategory category,
    Money amount,
    LocalDate expenseDate,
    String idempotencyKey) {}
