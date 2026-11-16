package com.nexus.rinde.subscription.application.internal.commandservices;

import com.nexus.rinde.shared.domain.exceptions.ConflictException;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import com.nexus.rinde.subscription.domain.model.aggregates.Payment;
import com.nexus.rinde.subscription.domain.model.aggregates.Plan;
import com.nexus.rinde.subscription.domain.model.aggregates.Subscription;
import com.nexus.rinde.subscription.domain.model.commands.ProcessPaymentWebhookCommand;
import com.nexus.rinde.subscription.domain.model.commands.StartSubscriptionCheckoutCommand;
import com.nexus.rinde.subscription.domain.model.commands.SimulatePaymentCommand;
import com.nexus.rinde.subscription.domain.model.valueobjects.PaymentCheckout;
import com.nexus.rinde.subscription.domain.model.valueobjects.PaymentGatewaySession;
import com.nexus.rinde.subscription.domain.model.valueobjects.PaymentStatus;
import com.nexus.rinde.subscription.domain.model.valueobjects.PaymentWebhookResult;
import com.nexus.rinde.subscription.domain.model.valueobjects.SubscriptionStatus;
import com.nexus.rinde.subscription.domain.services.PaymentCommandService;
import com.nexus.rinde.subscription.domain.services.PaymentGatewayAdapter;
import com.nexus.rinde.subscription.infrastructure.persistence.jpa.repositories.PaymentRepository;
import com.nexus.rinde.subscription.infrastructure.persistence.jpa.repositories.PlanRepository;
import com.nexus.rinde.subscription.infrastructure.persistence.jpa.repositories.SubscriptionRepository;
import com.nexus.rinde.subscription.interfaces.acl.PaymentReceiptRequested;
import com.nexus.rinde.subscription.interfaces.acl.PlanChanged;
import com.nexus.rinde.subscription.interfaces.acl.SubscriptionActivated;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordina intentos simulados y el resultado firmado que devuelve la pasarela. */
@Service
public class SubscriptionPaymentCommandServiceImpl implements PaymentCommandService {

  private static final List<SubscriptionStatus> PAYABLE_STATUSES =
      List.of(
          SubscriptionStatus.PENDING_PAYMENT,
          SubscriptionStatus.ACTIVE,
          SubscriptionStatus.EXPIRING,
          SubscriptionStatus.EXPIRED,
          SubscriptionStatus.SUSPENDED);

  private final SubscriptionRepository subscriptionRepository;
  private final PlanRepository planRepository;
  private final PaymentRepository paymentRepository;
  private final PaymentGatewayAdapter paymentGatewayAdapter;
  private final ApplicationEventPublisher eventPublisher;
  private final Clock clock;

  public SubscriptionPaymentCommandServiceImpl(
      SubscriptionRepository subscriptionRepository,
      PlanRepository planRepository,
      PaymentRepository paymentRepository,
      PaymentGatewayAdapter paymentGatewayAdapter,
      ApplicationEventPublisher eventPublisher,
      Clock clock) {
    this.subscriptionRepository = subscriptionRepository;
    this.planRepository = planRepository;
    this.paymentRepository = paymentRepository;
    this.paymentGatewayAdapter = paymentGatewayAdapter;
    this.eventPublisher = eventPublisher;
    this.clock = clock;
  }

  @Override
  @Transactional
  public PaymentCheckout handle(StartSubscriptionCheckoutCommand command) {
    Subscription subscription = findSubscription(command.tenantId(), command.subscriptionId());
    if (!PAYABLE_STATUSES.contains(subscription.getStatus())) {
      throw new ConflictException("La suscripción no admite un pago en su estado actual.");
    }

    var existing =
        paymentRepository.findFirstByTenantIdAndSubscriptionIdAndStatusOrderByCreatedAtDesc(
            command.tenantId(), command.subscriptionId(), PaymentStatus.PENDING);
    if (existing.isPresent()) {
      return toCheckout(existing.get(), false);
    }

    Plan plan =
        planRepository
            .findById(subscription.getPlanId())
            .orElseThrow(() -> new ResourceNotFoundException("El plan de la suscripción no existe."));
    PaymentGatewaySession session = paymentGatewayAdapter.createCheckout(plan.getMonthlyPrice());
    Payment payment =
        paymentRepository.saveAndFlush(
            Payment.pending(
                command.tenantId(),
                command.subscriptionId(),
                session.providerReference(),
                plan.getMonthlyPrice(),
                clock.instant()));
    return toCheckout(payment, true);
  }

