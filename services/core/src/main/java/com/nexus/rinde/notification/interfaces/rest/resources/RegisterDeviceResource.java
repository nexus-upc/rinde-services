package com.nexus.rinde.notification.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos de registro de un dispositivo móvil para avisos push. */
public record RegisterDeviceResource(
    @NotBlank(message = "El token del dispositivo es obligatorio.")
        @Size(max = 512, message = "El token del dispositivo no puede superar los 512 caracteres.")
        @Schema(example = "fcm-device-token-ejemplo-01")
        String token,
    @NotBlank(message = "La plataforma es obligatoria.")
        @Size(max = 30, message = "La plataforma no puede superar los 30 caracteres.")
        @Schema(example = "ANDROID")
        String platform) {}
