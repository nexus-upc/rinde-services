package com.nexus.rinde.trip.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/** Resultado del registro por lotes de ubicaciones del viaje. */
public record RecordTripLocationsResponseResource(
    @Schema(example = "2") int savedCount, @Schema(example = "0") int duplicateCount) {}
