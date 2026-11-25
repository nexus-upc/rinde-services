package com.nexus.rinde.notification.domain.model.commands;

import java.util.UUID;

/** Registra o actualiza el token de dispositivo de un conductor. */
public record RegisterDriverDeviceCommand(
    UUID tenantId, UUID userId, String token, String platform) {}
