package com.nexus.rinde.trip.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Lote de puntos de ubicación enviados desde la cola del conductor. */
public record RecordTripLocationsResource(
    @Schema(description = "Lista de 1 a 50 puntos capturados en ruta")
        @NotNull(message = "La lista de ubicaciones es obligatoria.")
        @Size(min = 1, max = 50, message = "Debe enviar entre 1 y 50 puntos de ubicación.")
        List<
                @NotNull(message = "Cada punto de ubicación es obligatorio.") @Valid
                TripLocationPointResource>
            points) {}
