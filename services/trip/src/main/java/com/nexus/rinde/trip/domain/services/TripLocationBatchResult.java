package com.nexus.rinde.trip.domain.services;

/** Resultado del registro por lotes de ubicaciones de un viaje. */
public record TripLocationBatchResult(int savedCount, int duplicateCount) {}
