package com.nexus.rinde.settlement.application.internal.outboundservices.acl;

import com.nexus.rinde.trip.interfaces.acl.TripContextFacade;
import com.nexus.rinde.trip.interfaces.acl.TripSummary;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Adaptador ACL que consulta a Trip el estado de un viaje de la empresa. */
@Service
public class ExternalTripService {

  private final TripContextFacade tripContextFacade;

  public ExternalTripService(TripContextFacade tripContextFacade) {
    this.tripContextFacade = tripContextFacade;
  }

  /** Estado del viaje, o vacío si no existe para la empresa. */
  public Optional<String> findStatusByTripId(UUID tenantId, UUID tripId) {
    return tripContextFacade.findByIdAndTenantId(tripId, tenantId).map(TripSummary::status);
  }
}
