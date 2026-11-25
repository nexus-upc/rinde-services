package com.nexus.rinde.notification.interfaces.rest.resources;

import java.util.List;

/** Página de notificaciones del centro de avisos. */
public record NotificationPageResource(
    List<NotificationResource> items, int page, int size, long totalItems, int totalPages) {}
