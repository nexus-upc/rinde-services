package com.nexus.rinde.trip.interfaces.rest.transform;

import com.nexus.rinde.trip.domain.model.commands.RecordTripLocationsCommand;
import com.nexus.rinde.trip.interfaces.rest.resources.RecordTripLocationsResource;
import java.util.List;
import java.util.UUID;

/** Convierte el lote HTTP de ubicaciones en el comando del dominio. */
public final class RecordTripLocationsCommandFromResourceAssembler {

  private RecordTripLocationsCommandFromResourceAssembler() {}

  public static RecordTripLocationsCommand toCommand(
      UUID tenantId, UUID tripId, UUID userId, RecordTripLocationsResource resource) {
    List<RecordTripLocationsCommand.LocationSample> samples =
        resource.points().stream()
            .map(
                point ->
                    new RecordTripLocationsCommand.LocationSample(
                        point.latitude(),
                        point.longitude(),
                        point.accuracyMeters(),
                        point.recordedAt()))
            .toList();
    return new RecordTripLocationsCommand(tenantId, tripId, userId, samples);
  }
}
