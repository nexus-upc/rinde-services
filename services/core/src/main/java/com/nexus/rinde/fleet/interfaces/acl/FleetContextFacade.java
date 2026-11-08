package com.nexus.rinde.fleet.interfaces.acl;

import java.util.List;
import java.util.UUID;

/**
 * Consultas que core necesita de Fleet & Maintenance: unidades y conductores de la empresa, y el
 * conteo de unidades para validar el plan.
 */
public interface FleetContextFacade {

  /** Número de unidades registradas en la empresa indicada. */
  long countVehiclesByTenantId(UUID tenantId);

  /** Unidades registradas en la empresa indicada. */
  List<FleetVehicleSummary> findVehiclesByTenantId(UUID tenantId);

  /** Conductores registrados en la empresa indicada. */
  List<FleetDriverSummary> findDriversByTenantId(UUID tenantId);
}
