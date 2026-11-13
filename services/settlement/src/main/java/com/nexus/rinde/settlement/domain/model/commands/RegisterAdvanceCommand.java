package com.nexus.rinde.settlement.domain.model.commands;

import java.math.BigDecimal;
import java.util.UUID;

/** Registra el anticipo entregado al conductor para un viaje. */
public record RegisterAdvanceCommand(
    UUID tenantId, UUID tripId, UUID registeredBy, BigDecimal amount, String currency) {}
