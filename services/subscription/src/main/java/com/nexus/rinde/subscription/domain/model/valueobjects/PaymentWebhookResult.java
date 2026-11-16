package com.nexus.rinde.subscription.domain.model.valueobjects;

import java.time.Instant;

/** Resultado procesado de un webhook de pago, sin incluir datos de tarjeta. */
public record PaymentWebhookResult(
    String providerEventId,
    boolean duplicate,
    PaymentStatus paymentStatus,
    SubscriptionStatus subscriptionStatus,
    Instant startsAt,
    Instant expiresAt,
    String failureReason) {}
