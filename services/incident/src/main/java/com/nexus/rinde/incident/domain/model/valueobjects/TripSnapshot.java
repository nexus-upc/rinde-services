package com.nexus.rinde.incident.domain.model.valueobjects;

import java.util.UUID;

/** Datos del viaje que Incident necesita para validar un reporte. Los entrega Trip por HTTP. */
public record TripSnapshot(UUID tripId, String status, UUID vehicleId, UUID driverId) {

  /** Un reporte solo se acepta mientras el viaje está en ruta. */
  public boolean isInRoute() {
    return "IN_ROUTE".equals(status);
  }

  public boolean isAssignedTo(UUID otherDriverId) {
    return otherDriverId != null && otherDriverId.equals(driverId);
  }
}
