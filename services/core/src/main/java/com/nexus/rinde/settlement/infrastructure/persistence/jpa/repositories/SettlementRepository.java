package com.nexus.rinde.settlement.infrastructure.persistence.jpa.repositories;

import com.nexus.rinde.settlement.domain.model.aggregates.Settlement;
import com.nexus.rinde.settlement.domain.model.valueobjects.SettlementStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repositorio de liquidaciones con aislamiento por empresa. */
@Repository
public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

  Optional<Settlement> findByIdAndTenantId(UUID id, UUID tenantId);

  Optional<Settlement> findByTripId(UUID tripId);

  Optional<Settlement> findByTripIdAndTenantId(UUID tripId, UUID tenantId);

  boolean existsByTripId(UUID tripId);

  boolean existsByTripIdAndTenantId(UUID tripId, UUID tenantId);

  long countByTenantIdAndStatus(UUID tenantId, SettlementStatus status);
}
