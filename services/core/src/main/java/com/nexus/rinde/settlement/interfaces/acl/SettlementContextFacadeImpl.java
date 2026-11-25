package com.nexus.rinde.settlement.interfaces.acl;

import com.nexus.rinde.settlement.domain.model.aggregates.Settlement;
import com.nexus.rinde.settlement.domain.model.aggregates.TripAdvance;
import com.nexus.rinde.settlement.domain.model.valueobjects.SettlementStatus;
import com.nexus.rinde.settlement.infrastructure.persistence.jpa.repositories.SettlementRepository;
import com.nexus.rinde.settlement.infrastructure.persistence.jpa.repositories.TripAdvanceRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Implementación de la fachada de Settlement. */
@Component
public class SettlementContextFacadeImpl implements SettlementContextFacade {

  private final SettlementRepository settlementRepository;
  private final TripAdvanceRepository tripAdvanceRepository;

  public SettlementContextFacadeImpl(
      SettlementRepository settlementRepository, TripAdvanceRepository tripAdvanceRepository) {
    this.settlementRepository = settlementRepository;
    this.tripAdvanceRepository = tripAdvanceRepository;
  }

  @Override
  public Optional<SettlementSummary> findByTripIdAndTenantId(UUID tripId, UUID tenantId) {
    return settlementRepository
        .findByTripIdAndTenantId(tripId, tenantId)
        .map(SettlementContextFacadeImpl::toSummary);
  }

  @Override
  public Optional<String> findLatestAdvanceDeliveryMethod(UUID tripId, UUID tenantId) {
    List<TripAdvance> advances =
        tripAdvanceRepository.findByTenantIdAndTripIdOrderByCreatedAtAsc(tenantId, tripId);
    if (advances.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(advances.get(advances.size() - 1).getDeliveryMethod().name());
  }

  @Override
  public long countOpenByTenantId(UUID tenantId) {
    return settlementRepository.countByTenantIdAndStatus(tenantId, SettlementStatus.OPEN);
  }

  private static SettlementSummary toSummary(Settlement settlement) {
    return new SettlementSummary(
        settlement.getId(),
        settlement.getStatus().name(),
        settlement.getAdvanceAmount(),
        settlement.getAdvanceBalance(),
        settlement.getClosedAt());
  }
}
