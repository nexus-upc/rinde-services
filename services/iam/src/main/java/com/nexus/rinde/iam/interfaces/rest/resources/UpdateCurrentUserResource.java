package com.nexus.rinde.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos para actualizar el nombre completo del usuario autenticado. */
public record UpdateCurrentUserResource(
    @NotBlank(message = "El nombre completo es obligatorio.")
        @Size(max = 150, message = "El nombre completo no puede superar los 150 caracteres.")
        @Schema(example = "Ana Torres Vega")
        String fullName) {}
