package com.nexus.rinde.incident.interfaces.acl;

import java.util.UUID;

/** Fachada pública del contexto Incident Management para el Dashboard operativo. */
public interface IncidentContextFacade {

  /** Cantidad de incidencias abiertas de la empresa; falla si el servicio de Incident no responde. */
  long countOpenByTenantId(UUID tenantId);
}
