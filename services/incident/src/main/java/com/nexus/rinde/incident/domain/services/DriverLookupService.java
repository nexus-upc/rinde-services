package com.nexus.rinde.incident.domain.services;

import java.util.Optional;
import java.util.UUID;

/** Consulta qué conductor de la empresa corresponde a un usuario, sin depender de Fleet. */
public interface DriverLookupService {

  /** Devuelve el conductor vinculado al usuario, o vacío si el usuario no es conductor. */
  Optional<UUID> findDriverIdByUserId(UUID tenantId, UUID userId);
}
