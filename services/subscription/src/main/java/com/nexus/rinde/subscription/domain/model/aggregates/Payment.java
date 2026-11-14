package com.nexus.rinde.subscription.domain.model.aggregates;

import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import com.nexus.rinde.shared.domain.exceptions.ConflictException;
import com.nexus.rinde.subscription.domain.model.valueobjects.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Resultado de un intento de pago; nunca contiene datos de tarjeta. */
@Entity
@Table(schema = "subscription", name = "payments")
public class Payment {

  @Id
  @Column(name = "payment_id", nullable = false, updatable = false)
  private UUID paymentId;

  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Column(name = "subscription_id", nullable = false, updatable = false)
  private UUID subscriptionId;

  @Column(name = "provider_event_id", unique = true, length = 160)
  private String providerEventId;

  @Column(name = "provider_reference", nullable = false, unique = true, length = 160)
  private String providerReference;

  @Column(name = "amount", nullable = false, precision = 10, scale = 2)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private PaymentStatus status;

  @Column(name = "failure_reason", length = 500)
  private String failureReason;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected Payment() {}

  private Payment(
      UUID tenantId,
      UUID subscriptionId,
      String providerEventId,
      String providerReference,
      BigDecimal amount,
      PaymentStatus status,
      String failureReason,
      Instant createdAt) {
    if (tenantId == null || subscriptionId == null) {
      throw new BusinessRuleException("La empresa y la suscripción del pago son obligatorias.");
    }
    if (providerReference == null || providerReference.isBlank()) {
      throw new BusinessRuleException("La referencia del pago es obligatoria.");
    }
    if (amount == null || amount.signum() <= 0) {
      throw new BusinessRuleException("El importe del pago debe ser positivo.");
    }
    if (status == null || createdAt == null) {
      throw new BusinessRuleException("El estado y la fecha de creación del pago son obligatorios.");
    }
    if (providerEventId != null && providerEventId.isBlank()) {
      throw new BusinessRuleException("El identificador del evento de pago no puede estar vacío.");
    }
    this.paymentId = UUID.randomUUID();
    this.tenantId = tenantId;
    this.subscriptionId = subscriptionId;
    this.providerEventId = providerEventId == null ? null : providerEventId.trim();
    this.providerReference = providerReference.trim();
    this.amount = amount;
    this.status = status;
    this.failureReason = normalizeFailureReason(failureReason);
    this.createdAt = createdAt;
  }

  public static Payment pending(
      UUID tenantId, UUID subscriptionId, String providerReference, BigDecimal amount, Instant now) {
    return new Payment(
        tenantId,
        subscriptionId,
        null,
        providerReference,
        amount,
        PaymentStatus.PENDING,
        null,
        now);
  }

  public void confirm(String eventId) {
    applyWebhook(eventId, PaymentStatus.CONFIRMED, null);
  }

  public void reject(String eventId, String reason) {
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException("La pasarela debe informar el motivo del rechazo.");
    }
    applyWebhook(eventId, PaymentStatus.REJECTED, reason);
  }

  private void applyWebhook(String eventId, PaymentStatus newStatus, String reason) {
    if (status != PaymentStatus.PENDING) {
      throw new ConflictException("El intento de pago ya tiene un resultado registrado.");
    }
    if (eventId == null || eventId.isBlank()) {
      throw new BusinessRuleException("El identificador del evento de pago es obligatorio.");
    }
    this.providerEventId = eventId.trim();
    this.status = newStatus;
    this.failureReason = normalizeFailureReason(reason);
  }

  private static String normalizeFailureReason(String reason) {
    if (reason == null || reason.isBlank()) {
      return null;
    }
    String normalized = reason.trim();
    if (normalized.length() > 500) {
      throw new BusinessRuleException("El motivo de rechazo admite hasta 500 caracteres.");
    }
    return normalized;
  }

  public UUID getPaymentId() {
    return paymentId;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public UUID getSubscriptionId() {
    return subscriptionId;
  }

  public String getProviderEventId() {
    return providerEventId;
  }

  public String getProviderReference() {
    return providerReference;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public PaymentStatus getStatus() {
    return status;
  }

  public String getFailureReason() {
    return failureReason;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
