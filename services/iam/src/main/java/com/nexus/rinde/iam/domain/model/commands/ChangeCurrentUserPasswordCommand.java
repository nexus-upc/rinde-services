package com.nexus.rinde.iam.domain.model.commands;

import com.nexus.rinde.iam.domain.model.valueobjects.TenantId;
import com.nexus.rinde.iam.domain.model.valueobjects.UserId;

/** Cambia la contraseña del usuario que inició sesión verificando la actual. */
public record ChangeCurrentUserPasswordCommand(
    TenantId tenantId, UserId userId, String currentPassword, String newPassword) {}
