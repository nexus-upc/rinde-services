package com.nexus.rinde.notification.domain.model.valueobjects;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Página de notificaciones con estado de lectura resuelto para el usuario actual. */
public record NotificationPage(
    List<NotificationItem> items, int page, int size, long totalItems, int totalPages) {

  /** Elemento individual de notificación con marca de lectura. */
  public record NotificationItem(
      UUID id,
      String type,
      String title,
      String message,
      String link,
      Instant createdAt,
      Instant readAt) {}
}
