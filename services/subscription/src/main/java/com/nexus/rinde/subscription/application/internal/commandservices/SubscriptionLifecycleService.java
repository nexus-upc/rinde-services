package com.nexus.rinde.subscription.application.internal.commandservices;

import com.nexus.rinde.subscription.domain.model.aggregates.Subscription;
import com.nexus.rinde.subscription.domain.model.valueobjects.SubscriptionStatus;
import com.nexus.rinde.subscription.infrastructure.persistence.jpa.repositories.SubscriptionRepository;
import com.nexus.rinde.subscription.interfaces.acl.SubscriptionExpiring;
import com.nexus.rinde.subscription.interfaces.acl.SubscriptionSuspended;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Avanza estados de suscripción y publica avisos una sola vez por transición. */
@Service
public class SubscriptionLifecycleService {

  public static final Duration REMINDER_WINDOW = Duration.ofDays(5);
  public static final Duration GRACE_PERIOD = Duration.ofDays(3);

  private final SubscriptionRepository subscriptionRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final Clock clock;

  public SubscriptionLifecycleService(
      SubscriptionRepository subscriptionRepository,
      ApplicationEventPublisher eventPublisher,
      Clock clock) {
    this.subscriptionRepository = subscriptionRepository;
    this.eventPublisher = eventPublisher;
    this.clock = clock;
  }

  @Transactional
  public void advanceSubscriptions() {
    Instant now = clock.instant();
    Instant reminderCutoff = now.plus(REMINDER_WINDOW);
    for (Subscription subscription :
        subscriptionRepository.findByStatusAndExpiresAtBetween(
            SubscriptionStatus.ACTIVE, now.plusNanos(1), reminderCutoff)) {
      subscription.markExpiring();
      subscriptionRepository.save(subscription);
      eventPublisher.publishEvent(
          new SubscriptionExpiring(
              stableEventId("expiring", subscription),
              now,
              subscription.getTenantId(),
              subscription.getSubscriptionId(),
              subscription.getExpiresAt()));
    }

    for (Subscription subscription :
        subscriptionRepository.findByStatusAndExpiresAtLessThanEqual(
            SubscriptionStatus.ACTIVE, now)) {
      advanceExpiredSubscription(subscription, now);
    }
    for (Subscription subscription :
        subscriptionRepository.findByStatusAndExpiresAtLessThanEqual(
            SubscriptionStatus.EXPIRING, now)) {
      advanceExpiredSubscription(subscription, now);
    }
    for (Subscription subscription :
        subscriptionRepository.findByStatusAndExpiresAtLessThanEqual(
            SubscriptionStatus.EXPIRED, now.minus(GRACE_PERIOD))) {
      suspend(subscription, now);
    }
  }

  private void advanceExpiredSubscription(Subscription subscription, Instant now) {
    Instant suspensionAt = subscription.getExpiresAt().plus(GRACE_PERIOD);
    subscription.markExpired();
    if (!suspensionAt.isAfter(now)) {
      subscription.suspend();
      subscriptionRepository.save(subscription);
      publishSuspended(subscription, now);
      return;
    }
    subscriptionRepository.save(subscription);
  }

  private void suspend(Subscription subscription, Instant now) {
    subscription.suspend();
    subscriptionRepository.save(subscription);
    publishSuspended(subscription, now);
  }

  private void publishSuspended(Subscription subscription, Instant now) {
    eventPublisher.publishEvent(
        new SubscriptionSuspended(
            stableEventId("suspended", subscription), now, subscription.getTenantId()));
  }

  private UUID stableEventId(String transition, Subscription subscription) {
    String key =
        "subscription:"
            + transition
            + ":"
            + subscription.getSubscriptionId()
            + ":"
            + subscription.getExpiresAt();
    return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
  }
}
