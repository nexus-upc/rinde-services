package com.nexus.rinde.settlement.domain.model.valueobjects;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Gasto aprobado del viaje tal como lo necesita la liquidación. */
public record ApprovedExpense(
    UUID expenseId,
    String category,
    BigDecimal amount,
    String currency,
    LocalDate expenseDate,
    String status) {}
