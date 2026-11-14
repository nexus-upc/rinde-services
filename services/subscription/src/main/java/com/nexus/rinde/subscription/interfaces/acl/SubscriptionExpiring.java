package com.nexus.rinde.subscription.interfaces.acl;

import com.nexus.rinde.shared.domain.model.events.IntegrationEvent;
import java.time.Instant;
import java.util.UUID;

/** Aviso que Notifications convierte en un correo simulado cinco días antes del vencimiento. */
public record SubscriptionExpiring(
    UUID eventId, Instant occurredAt, UUID tenantId, UUID subscriptionId, Instant expiresAt)
    implements IntegrationEvent {

  @Override
  public String type() {
    return "SubscriptionExpiring";
  }
}
