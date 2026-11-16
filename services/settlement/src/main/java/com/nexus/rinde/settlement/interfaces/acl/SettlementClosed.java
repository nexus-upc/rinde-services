package com.nexus.rinde.settlement.interfaces.acl;

import com.nexus.rinde.shared.domain.model.events.IntegrationEvent;
import java.time.Instant;
import java.util.UUID;

/** Contrato de cierre que Settlement publicará para actualizar los contextos consumidores. */
// TODO: Settlement debe completar y confirmar este contrato al implementarse.
public record SettlementClosed(
    UUID eventId, Instant occurredAt, UUID tenantId, UUID tripId, UUID closedBy)
    implements IntegrationEvent {

  @Override
  public String type() {
    return "SettlementClosed";
  }
}
