package com.nexus.rinde.notification.domain.model.valueobjects;

import java.time.Instant;
import java.util.UUID;

/** Aviso de empresa listo para guardarse, identificado por el evento que lo origina. */
public record CompanyNotice(
    UUID eventId,
    UUID tenantId,
    NotificationType type,
    String title,
    String message,
    String link,
    Instant createdAt) {}
