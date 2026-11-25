package com.nexus.rinde.expense.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/** Gasto de un lote que no se guardó, con el motivo para mostrarlo al conductor (US22). */
@Schema(description = "Gasto del lote que no se pudo sincronizar")
public record SyncRejectedItemResource(
    @Schema(
            description = "Clave de idempotencia del gasto rechazado",
            example = "OFFLINE-EXP-002")
        String idempotencyKey,
    @Schema(description = "Mensaje para el usuario que explica el rechazo", example = "El viaje no existe.")
        String reason,
    @Schema(
            description = "Código del rechazo",
            example = "TRIP_NOT_FOUND",
            allowableValues = {
              "TRIP_NOT_FOUND",
              "TRIP_NOT_ASSIGNED",
              "TRIP_NOT_STARTED",
              "TRIP_SETTLED",
              "INVALID_EXPENSE",
              "DUPLICATE_KEY"
            })
        String code) {}
