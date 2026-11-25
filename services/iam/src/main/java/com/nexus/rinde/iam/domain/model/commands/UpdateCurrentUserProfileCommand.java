package com.nexus.rinde.iam.domain.model.commands;

import com.nexus.rinde.iam.domain.model.valueobjects.TenantId;
import com.nexus.rinde.iam.domain.model.valueobjects.UserId;

/** Actualiza el nombre completo del usuario que inició sesión. */
public record UpdateCurrentUserProfileCommand(TenantId tenantId, UserId userId, String fullName) {}
