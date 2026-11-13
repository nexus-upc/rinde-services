package com.nexus.rinde.dashboard.interfaces.rest.resources;

import java.math.BigDecimal;
import java.util.Map;

/** Métricas operativas consolidadas de la empresa (US37), filtradas por periodo. */
public record OperationalMetricsResource(
    PeriodInfo period,
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
    Map<String, BigDecimal> approvedExpenseTotals,
    long pendingSettlements,
    IncidentsInfo incidents,
    boolean partial) {

  public record PeriodInfo(String from, String to) {}

  public record IncidentsInfo(Integer openCount, String source) {}
}
