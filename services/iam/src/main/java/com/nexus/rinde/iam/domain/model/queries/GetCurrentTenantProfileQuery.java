package com.nexus.rinde.iam.domain.model.queries;

import com.nexus.rinde.iam.domain.model.valueobjects.TenantId;

/** Consulta el perfil completo de la empresa del usuario autenticado. */
public record GetCurrentTenantProfileQuery(TenantId tenantId) {}
