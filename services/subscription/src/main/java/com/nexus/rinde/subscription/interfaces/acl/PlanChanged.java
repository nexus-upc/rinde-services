package com.nexus.rinde.subscription.interfaces.acl;

import com.nexus.rinde.shared.domain.model.events.IntegrationEvent;
import java.time.Instant;
import java.util.UUID;

/** Fleet mantiene este plan y su límite para validar nuevas unidades sin consultar Subscriptions. */
public record PlanChanged(
    UUID eventId, Instant occurredAt, UUID tenantId, UUID planId, int unitLimit)
    implements IntegrationEvent {

  @Override
  public String type() {
    return "PlanChanged";
  }
}
