package com.nexus.rinde.subscription.interfaces.rest.resources;

import com.nexus.rinde.subscription.domain.model.valueobjects.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

/** Respuesta de un checkout local; no expone ni solicita datos de tarjeta. */
public record PaymentCheckoutResource(
    UUID paymentId,
    String providerReference,
    BigDecimal amount,
    PaymentStatus status,
    @Schema(description = "Indica que el procesador se simula localmente.") boolean simulated,
    @Schema(description = "Habilita la confirmación desde la interfaz de pruebas.") boolean confirmationEnabled) {}
