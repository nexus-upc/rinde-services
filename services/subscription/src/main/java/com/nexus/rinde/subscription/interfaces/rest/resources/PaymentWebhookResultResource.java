package com.nexus.rinde.subscription.interfaces.rest.resources;

import com.nexus.rinde.subscription.domain.model.valueobjects.PaymentStatus;
import com.nexus.rinde.subscription.domain.model.valueobjects.SubscriptionStatus;
import java.time.Instant;

/** Confirmación idempotente del resultado de un pago. */
public record PaymentWebhookResultResource(
    String providerEventId,
    boolean duplicate,
    PaymentStatus paymentStatus,
    SubscriptionStatus subscriptionStatus,
    Instant startsAt,
    Instant expiresAt,
    String failureReason) {}
