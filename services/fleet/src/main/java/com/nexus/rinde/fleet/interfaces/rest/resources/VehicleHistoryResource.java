package com.nexus.rinde.fleet.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Historial consolidado de mantenimientos, viajes e incidencias de un vehículo. */
public record VehicleHistoryResource(
    @Schema(example = "deac3d8e-12d4-4a72-98f2-1e2fc4d0d230") UUID vehicleId,
    @Schema(nullable = true, example = "2027-02-09") LocalDate nextMaintenanceDate,
    List<VehicleHistoryItemResource> items,
    boolean partial,
    List<String> unavailableSources) {}
