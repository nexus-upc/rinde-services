package com.nexus.rinde.notification.domain.model.queries;

import java.util.UUID;

/** Consulta el número de notificaciones no leídas para el usuario autenticado. */
public record GetUnreadNotificationCountQuery(UUID tenantId, UUID userId) {}
