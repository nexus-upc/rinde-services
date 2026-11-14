package com.nexus.rinde.subscription.interfaces.acl;

import com.nexus.rinde.shared.domain.model.events.IntegrationEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Lo publica Subscriptions & Billing al suspender una suscripción; IAM restringe la empresa. TODO:
 * completar el contenido (motivo) cuando se implemente Subscriptions.
 */
public record SubscriptionSuspended(UUID eventId, Instant occurredAt, UUID tenantId)
    implements IntegrationEvent {

  @Override
  public String type() {
    return "SubscriptionSuspended";
  }
}
