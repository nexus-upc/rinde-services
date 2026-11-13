package com.nexus.rinde.settlement.interfaces.acl;

import com.nexus.rinde.shared.domain.model.events.IntegrationEvent;
import java.time.Instant;
import java.util.UUID;

/** Contrato de integración publicado en RabbitMQ al cerrar una liquidación. */
public record SettlementClosed(
    UUID eventId, Instant occurredAt, UUID tenantId, UUID tripId, UUID closedBy)
    implements IntegrationEvent {

  @Override
  public String type() {
    return "SettlementClosed";
  }
}
