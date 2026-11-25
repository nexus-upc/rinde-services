package com.nexus.rinde.notification.domain.model.queries;

import java.util.UUID;

/** Lista las notificaciones de la empresa con estado de lectura por usuario. */
public record ListCompanyNotificationsQuery(
    UUID tenantId, UUID userId, boolean unreadOnly, int page, int size) {}
