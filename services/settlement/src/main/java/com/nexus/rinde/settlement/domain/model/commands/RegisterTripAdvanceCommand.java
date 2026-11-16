package com.nexus.rinde.settlement.domain.model.commands;

import com.nexus.rinde.settlement.domain.model.valueobjects.AdvanceDeliveryMethod;
import java.math.BigDecimal;
import java.util.UUID;

/** Registra un adelanto de viáticos para un viaje programado o asignado. */
public record RegisterTripAdvanceCommand(
    UUID tenantId,
    UUID tripId,
    UUID registeredBy,
    BigDecimal amount,
    String currency,
    AdvanceDeliveryMethod deliveryMethod) {}
