package com.nexus.rinde.settlement.interfaces.rest.resources;

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
    Instant updatedAt) {}
