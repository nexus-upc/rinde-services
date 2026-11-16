package com.nexus.rinde.expense.interfaces.rest.resources;

import com.nexus.rinde.expense.domain.model.valueobjects.ExpenseCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos para registrar un gasto operativo durante un viaje. El comprobante no va en este cuerpo:
 * se sube después con POST /api/v1/expenses/{expenseId}/evidences.
 */
public record RegisterExpenseResource(
    @Schema(example = "FUEL") @NotNull ExpenseCategory category,
    @Schema(example = "180.50")
        @NotNull
        @DecimalMin(value = "0", inclusive = false, message = "El monto debe ser mayor a cero.")
        @Digits(integer = 8, fraction = 2)
        BigDecimal amount,
    @Schema(example = "PEN", defaultValue = "PEN") @Size(min = 3, max = 3) String currency,
    @Schema(example = "2026-10-20") @NotNull LocalDate expenseDate,
    @Schema(example = "EXP-9831-UUID-01") @NotBlank @Size(max = 64) String idempotencyKey) {}
