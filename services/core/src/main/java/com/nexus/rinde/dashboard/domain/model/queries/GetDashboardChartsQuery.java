package com.nexus.rinde.dashboard.domain.model.queries;

import java.time.LocalDate;
import java.util.UUID;

/** Consulta las series agregadas para los gráficos del panel operativo. */
public record GetDashboardChartsQuery(UUID tenantId, LocalDate from, LocalDate to) {}
