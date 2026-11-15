package com.nexus.rinde.subscription.application.internal.queryservices;

import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import com.nexus.rinde.subscription.domain.model.aggregates.Plan;
import com.nexus.rinde.subscription.domain.model.aggregates.Subscription;
import com.nexus.rinde.subscription.domain.model.queries.GetSubscriptionByTenantQuery;
import com.nexus.rinde.subscription.domain.model.queries.ListActivePlansQuery;
import com.nexus.rinde.subscription.domain.model.queries.SubscriptionDetails;
import com.nexus.rinde.subscription.domain.model.valueobjects.SubscriptionStatus;
import com.nexus.rinde.subscription.domain.services.SubscriptionQueryService;
import com.nexus.rinde.subscription.application.internal.commandservices.SubscriptionLifecycleService;
import com.nexus.rinde.subscription.infrastructure.persistence.jpa.repositories.PaymentRepository;
import com.nexus.rinde.subscription.infrastructure.persistence.jpa.repositories.PlanRepository;
import com.nexus.rinde.subscription.infrastructure.persistence.jpa.repositories.SubscriptionRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import com.nexus.rinde.subscription.domain.model.queries.GetCurrentSubscriptionQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Atiende el catálogo y las consultas de suscripción limitadas por empresa. */
@Service
public class SubscriptionQueryServiceImpl implements SubscriptionQueryService {

  private final PlanRepository planRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final PaymentRepository paymentRepository;
  private final Clock clock;

  public SubscriptionQueryServiceImpl(
      PlanRepository planRepository,
      SubscriptionRepository subscriptionRepository,
      PaymentRepository paymentRepository,
      Clock clock) {
    this.planRepository = planRepository;
    this.subscriptionRepository = subscriptionRepository;
    this.paymentRepository = paymentRepository;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Plan> handle(ListActivePlansQuery query) {
    return planRepository.findByActiveTrueOrderByNameAsc();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<SubscriptionDetails> handle(GetCurrentSubscriptionQuery query) {
    return subscriptionRepository.findCurrentCandidates(
        query.tenantId(), SubscriptionStatus.PENDING_PAYMENT, PageRequest.of(0, 1)).stream()
        .findFirst().map(subscription -> handle(new GetSubscriptionByTenantQuery(
            query.tenantId(), subscription.getSubscriptionId())));
  }

  @Override
  @Transactional(readOnly = true)
  public SubscriptionDetails handle(GetSubscriptionByTenantQuery query) {
    Subscription subscription =
        subscriptionRepository
            .findBySubscriptionIdAndTenantId(query.subscriptionId(), query.tenantId())
            .orElseThrow(() -> new ResourceNotFoundException("La suscripción no existe."));
    Plan plan =
        planRepository
            .findById(subscription.getPlanId())
            .orElseThrow(() -> new ResourceNotFoundException("El plan de la suscripción no existe."));
    var latestPayment =
        paymentRepository.findFirstByTenantIdAndSubscriptionIdOrderByCreatedAtDesc(
            query.tenantId(), query.subscriptionId());
    boolean renewalNotice =
        subscription.getExpiresAt() != null
            && (subscription.getStatus() == SubscriptionStatus.EXPIRING
                || (subscription.getStatus() == SubscriptionStatus.ACTIVE
                    && subscription.getExpiresAt().isAfter(clock.instant())
                    && Duration.between(clock.instant(), subscription.getExpiresAt())
                            .compareTo(SubscriptionLifecycleService.REMINDER_WINDOW)
                        <= 0));
    return new SubscriptionDetails(
        subscription,
        plan,
        renewalNotice,
        latestPayment.map(payment -> payment.getStatus()).orElse(null),
        latestPayment.map(payment -> payment.getFailureReason()).orElse(null));
  }
}
