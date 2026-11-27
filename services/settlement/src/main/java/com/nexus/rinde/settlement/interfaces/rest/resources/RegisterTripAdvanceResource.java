package com.nexus.rinde.settlement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/** Cuerpo de la solicitud para registrar un adelanto de viáticos. */
public record RegisterTripAdvanceResource(
    @Schema(description = "Monto entregado al conductor. Debe ser mayor a cero.", example = "500.00")
        @NotNull(message = "Indique el monto del adelanto.")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero.")
        BigDecimal amount,
    @Schema(
            description = "Código de moneda de tres letras. Si se omite, es PEN.",
            example = "PEN",
            nullable = true)
        @Pattern(
            regexp = "^[A-Za-z]{3}$",
            message = "La moneda debe tener tres letras, por ejemplo PEN.")
        String currency,
    @Schema(
            description = "Medio con el que se entrega el adelanto.",
            allowableValues = {"CASH", "TRANSFER"},
            example = "CASH")
        @NotBlank(message = "Indique el medio de entrega del adelanto.")
        @Pattern(
            regexp = "CASH|TRANSFER",
            message = "El medio de entrega debe ser CASH o TRANSFER.")
        String deliveryMethod) {}
