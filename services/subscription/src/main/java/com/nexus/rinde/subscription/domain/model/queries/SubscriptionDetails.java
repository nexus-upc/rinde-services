package com.nexus.rinde.subscription.domain.model.queries;

import com.nexus.rinde.subscription.domain.model.aggregates.Plan;
import com.nexus.rinde.subscription.domain.model.aggregates.Subscription;
import com.nexus.rinde.subscription.domain.model.valueobjects.PaymentStatus;

/** Proyección de consulta que contiene el estado de la suscripción y los datos del plan asociado. */
public record SubscriptionDetails(
    Subscription subscription,
    Plan plan,
    boolean renewalNotice,
    PaymentStatus latestPaymentStatus,
    String lastPaymentFailureReason) {}
