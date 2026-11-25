package com.nexus.rinde.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/** Datos opcionales para actualizar el perfil de la empresa actual. */
public record UpdateCompanyProfileResource(
    @Size(max = 120, message = "La razón social no puede superar los 120 caracteres.")
        @Schema(nullable = true, example = "Transportes Andes del Perú SAC")
        String tradeName,
    @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres.")
        @Schema(nullable = true, example = "+51 999 888 777")
        String phone,
    @Size(max = 200, message = "La dirección no puede superar los 200 caracteres.")
        @Schema(nullable = true, example = "Av. Nicolás Arriola 1200, La Victoria, Lima")
        String address) {}
