package com.nexus.rinde.subscription.interfaces.acl;

import com.nexus.rinde.shared.domain.model.events.IntegrationEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Lo publica Subscriptions & Billing al activar una suscripción; IAM reactiva la empresa. TODO:
 * completar el contenido (plan, vigencia) cuando se implemente Subscriptions.
 */
public record SubscriptionActivated(UUID eventId, Instant occurredAt, UUID tenantId)
    implements IntegrationEvent {

  @Override
  public String type() {
    return "SubscriptionActivated";
  }
}
