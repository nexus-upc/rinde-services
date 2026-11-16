package com.nexus.rinde.fleet.domain.model.queries;

import java.util.UUID;

/** Consulta el conductor vinculado a una cuenta de usuario de la empresa. */
public record GetDriverByUserIdQuery(UUID tenantId, UUID userId) {}
