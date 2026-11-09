package com.nexus.rinde.iam.demorequest.interfaces.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos del formulario de la landing para solicitar una demostración (US06). */
public record DemoRequestResource(
    @Schema(description = "Nombre de quien solicita la demostración", example = "Ana Rojas Paredes")
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres.")
        String fullName,
    @Schema(description = "Nombre de la empresa de transporte", example = "Transportes Andes SAC")
        @NotBlank(message = "La empresa es obligatoria.")
        @Size(max = 120, message = "El nombre de la empresa no puede superar los 120 caracteres.")
        String companyName,
    @Schema(description = "Correo de contacto", example = "ana.rojas@transportesandes.pe")
        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "Ingresa un correo válido.")
        @Size(max = 150, message = "El correo no puede superar los 150 caracteres.")
        String email,
    @Schema(description = "Teléfono de contacto. Opcional.", example = "+51 999 888 777")
        @Pattern(
            regexp = "^$|^\\+?[0-9()\\s-]{6,30}$",
            message = "El teléfono no tiene un formato válido.")
        String phone,
    @Schema(
            description = "Cantidad de unidades de la flota, de 1 a 500",
            example = "12",
            minimum = "1",
            maximum = "500")
        @NotNull(message = "La cantidad de unidades es obligatoria.")
        @Min(value = 1, message = "La cantidad de unidades debe estar entre 1 y 500.")
        @Max(value = 500, message = "La cantidad de unidades debe estar entre 1 y 500.")
        Integer fleetSize,
    @Schema(
            description = "Comentario libre de hasta 500 caracteres. Opcional.",
            example = "Queremos revisar la gestión de viajes y gastos.")
        @Size(max = 500, message = "El mensaje no puede superar los 500 caracteres.")
        String message) {}
