package com.nexus.rinde.iam.domain.model.commands;

import com.nexus.rinde.iam.domain.model.valueobjects.TenantId;

/** Actualiza razón social o datos de contacto de la empresa del administrador. */
public record UpdateCurrentTenantProfileCommand(
    TenantId tenantId, String tradeName, String phone, String address) {}
