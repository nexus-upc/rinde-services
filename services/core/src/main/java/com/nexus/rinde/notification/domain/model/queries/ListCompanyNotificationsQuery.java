package com.nexus.rinde.notification.domain.model.queries;

import com.nexus.rinde.notification.domain.model.valueobjects.NotificationType;
import java.util.Set;
import java.util.UUID;

/** Lista las notificaciones de la empresa con filtros opcionales y lectura por usuario. */
public record ListCompanyNotificationsQuery(
    UUID tenantId, UUID userId, boolean unreadOnly, Set<NotificationType> types, int page, int size) {}
