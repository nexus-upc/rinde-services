package com.nexus.rinde.subscription.domain.services;

import java.util.UUID;

/** Puerto hacia Fleet: cuántas unidades tiene registradas la empresa. */
public interface FleetUnitCounter {

  /** Devuelve la cantidad de unidades de la empresa; si Fleet no responde, lanza una excepción. */
  long countVehicles(UUID tenantId);
}
