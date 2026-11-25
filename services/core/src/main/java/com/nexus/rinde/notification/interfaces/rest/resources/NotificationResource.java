package com.nexus.rinde.notification.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** Notificación del centro de avisos de la empresa. */
public record NotificationResource(
    @Schema(example = "a1b2c3d4-1111-4222-8333-444455556666") UUID id,
    @Schema(example = "EXPENSE_PENDING_REVIEW") String type,
    @Schema(example = "Gasto pendiente de revisión") String title,
    @Schema(example = "Se registró un gasto por PEN 180.50 para revisar.") String message,
    @Schema(example = "/expenses?tripId=87460614-42ea-4cd5-9de1-8f38d9496434") String link,
    @Schema(example = "2026-11-25T14:00:00Z") Instant createdAt,
    @Schema(nullable = true, example = "2026-11-25T15:00:00Z") Instant readAt) {}
