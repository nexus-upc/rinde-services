package com.nexus.rinde.subscription.domain.model.commands;

import java.util.UUID;

/** Comando para asociar un plan disponible a la empresa autenticada. */
public record ChoosePlanCommand(UUID tenantId, UUID planId) {}
