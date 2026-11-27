package com.nexus.rinde.settlement.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Representación de una liquidación devuelta al cliente. */
public record SettlementResource(
    UUID id,
    UUID tenantId,
    UUID tripId,
    UUID driverId,
    String status,
    BigDecimal advanceAmount,
    String advanceCurrency,
    BigDecimal expenseTotalAmount,
    String expenseTotalCurrency,
    BigDecimal advanceBalance,
    Instant createdAt,
    Instant updatedAt,
    @Schema(
            description =
                "Gastos del viaje que aún no están aprobados ni observados. Solo viene en la respuesta del recálculo.")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Integer pendingExpenseCount) {}
