package com.nexus.rinde.subscription.infrastructure.persistence.jpa.repositories;

import com.nexus.rinde.subscription.domain.model.aggregates.Plan;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repositorio del catálogo compartido de planes. */
@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {

  List<Plan> findByActiveTrueOrderByNameAsc();

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select p from Plan p where p.planId=:id")
  Optional<Plan> findForPlatformUpdate(
      @org.springframework.data.repository.query.Param("id") UUID id);

  Optional<Plan> findByPlanIdAndActiveTrue(UUID planId);
}
