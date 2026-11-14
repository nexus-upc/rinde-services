package com.nexus.rinde.subscription.domain.model.aggregates;

import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import com.nexus.rinde.subscription.domain.model.valueobjects.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

/** Suscripción de una empresa a un plan, identificando sus referencias sin importar otros modelos. */
@Entity
@Table(schema = "subscription", name = "subscriptions")
public class Subscription {

  @Id
  @Column(name = "subscription_id", nullable = false, updatable = false)
  private UUID subscriptionId;

  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Column(name = "plan_id", nullable = false, updatable = false)
  private UUID planId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private SubscriptionStatus status;

  @Column(name = "starts_at")
  private Instant startsAt;

  @Column(name = "expires_at")
  private Instant expiresAt;

  protected Subscription() {}

  private Subscription(UUID tenantId, UUID planId) {
    if (tenantId == null) {
      throw new BusinessRuleException("El identificador de empresa es obligatorio.");
    }
    if (planId == null) {
      throw new BusinessRuleException("El identificador del plan es obligatorio.");
    }
    this.subscriptionId = UUID.randomUUID();
    this.tenantId = tenantId;
    this.planId = planId;
    this.status = SubscriptionStatus.PENDING_PAYMENT;
    this.startsAt = null;
    this.expiresAt = null;
  }

  public static Subscription pendingPayment(UUID tenantId, UUID planId) {
    return new Subscription(tenantId, planId);
  }

  /** Activa o renueva un periodo mensual solo después de una confirmación de pago. */
  public void applyConfirmedPayment(Instant paidAt) {
    if (paidAt == null) {
      throw new BusinessRuleException("La fecha de confirmación del pago es obligatoria.");
    }
    Instant periodStart = expiresAt != null && expiresAt.isAfter(paidAt) ? expiresAt : paidAt;
    this.startsAt = periodStart;
    this.expiresAt = ZonedDateTime.ofInstant(periodStart, ZoneOffset.UTC).plusMonths(1).toInstant();
    this.status = SubscriptionStatus.ACTIVE;
  }

  public void markExpiring() {
    requireStatus(SubscriptionStatus.ACTIVE, "avisar el vencimiento de");
    this.status = SubscriptionStatus.EXPIRING;
  }

  public void markExpired() {
    if (status != SubscriptionStatus.ACTIVE && status != SubscriptionStatus.EXPIRING) {
      throw new BusinessRuleException("Solo una suscripción vigente puede marcarse vencida.");
    }
    this.status = SubscriptionStatus.EXPIRED;
  }

  public void suspend() {
    requireStatus(SubscriptionStatus.EXPIRED, "suspender");
    this.status = SubscriptionStatus.SUSPENDED;
  }

  private void requireStatus(SubscriptionStatus expected, String action) {
    if (status != expected) {
      throw new BusinessRuleException(
          "No se puede " + action + " la suscripción desde el estado " + status + ".");
    }
  }

  public UUID getSubscriptionId() {
    return subscriptionId;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public UUID getPlanId() {
    return planId;
  }

  public SubscriptionStatus getStatus() {
    return status;
  }

  public Instant getStartsAt() {
    return startsAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }
}
