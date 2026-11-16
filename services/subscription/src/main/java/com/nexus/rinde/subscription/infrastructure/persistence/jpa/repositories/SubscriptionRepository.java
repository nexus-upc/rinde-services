package com.nexus.rinde.subscription.infrastructure.persistence.jpa.repositories;

import com.nexus.rinde.subscription.domain.model.aggregates.Subscription;
import com.nexus.rinde.subscription.domain.model.valueobjects.SubscriptionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositorio de suscripciones. Las consultas que reciben empresa filtran por tenantId. */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

  Optional<Subscription> findBySubscriptionIdAndTenantId(UUID subscriptionId, UUID tenantId);

  boolean existsByTenantIdAndStatus(UUID tenantId, SubscriptionStatus status);

  boolean existsByTenantIdAndStatusIn(UUID tenantId, List<SubscriptionStatus> statuses);

  List<Subscription> findByStatusAndExpiresAtBetween(
      SubscriptionStatus status, Instant from, Instant to);

  List<Subscription> findByStatusAndExpiresAtLessThanEqual(SubscriptionStatus status, Instant at);

  long countByTenantId(UUID tenantId);

  @Query("select s from Subscription s where s.tenantId = :tenantId "
      + "order by case when s.status = :pending then 0 else 1 end, "
      + "s.expiresAt desc, s.subscriptionId desc")
  List<Subscription> findCurrentCandidates(@Param("tenantId") UUID tenantId,
      @Param("pending") SubscriptionStatus pending, Pageable page);
}
