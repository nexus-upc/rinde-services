package com.nexus.rinde.expense.interfaces.rest.resources;

import com.nexus.rinde.expense.domain.model.valueobjects.ExpenseCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Ítem individual para sincronización en lote de gastos capturados sin conexión (US22). El
 * comprobante se sube después, con el id del gasto que devuelve la sincronización. Un campo
 * evidence heredado se ignora.
 */
@Schema(description = "Datos de un gasto registrado en modo offline para sincronización")
public record SyncExpenseItemResource(
    @NotNull(message = "El identificador del viaje es obligatorio")
        @Schema(
            description = "Identificador único del viaje",
            example = "33c310fc-cb77-4624-a790-d3706291e28f")
        UUID tripId,
    @NotNull(message = "La categoría es obligatoria")
        @Schema(description = "Categoría del gasto", example = "FUEL")
        ExpenseCategory category,
    @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        @Schema(description = "Monto monetario", example = "120.50")
        BigDecimal amount,
    @NotBlank(message = "La moneda es obligatoria")
        @Size(min = 3, max = 3, message = "La moneda debe tener formato ISO de 3 caracteres")
        @Schema(description = "Código de moneda ISO", example = "PEN")
        String currency,
    @NotNull(message = "La fecha del gasto es obligatoria")
        @Schema(description = "Fecha de ocurrencia del gasto", example = "2026-10-25")
        LocalDate expenseDate,
    @NotBlank(message = "La clave de idempotencia es obligatoria")
        @Schema(
            description = "Identificador único de idempotencia generado en el móvil",
            example = "OFFLINE-EXP-001")
        String idempotencyKey) {}
