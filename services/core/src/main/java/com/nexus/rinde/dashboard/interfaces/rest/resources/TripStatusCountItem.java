package com.nexus.rinde.dashboard.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/** Conteo de viajes por estado operativo. */
public record TripStatusCountItem(
    @Schema(example = "FINISHED") String status, @Schema(example = "4") int count) {}
