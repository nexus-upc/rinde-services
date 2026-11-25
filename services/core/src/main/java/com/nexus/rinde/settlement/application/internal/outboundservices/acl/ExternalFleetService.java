package com.nexus.rinde.settlement.application.internal.outboundservices.acl;

import com.nexus.rinde.fleet.interfaces.acl.FleetContextFacade;
import com.nexus.rinde.fleet.interfaces.acl.FleetDriverSummary;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Adaptador ACL que consulta en Fleet el conductor vinculado a un usuario de la empresa. */
@Service
public class ExternalFleetService {

  private final FleetContextFacade fleetContextFacade;

  public ExternalFleetService(FleetContextFacade fleetContextFacade) {
    this.fleetContextFacade = fleetContextFacade;
  }

  /** Identificador de conductor en Fleet asociado al usuario, o vacío si no existe. */
  public Optional<UUID> findDriverIdByUserId(UUID tenantId, UUID userId) {
    if (userId == null) {
      return Optional.empty();
    }
    return fleetContextFacade.findDriversByTenantId(tenantId).stream()
        .filter(driver -> userId.equals(driver.userId()))
        .map(FleetDriverSummary::id)
        .findFirst();
  }
}
