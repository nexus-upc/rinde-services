package com.nexus.rinde.notification.domain.model.commands;

import java.util.UUID;

/** Elimina el token de dispositivo de un conductor al cerrar sesión. */
public record RemoveDriverDeviceCommand(UUID tenantId, UUID userId, String token) {}
