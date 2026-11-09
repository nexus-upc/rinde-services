package com.nexus.rinde.incident.domain.model.valueobjects;

import com.nexus.rinde.incident.domain.model.aggregates.Incident;

/**
 * Resultado de un reporte: la incidencia y si se creó en esta petición o ya existía con la misma
 * clave de idempotencia (reenvío de un reporte hecho sin conexión).
 */
public record ReportOutcome(Incident incident, boolean created) {}
