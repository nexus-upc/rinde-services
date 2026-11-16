package com.nexus.rinde.settlement.infrastructure.persistence.jpa.repositories;

import com.nexus.rinde.settlement.domain.model.aggregates.TripAdvance;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repositorio de adelantos de viáticos con aislamiento por empresa. */
@Repository
public interface TripAdvanceRepository extends JpaRepository<TripAdvance, UUID> {

  List<TripAdvance> findByTenantIdAndTripIdOrderByCreatedAtAsc(UUID tenantId, UUID tripId);
}
