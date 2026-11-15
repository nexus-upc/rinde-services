package com.nexus.rinde.subscription.interfaces.acl;

import com.nexus.rinde.shared.domain.model.events.IntegrationEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Aviso de comprobante para que Notifications lo entregue por el canal de correo simulado. */
public record PaymentReceiptRequested(
    UUID eventId,
    Instant occurredAt,
    UUID tenantId,
    UUID subscriptionId,
    UUID paymentId,
    String providerReference,
    BigDecimal amount,
    Instant startsAt,
    Instant expiresAt)
    implements IntegrationEvent {

  @Override
  public String type() {
    return "PaymentReceiptRequested";
  }
}
