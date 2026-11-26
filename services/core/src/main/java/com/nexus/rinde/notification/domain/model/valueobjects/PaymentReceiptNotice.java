package com.nexus.rinde.notification.domain.model.valueobjects;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Aviso de comprobante de pago de suscripción que Notifications entrega por correo simulado. */
public record PaymentReceiptNotice(
    UUID eventId,
    Instant occurredAt,
    UUID tenantId,
    UUID subscriptionId,
    UUID paymentId,
    String providerReference,
    BigDecimal amount,
    Instant startsAt,
    Instant expiresAt) {}
