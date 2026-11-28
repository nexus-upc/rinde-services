package com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories;

import java.time.Instant;
import java.util.UUID;

/** Fila de la lista del centro de notificaciones con la lectura del usuario consultado. */
public record CompanyNotificationRow(
    UUID id,
    String type,
    String title,
    String message,
    String link,
    Instant createdAt,
    Instant readAt) {}
