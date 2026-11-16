package com.nexus.rinde.fleet.domain.model.commands;

import java.util.UUID;

/** Comando para cambiar o retirar la cuenta vinculada a un conductor; userId nulo la retira. */
public record LinkDriverAccountCommand(UUID tenantId, UUID driverId, UUID userId) {}
