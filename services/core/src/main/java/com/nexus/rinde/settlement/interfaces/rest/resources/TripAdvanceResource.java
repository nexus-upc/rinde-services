package com.nexus.rinde.settlement.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Adelanto de viáticos registrado para un viaje, visible para el responsable y el conductor. */
public record TripAdvanceResource(
    UUID id,
    UUID tripId,
    BigDecimal amount,
    String currency,
    String deliveryMethod,
    UUID registeredBy,
    Instant registeredAt) {}
