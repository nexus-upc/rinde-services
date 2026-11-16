package com.nexus.rinde.dashboard.domain.model.queries;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Consulta las métricas operativas de la empresa (US37). Las fechas del periodo pueden ser nulas;
 * sin ellas, el periodo va desde el día 1 del mes actual hasta hoy.
 */
public record GetOperationalMetricsQuery(UUID tenantId, LocalDate from, LocalDate to) {}
