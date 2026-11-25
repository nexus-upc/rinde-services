package com.nexus.rinde.notification.domain.model.commands;

import java.util.UUID;

/** Marca una notificación como leída por el usuario autenticado. */
public record MarkNotificationReadCommand(UUID tenantId, UUID userId, UUID notificationId) {}
