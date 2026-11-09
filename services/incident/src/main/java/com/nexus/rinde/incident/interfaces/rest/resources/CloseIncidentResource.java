package com.nexus.rinde.incident.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Acción tomada frente a la incidencia, que queda en su historial al cerrarla. */
public record CloseIncidentResource(
    @Schema(example = "Se cambió la llanta de repuesto y el viaje continuó")
        @NotBlank
        @Size(max = 500)
        String action) {}
