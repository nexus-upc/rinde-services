package com.nexus.rinde.dashboard.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

/** Gasto acumulado en moneda base asociado a un vehículo. */
public record VehicleExpenseItem(
    @Schema(example = "deac3d8e-12d4-4a72-98f2-1e2fc4d0d230") UUID vehicleId,
    @Schema(nullable = true, example = "ABC-123") String plateNumber,
    @Schema(example = "1890.50") BigDecimal total) {}
