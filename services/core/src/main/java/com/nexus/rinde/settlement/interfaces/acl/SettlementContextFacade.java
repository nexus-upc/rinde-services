package com.nexus.rinde.settlement.interfaces.acl;

import java.util.Optional;
import java.util.UUID;

/** Fachada pública del Bounded Context Settlement para otros contextos. */
public interface SettlementContextFacade {

  Optional<SettlementSummary> findByTripIdAndTenantId(UUID tripId, UUID tenantId);

  /** Cantidad de liquidaciones de la empresa que todavía no se cierran. */
  long countOpenByTenantId(UUID tenantId);
}
