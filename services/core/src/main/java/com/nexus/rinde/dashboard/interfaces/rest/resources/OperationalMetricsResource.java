package com.nexus.rinde.dashboard.interfaces.rest.resources;

/** Métricas operativas consolidadas de la empresa (US37). */
public record OperationalMetricsResource(
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
    long pendingSettlements) {}
