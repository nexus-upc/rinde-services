package com.nexus.rinde.dashboard.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/** Series consolidadas para los gráficos del panel operativo. */
public record DashboardChartsResource(
    LocalDate from,
    LocalDate to,
    @Schema(example = "PEN") String currency,
    List<String> excludedCurrencies,
    List<CategoryExpenseItem> expensesByCategory,
    List<TripStatusCountItem> tripsByStatus,
    List<VehicleExpenseItem> expensesByVehicle,
    boolean partial,
    List<String> unavailableSources) {}
