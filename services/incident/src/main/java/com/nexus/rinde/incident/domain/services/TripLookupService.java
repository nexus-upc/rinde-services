package com.nexus.rinde.incident.domain.services;

import com.nexus.rinde.incident.domain.model.valueobjects.TripSnapshot;
import java.util.Optional;
import java.util.UUID;

/** Consulta un viaje ajeno al contexto, filtrado por empresa. */
public interface TripLookupService {

  /** Devuelve los datos del viaje de la empresa, o vacío si no existe. */
  Optional<TripSnapshot> findTrip(UUID tenantId, UUID tripId);
}
