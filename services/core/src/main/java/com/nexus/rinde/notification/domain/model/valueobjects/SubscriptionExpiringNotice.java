package com.nexus.rinde.notification.domain.model.valueobjects;

import java.time.Instant;
import java.util.UUID;

/** Aviso de vencimiento de suscripción que Notifications convierte en un correo simulado. */
public record SubscriptionExpiringNotice(
    UUID eventId, Instant occurredAt, UUID tenantId, UUID subscriptionId, Instant expiresAt) {}
