package com.nexus.rinde.subscription.domain.model.valueobjects;

import java.math.BigDecimal;
import java.util.UUID;

/** Datos del intento de pago que puede completar la pasarela simulada. */
public record PaymentCheckout(
    UUID paymentId,
    String providerReference,
    BigDecimal amount,
    PaymentStatus status,
    boolean simulated,
    boolean created) {}
