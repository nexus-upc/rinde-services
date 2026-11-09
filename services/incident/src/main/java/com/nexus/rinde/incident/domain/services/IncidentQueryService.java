package com.nexus.rinde.incident.domain.services;

import com.nexus.rinde.incident.domain.model.aggregates.Incident;
import com.nexus.rinde.incident.domain.model.valueobjects.IncidentStatus;
import java.util.List;
import java.util.UUID;

/** Consultas de incidencias, siempre filtradas por empresa. */
public interface IncidentQueryService {

  /** Lista las incidencias de la empresa, de la más reciente a la más antigua. */
  List<Incident> list(UUID tenantId, IncidentStatus status, UUID tripId, UUID vehicleId);

  /** Devuelve una incidencia que el rol puede ver; un conductor solo ve las que reportó. */
  Incident findVisible(UUID tenantId, UUID incidentId, UUID userId, String role);

  /** Incidencias que aún no están cerradas: abiertas o en atención. */
  List<Incident> findOpen(UUID tenantId);
}
