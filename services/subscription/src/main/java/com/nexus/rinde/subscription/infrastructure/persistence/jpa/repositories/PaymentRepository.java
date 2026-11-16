package com.nexus.rinde.subscription.infrastructure.persistence.jpa.repositories;

import com.nexus.rinde.subscription.domain.model.aggregates.Payment;
import com.nexus.rinde.subscription.domain.model.valueobjects.PaymentStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repositorio de intentos y resultados de pago, limitados a la empresa. */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

  Optional<Payment> findByProviderEventId(String providerEventId);

  Optional<Payment> findByProviderReference(String providerReference);

  Optional<Payment> findFirstByTenantIdAndSubscriptionIdAndStatusOrderByCreatedAtDesc(
      UUID tenantId, UUID subscriptionId, PaymentStatus status);

  Optional<Payment> findFirstByTenantIdAndSubscriptionIdOrderByCreatedAtDesc(
      UUID tenantId, UUID subscriptionId);

  long countByTenantIdAndSubscriptionId(UUID tenantId, UUID subscriptionId);
}
