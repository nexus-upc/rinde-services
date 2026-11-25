package com.nexus.rinde.expense.domain.services;

import com.nexus.rinde.expense.domain.model.valueobjects.TripSnapshot;
import java.util.Optional;
import java.util.UUID;

/** Consulta el estado y asignación de un viaje ajeno al contexto, filtrado por empresa. */
public interface TripStatusService {

  /** Devuelve los datos relevantes del viaje, o vacío si no existe para la empresa. */
  Optional<TripSnapshot> findTrip(UUID tenantId, UUID tripId);

  /** Devuelve el nombre del estado del viaje, o vacío si no existe para la empresa. */
  default Optional<String> findTripStatus(UUID tenantId, UUID tripId) {
    return findTrip(tenantId, tripId).map(TripSnapshot::status);
  }
}
