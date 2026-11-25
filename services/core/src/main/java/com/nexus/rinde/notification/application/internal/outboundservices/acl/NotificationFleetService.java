package com.nexus.rinde.notification.application.internal.outboundservices.acl;

import com.nexus.rinde.fleet.interfaces.acl.FleetContextFacade;
import com.nexus.rinde.fleet.interfaces.acl.FleetDriverSummary;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Adaptador ACL que resuelve en Fleet el identificador de usuario vinculado a un conductor. */
@Service
public class NotificationFleetService {

  private final FleetContextFacade fleetContextFacade;

  public NotificationFleetService(FleetContextFacade fleetContextFacade) {
    this.fleetContextFacade = fleetContextFacade;
  }

  /** Devuelve el userId de IAM asociado al conductor indicado, o vacío si no existe enlace. */
  public Optional<UUID> findUserIdByDriverId(UUID tenantId, UUID driverId) {
    if (tenantId == null || driverId == null) {
      return Optional.empty();
    }
    return fleetContextFacade.findDriversByTenantId(tenantId).stream()
        .filter(driver -> driverId.equals(driver.id()))
        .map(FleetDriverSummary::userId)
        .filter(Objects::nonNull)
        .findFirst();
  }
}
