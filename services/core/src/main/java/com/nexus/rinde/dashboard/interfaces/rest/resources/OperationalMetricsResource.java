package com.nexus.rinde.dashboard.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Métricas operativas consolidadas de la empresa (US37). Los conteos de viajes, gastos y
 * liquidaciones no dependen del periodo; el gasto aprobado sí.
 */
public record OperationalMetricsResource(
    Period period,
    int totalTrips,
    int scheduledTrips,
    int assignedTrips,
    int inRouteTrips,
    int finishedTrips,
    int settledTrips,
    int totalExpenses,
    int approvedExpenses,
    int pendingExpenses,
    int observedExpenses,
    @Schema(
            description =
                "Suma de los gastos aprobados cuya fecha está en el periodo, por código de moneda.",
            example = "{\"PEN\": 1250.5}")
        Map<String, BigDecimal> approvedExpenseTotals,
    @Schema(description = "Liquidaciones de la empresa que todavía no se cierran.")
        int pendingSettlements,
    Incidents incidents,
    @Schema(
            description =
                "Verdadero cuando alguna fuente no respondió y la consulta quedó incompleta.")
        boolean partial) {

  /** Periodo con el que se sumó el gasto aprobado. */
  public record Period(LocalDate from, LocalDate to) {}

  /**
   * Incidencias abiertas y el estado de su fuente. Source vale AVAILABLE o UNAVAILABLE; con
   * UNAVAILABLE, openCount viene vacío.
   */
  public record Incidents(Long openCount, String source) {}
}
