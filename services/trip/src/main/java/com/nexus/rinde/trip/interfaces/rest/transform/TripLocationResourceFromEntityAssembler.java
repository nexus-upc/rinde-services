package com.nexus.rinde.trip.interfaces.rest.transform;

import com.nexus.rinde.trip.domain.services.LatestTripLocation;
import com.nexus.rinde.trip.domain.services.TripLocationBatchResult;
import com.nexus.rinde.trip.interfaces.rest.resources.LatestTripLocationResource;
import com.nexus.rinde.trip.interfaces.rest.resources.RecordTripLocationsResponseResource;

/** Convierte los resultados de ubicaciones del dominio a recursos REST. */
public final class TripLocationResourceFromEntityAssembler {

  private TripLocationResourceFromEntityAssembler() {}

  public static RecordTripLocationsResponseResource toBatchResponse(
      TripLocationBatchResult result) {
    return new RecordTripLocationsResponseResource(result.savedCount(), result.duplicateCount());
  }

  public static LatestTripLocationResource toLatestResource(LatestTripLocation location) {
    return new LatestTripLocationResource(
        location.tripId(),
        location.code(),
        location.vehicleId(),
        location.latitude(),
        location.longitude(),
        location.accuracyMeters(),
        location.recordedAt());
  }
}
