package com.nexus.rinde.settlement.interfaces.rest.resources;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/** Cuerpo de la solicitud para registrar el anticipo de un viaje. */
public record RegisterAdvanceResource(
    @NotNull UUID tripId,
    @NotNull @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero.") BigDecimal amount,
    String currency) {}