  @Override
  @Transactional
  public PaymentWebhookResult handle(SimulatePaymentCommand command) {
    var payment = paymentRepository.findById(command.paymentId())
        .filter(value -> command.tenantId().equals(value.getTenantId())
            && command.subscriptionId().equals(value.getSubscriptionId()))
        .orElseThrow(() -> new ResourceNotFoundException("El intento de pago no existe en su empresa."));
    return handle(new ProcessPaymentWebhookCommand("simulation:" + command.paymentId(),
        payment.getProviderReference(), command.status(), payment.getAmount().toPlainString(),
        command.status() == PaymentStatus.REJECTED ? "El pago de prueba fue rechazado." : null));
  }

  @Override
  @Transactional
  public PaymentWebhookResult handle(ProcessPaymentWebhookCommand command) {
    if (command.status() == null || command.status() == PaymentStatus.PENDING) {
      throw new ConflictException("El webhook debe informar CONFIRMED o REJECTED.");
    }
    BigDecimal amount = parseAmount(command.amount());

    var duplicate = paymentRepository.findByProviderEventId(command.providerEventId());
    if (duplicate.isPresent()) {
      Payment payment = duplicate.get();
      if (!payment.getProviderReference().equals(command.providerReference())
          || payment.getStatus() != command.status()
          || payment.getAmount().compareTo(amount) != 0) {
        throw new ConflictException("El identificador de evento ya se usó con otro resultado.");
      }
      Subscription subscription =
          findSubscription(payment.getTenantId(), payment.getSubscriptionId());
      return toWebhookResult(payment, subscription, true);
    }

    Payment payment =
        paymentRepository
            .findByProviderReference(command.providerReference())
            .orElseThrow(() -> new ResourceNotFoundException("El intento de pago no existe."));
    if (payment.getAmount().compareTo(amount) != 0) {
      throw new ConflictException("El importe del pago no coincide con el plan seleccionado.");
    }
    Subscription subscription =
        findSubscription(payment.getTenantId(), payment.getSubscriptionId());
    if (command.status() == PaymentStatus.CONFIRMED) {
      payment.confirm(command.providerEventId());
      Instant paidAt = clock.instant();
      subscription.applyConfirmedPayment(paidAt);
      paymentRepository.saveAndFlush(payment);
      subscriptionRepository.saveAndFlush(subscription);
      publishPaymentConfirmed(payment, subscription, paidAt);
    } else {
      payment.reject(command.providerEventId(), command.failureReason());
      paymentRepository.saveAndFlush(payment);
    }
    return toWebhookResult(payment, subscription, false);
  }

  private void publishPaymentConfirmed(Payment payment, Subscription subscription, Instant now) {
    Plan plan =
        planRepository
            .findById(subscription.getPlanId())
            .orElseThrow(() -> new ResourceNotFoundException("El plan de la suscripción no existe."));
    eventPublisher.publishEvent(new SubscriptionActivated(UUID.randomUUID(), now, subscription.getTenantId()));
    eventPublisher.publishEvent(
        new PlanChanged(
            UUID.randomUUID(), now, subscription.getTenantId(), plan.getPlanId(), plan.getUnitLimit()));
    UUID receiptEventId =
        UUID.nameUUIDFromBytes(
            ("payment-receipt:" + payment.getPaymentId()).getBytes(StandardCharsets.UTF_8));
    eventPublisher.publishEvent(
        new PaymentReceiptRequested(
            receiptEventId,
            now,
            subscription.getTenantId(),
            subscription.getSubscriptionId(),
            payment.getPaymentId(),
            payment.getProviderReference(),
            payment.getAmount(),
            subscription.getStartsAt(),
            subscription.getExpiresAt()));
  }

  private BigDecimal parseAmount(String rawAmount) {
    try {
      BigDecimal amount = new BigDecimal(rawAmount);
      if (amount.signum() <= 0) {
        throw new NumberFormatException("non-positive amount");
      }
      return amount;
    } catch (NumberFormatException ex) {
      throw new ConflictException("El importe reportado por la pasarela no es válido.");
    }
  }

  private Subscription findSubscription(UUID tenantId, UUID subscriptionId) {
    return subscriptionRepository
        .findBySubscriptionIdAndTenantId(subscriptionId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("La suscripción no existe."));
  }

  private PaymentCheckout toCheckout(Payment payment, boolean created) {
    return new PaymentCheckout(
        payment.getPaymentId(),
        payment.getProviderReference(),
        payment.getAmount(),
        payment.getStatus(),
        true,
        created);
  }

  private PaymentWebhookResult toWebhookResult(
      Payment payment, Subscription subscription, boolean duplicate) {
    return new PaymentWebhookResult(
        payment.getProviderEventId(),
        duplicate,
        payment.getStatus(),
        subscription.getStatus(),
        subscription.getStartsAt(),
        subscription.getExpiresAt(),
        payment.getFailureReason());
  }
}
