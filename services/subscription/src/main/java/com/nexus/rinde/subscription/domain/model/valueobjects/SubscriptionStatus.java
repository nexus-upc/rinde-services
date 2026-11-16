package com.nexus.rinde.subscription.domain.model.valueobjects;

/** Estados del ciclo de vida de una suscripción. */
public enum SubscriptionStatus {
  PENDING_PAYMENT,
  ACTIVE,
  EXPIRING,
  EXPIRED,
  SUSPENDED
}
