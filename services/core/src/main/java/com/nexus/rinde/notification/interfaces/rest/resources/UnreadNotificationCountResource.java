package com.nexus.rinde.notification.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/** Conteo de notificaciones no leídas para el usuario actual. */
public record UnreadNotificationCountResource(@Schema(example = "3") long count) {}
