package com.nexus.rinde.subscription.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexus.rinde.subscription.domain.model.aggregates.Plan;
import com.nexus.rinde.subscription.domain.model.aggregates.Subscription;
import com.nexus.rinde.subscription.domain.model.commands.ProcessPaymentWebhookCommand;
import com.nexus.rinde.subscription.domain.model.commands.StartSubscriptionCheckoutCommand;
import com.nexus.rinde.subscription.domain.model.valueobjects.PaymentStatus;
import com.nexus.rinde.subscription.domain.services.PaymentCommandService;
import com.nexus.rinde.subscription.infrastructure.persistence.jpa.repositories.PlanRepository;
import com.nexus.rinde.subscription.infrastructure.persistence.jpa.repositories.SubscriptionRepository;
import com.nexus.rinde.subscription.interfaces.acl.SubscriptionActivated;
import com.nexus.rinde.subscription.interfaces.acl.SubscriptionSuspended;
import com.nexus.rinde.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/** Verifica publicación posterior al commit y el contrato JSON que consume IAM. */
class SubscriptionPublisherIntegrationTest extends AbstractIntegrationTest {

  @Autowired private RabbitTemplate rabbit;
  @Autowired private AmqpAdmin admin;
  @Autowired private Environment environment;
  @Autowired private ApplicationEventPublisher publisher;
  @Autowired private PlatformTransactionManager transactions;
  @Autowired private PaymentCommandService payments;
  @Autowired private PlanRepository plans;
  @Autowired private SubscriptionRepository subscriptions;

  @BeforeEach
  void prepareQueues() {
    admin.initialize();
    admin.purgeQueue(queue(), false);
  }

  @Test
  void publishesTheActivationProducedByAConfirmedPayment() throws Exception {
    UUID tenant = UUID.fromString(api.registerAndVerifyTenant("ana@andes.pe"));
    Plan plan = plans.saveAndFlush(new Plan("Plan de prueba", new BigDecimal("99.00"), 10, true));
    Subscription subscription = subscriptions.saveAndFlush(Subscription.pendingPayment(tenant, plan.getPlanId()));
    var checkout = payments.handle(new StartSubscriptionCheckoutCommand(tenant, subscription.getSubscriptionId()));
    payments.handle(new ProcessPaymentWebhookCommand("payment-" + UUID.randomUUID(),
        checkout.providerReference(), PaymentStatus.CONFIRMED, "99.00", null));

    Message message = rabbit.receive(queue(), 5_000);
    assertThat(message).isNotNull();
    assertThat(message.getMessageProperties().getReceivedRoutingKey()).isEqualTo("subscription.activated");
    var json = objectMapper.readTree(message.getBody());
    assertThat(json.get("tenantId").asText()).isEqualTo(tenant.toString());
    assertThat(json.get("eventId").asText()).isEqualTo(message.getMessageProperties().getMessageId());
    assertThat(json.hasNonNull("occurredAt")).isTrue();
  }

  @Test
  void publishesSuspensionOnlyAfterTheTransactionCommits() throws Exception {
    UUID tenant = UUID.fromString(api.registerAndVerifyTenant("ana@andes.pe"));
    SubscriptionSuspended event = new SubscriptionSuspended(UUID.randomUUID(), Instant.now(), tenant);
    new TransactionTemplate(transactions).executeWithoutResult(status -> {
      publisher.publishEvent(event);
      assertThat(rabbit.receive(queue(), 100)).isNull();
    });

    Message message = rabbit.receive(queue(), 5_000);
    assertThat(message).isNotNull();
    assertThat(message.getMessageProperties().getReceivedRoutingKey()).isEqualTo("subscription.suspended");
    assertThat(objectMapper.readTree(message.getBody()).get("eventId").asText())
        .isEqualTo(event.eventId().toString());
  }

  @Test
  void doesNotPublishAnEventFromARolledBackTransaction() {
    new TransactionTemplate(transactions).executeWithoutResult(status -> {
      publisher.publishEvent(new SubscriptionActivated(UUID.randomUUID(), Instant.now(), UUID.randomUUID()));
      status.setRollbackOnly();
    });
    assertThat(rabbit.receive(queue(), 300)).isNull();
  }

  private String queue() {
    return environment.getRequiredProperty("rinde.messaging.subscription.queue");
  }
}
