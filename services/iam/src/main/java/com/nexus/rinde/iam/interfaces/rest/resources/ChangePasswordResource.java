package com.nexus.rinde.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cambio de contraseña del usuario autenticado desde su configuración. */
public record ChangePasswordResource(
    @NotBlank(message = "La contraseña actual es obligatoria.")
        @Schema(example = "Clave-Actual-2026")
        String currentPassword,
    @NotBlank(message = "La nueva contraseña es obligatoria.")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres.")
        @Schema(example = "Clave-Nueva-2026")
        String newPassword) {}
