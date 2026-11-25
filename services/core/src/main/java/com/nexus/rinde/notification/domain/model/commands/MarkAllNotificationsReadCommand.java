package com.nexus.rinde.notification.domain.model.commands;

import java.util.UUID;

/** Marca todas las notificaciones pendientes de la empresa como leídas para el usuario. */
public record MarkAllNotificationsReadCommand(UUID tenantId, UUID userId) {}
