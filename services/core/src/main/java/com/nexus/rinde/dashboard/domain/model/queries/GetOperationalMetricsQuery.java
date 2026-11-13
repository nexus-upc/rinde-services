package com.nexus.rinde.dashboard.domain.model.queries;

import java.time.LocalDate;
import java.util.UUID;

/** Consulta las métricas operativas consolidadas de la empresa (US37), opcionalmente filtradas por periodo. */
public record GetOperationalMetricsQuery(UUID tenantId, LocalDate from, LocalDate to) {}
