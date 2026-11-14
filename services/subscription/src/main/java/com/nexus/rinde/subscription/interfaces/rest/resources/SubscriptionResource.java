package com.nexus.rinde.subscription.interfaces.rest.resources;

import com.nexus.rinde.subscription.domain.model.valueobjects.SubscriptionStatus;
import com.nexus.rinde.subscription.domain.model.valueobjects.PaymentStatus;
import java.time.Instant;
import java.util.UUID;

/** Representación pública del estado de una suscripción y del plan asociado. */
public record SubscriptionResource(
    UUID subscriptionId,
    PlanResource plan,
    SubscriptionStatus status,
    Instant startsAt,
    Instant expiresAt,
    boolean renewalNotice,
    PaymentStatus latestPaymentStatus,
    String lastPaymentFailureReason) {}
