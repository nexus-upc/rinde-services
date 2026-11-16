package com.nexus.rinde.settlement.interfaces.acl;

import com.nexus.rinde.settlement.domain.model.aggregates.Settlement;
import com.nexus.rinde.settlement.domain.model.valueobjects.SettlementStatus;
import com.nexus.rinde.settlement.infrastructure.persistence.jpa.repositories.SettlementRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Implementación de la fachada de Settlement. */
@Component
public class SettlementContextFacadeImpl implements SettlementContextFacade {

  private final SettlementRepository settlementRepository;

  public SettlementContextFacadeImpl(SettlementRepository settlementRepository) {
    this.settlementRepository = settlementRepository;
  }

  @Override
  public Optional<SettlementSummary> findByTripIdAndTenantId(UUID tripId, UUID tenantId) {
    return settlementRepository
        .findByTripIdAndTenantId(tripId, tenantId)
        .map(SettlementContextFacadeImpl::toSummary);
  }

  @Override
  public long countOpenByTenantId(UUID tenantId) {
    return settlementRepository.countByTenantIdAndStatus(tenantId, SettlementStatus.OPEN);
  }

  private static SettlementSummary toSummary(Settlement settlement) {
    return new SettlementSummary(
        settlement.getStatus().name(),
        settlement.getAdvanceAmount(),
        settlement.getAdvanceBalance());
  }
}
